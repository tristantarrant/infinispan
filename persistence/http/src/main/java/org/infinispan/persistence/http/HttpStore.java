package org.infinispan.persistence.http;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.net.ssl.SSLException;

import org.infinispan.commons.configuration.ConfiguredBy;
import org.infinispan.commons.time.TimeService;
import org.infinispan.commons.util.IntSet;
import org.infinispan.persistence.http.configuration.HttpStoreConfiguration;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.infinispan.persistence.spi.MarshallableEntryFactory;
import org.infinispan.persistence.spi.NonBlockingStore;
import org.infinispan.persistence.spi.PersistenceException;
import org.reactivestreams.Publisher;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.reactivex.rxjava3.core.Flowable;

@ConfiguredBy(HttpStoreConfiguration.class)
public class HttpStore<K, V> implements NonBlockingStore<K, V> {

   private HttpStoreConfiguration config;
   private MarshallableEntryFactory<K, V> entryFactory;
   private TimeService timeService;
   private Executor nonBlockingExecutor;
   private EventLoopGroup eventLoopGroup;
   private Bootstrap bootstrap;
   private URI baseUri;
   private String host;
   private int port;
   private boolean ssl;
   private String authorizationHeader;

   private static final Set<String> SKIP_HEADERS = Set.of(
         "connection", "keep-alive", "transfer-encoding", "content-length", "content-encoding"
   );

   @Override
   public CompletionStage<Void> start(InitializationContext ctx) {
      config = ctx.getConfiguration();
      entryFactory = ctx.getMarshallableEntryFactory();
      timeService = ctx.getTimeService();
      nonBlockingExecutor = ctx.getNonBlockingExecutor();

      baseUri = URI.create(config.baseUrl());
      host = baseUri.getHost();
      ssl = "https".equalsIgnoreCase(baseUri.getScheme());
      port = baseUri.getPort();
      if (port == -1) {
         port = ssl ? 443 : 80;
      }

      SslContext sslCtx;
      if (ssl) {
         try {
            sslCtx = SslContextBuilder.forClient().build();
         } catch (SSLException e) {
            return CompletableFuture.failedFuture(new PersistenceException("Failed to create SSL context", e));
         }
      } else {
         sslCtx = null;
      }

      eventLoopGroup = new NioEventLoopGroup(1);
      bootstrap = new Bootstrap()
            .group(eventLoopGroup)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
               @Override
               protected void initChannel(SocketChannel ch) {
                  ChannelPipeline p = ch.pipeline();
                  if (sslCtx != null) {
                     p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                  }
                  p.addLast(new HttpClientCodec());
                  p.addLast(new HttpObjectAggregator(1048576));
               }
            });

      String bearerToken = config.bearerToken();
      String username = config.username();
      if (bearerToken != null) {
         authorizationHeader = "Bearer " + bearerToken;
      } else if (username != null) {
         String credentials = username + ":" + config.password();
         authorizationHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
      }

      return CompletableFuture.completedFuture(null);
   }

   @Override
   public CompletionStage<Void> stop() {
      if (eventLoopGroup != null) {
         CompletableFuture<Void> future = new CompletableFuture<>();
         eventLoopGroup.shutdownGracefully().addListener(f -> {
            if (f.isSuccess()) {
               future.complete(null);
            } else {
               future.completeExceptionally(f.cause());
            }
         });
         return future;
      }
      return CompletableFuture.completedFuture(null);
   }

   @Override
   public Set<Characteristic> characteristics() {
      return EnumSet.of(Characteristic.READ_ONLY, Characteristic.SHAREABLE);
   }

   @Override
   public CompletionStage<MarshallableEntry<K, V>> load(int segment, Object key) {
      String path = baseUri.getRawPath() + "/" + URLEncoder.encode(key.toString(), StandardCharsets.UTF_8);

      CompletableFuture<MarshallableEntry<K, V>> result = new CompletableFuture<>();

      ChannelFuture connectFuture = bootstrap.connect(host, port);
      connectFuture.addListener((ChannelFuture cf) -> {
         if (!cf.isSuccess()) {
            result.completeExceptionally(new PersistenceException("Failed to connect to " + host + ":" + port, cf.cause()));
            return;
         }

         Channel channel = cf.channel();
         channel.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpResponse>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
               try {
                  if (response.status().equals(HttpResponseStatus.OK)) {
                     ByteBuf content = response.content();
                     byte[] bytes = new byte[content.readableBytes()];
                     content.readBytes(bytes);
                     Map<String, String> headers = new LinkedHashMap<>();
                     for (Map.Entry<String, String> h : response.headers()) {
                        if (!SKIP_HEADERS.contains(h.getKey().toLowerCase())) {
                           headers.put(h.getKey(), h.getValue());
                        }
                     }
                     long maxAge = parseMaxAge(response.headers().get(HttpHeaderNames.CACHE_CONTROL));
                     long lifespanMillis = maxAge > 0 ? TimeUnit.SECONDS.toMillis(maxAge) : -1;
                     HttpMetadata metadata = new HttpMetadata(lifespanMillis, headers);
                     long now = maxAge > 0 ? timeService.wallClockTime() : -1;
                     MarshallableEntry<K, V> entry = entryFactory.create(key, (Object) bytes, metadata, null, now, -1);
                     result.complete(entry);
                  } else if (response.status().equals(HttpResponseStatus.NOT_FOUND)) {
                     result.complete(null);
                  } else {
                     result.completeExceptionally(new PersistenceException(
                           "Unexpected HTTP status: " + response.status()));
                  }
               } finally {
                  ctx.close();
               }
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
               result.completeExceptionally(new PersistenceException("HTTP request failed", cause));
               ctx.close();
            }
         });

         DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
         request.headers().set(HttpHeaderNames.HOST, host);
         request.headers().set(HttpHeaderNames.CONNECTION, "close");
         if (authorizationHeader != null) {
            request.headers().set(HttpHeaderNames.AUTHORIZATION, authorizationHeader);
         }
         channel.writeAndFlush(request);
      });

      return result.thenApplyAsync(Function.identity(), nonBlockingExecutor);
   }

   @Override
   public CompletionStage<Void> write(int segment, MarshallableEntry<? extends K, ? extends V> entry) {
      throw new UnsupportedOperationException("HTTP store is read-only");
   }

   @Override
   public CompletionStage<Boolean> delete(int segment, Object key) {
      throw new UnsupportedOperationException("HTTP store is read-only");
   }

   @Override
   public CompletionStage<Void> clear() {
      throw new UnsupportedOperationException("HTTP store is read-only");
   }

   @Override
   public Publisher<K> publishKeys(IntSet segments, Predicate<? super K> filter) {
      return Flowable.empty();
   }

   @Override
   public Publisher<MarshallableEntry<K, V>> publishEntries(IntSet segments, Predicate<? super K> filter, boolean includeValues) {
      return Flowable.empty();
   }

   static long parseMaxAge(String cacheControl) {
      if (cacheControl == null || cacheControl.isEmpty()) {
         return -1;
      }
      for (String directive : cacheControl.split(",")) {
         String trimmed = directive.trim().toLowerCase();
         if (trimmed.startsWith("max-age=")) {
            try {
               return Long.parseLong(trimmed.substring("max-age=".length()).trim());
            } catch (NumberFormatException e) {
               return -1;
            }
         }
      }
      return -1;
   }
}
