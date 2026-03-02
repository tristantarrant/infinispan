package org.infinispan.persistence.http;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.marshall.TestObjectStreamMarshaller;
import org.infinispan.persistence.http.configuration.HttpStoreConfigurationBuilder;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.infinispan.persistence.spi.NonBlockingStore;
import org.infinispan.test.AbstractInfinispanTest;
import org.infinispan.util.PersistenceMockUtil;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;

@Test(groups = "unit", testName = "persistence.http.HttpStoreTest")
public class HttpStoreTest extends AbstractInfinispanTest {

   private final ConcurrentHashMap<String, byte[]> serverData = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<String, String> serverCacheControl = new ConcurrentHashMap<>();
   private volatile String requiredAuthorization;
   private EventLoopGroup bossGroup;
   private EventLoopGroup workerGroup;
   private Channel serverChannel;
   private int serverPort;

   @BeforeClass
   public void startServer() throws Exception {
      bossGroup = new NioEventLoopGroup(1);
      workerGroup = new NioEventLoopGroup(1);

      ServerBootstrap b = new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
               @Override
               protected void initChannel(SocketChannel ch) {
                  ChannelPipeline p = ch.pipeline();
                  p.addLast(new HttpServerCodec());
                  p.addLast(new HttpObjectAggregator(1048576));
                  p.addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                     @Override
                     protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
                        String expected = requiredAuthorization;
                        if (expected != null) {
                           String actual = request.headers().get(HttpHeaderNames.AUTHORIZATION);
                           if (!expected.equals(actual)) {
                              DefaultFullHttpResponse unauthorized = new DefaultFullHttpResponse(
                                    HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
                              ctx.writeAndFlush(unauthorized).addListener(ChannelFutureListener.CLOSE);
                              return;
                           }
                        }
                        String uri = request.uri();
                        byte[] data = serverData.get(uri);
                        DefaultFullHttpResponse response;
                        if (data != null) {
                           response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                 HttpResponseStatus.OK, Unpooled.wrappedBuffer(data));
                           String cc = serverCacheControl.get(uri);
                           if (cc != null) {
                              response.headers().set(HttpHeaderNames.CACHE_CONTROL, cc);
                           }
                        } else {
                           response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                 HttpResponseStatus.NOT_FOUND);
                        }
                        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                     }
                  });
               }
            });

      serverChannel = b.bind(0).sync().channel();
      serverPort = ((InetSocketAddress) serverChannel.localAddress()).getPort();
   }

   @AfterClass
   public void stopServer() throws Exception {
      if (serverChannel != null) {
         serverChannel.close().sync();
      }
      if (bossGroup != null) {
         bossGroup.shutdownGracefully().sync();
      }
      if (workerGroup != null) {
         workerGroup.shutdownGracefully().sync();
      }
      serverData.clear();
      serverCacheControl.clear();
   }

   private HttpStore<String, Object> createAndStartStore() throws Exception {
      return createAndStartStore(b -> {});
   }

   private HttpStore<String, Object> createAndStartStore(Consumer<HttpStoreConfigurationBuilder> customizer) throws Exception {
      ConfigurationBuilder cb = new ConfigurationBuilder();
      HttpStoreConfigurationBuilder storeBuilder = cb.persistence()
            .addStore(HttpStoreConfigurationBuilder.class)
            .baseUrl("http://localhost:" + serverPort);
      customizer.accept(storeBuilder);

      TestObjectStreamMarshaller marshaller = new TestObjectStreamMarshaller();
      InitializationContext ctx = PersistenceMockUtil.createContext(HttpStoreTest.class, cb.build(), marshaller);

      HttpStore<String, Object> store = new HttpStore<>();
      store.start(ctx).toCompletableFuture().join();
      return store;
   }

   public void testLoadExistingKey() throws Exception {
      serverData.put("/mykey", "hello world".getBytes(StandardCharsets.UTF_8));
      HttpStore<String, Object> store = createAndStartStore();
      try {
         MarshallableEntry<String, Object> entry = store.load(0, "mykey").toCompletableFuture().join();
         assertNotNull(entry);
         byte[] value = (byte[]) entry.getValue();
         assertEquals("hello world", new String(value, StandardCharsets.UTF_8));
      } finally {
         store.stop().toCompletableFuture().join();
         serverData.remove("/mykey");
      }
   }

   public void testLoadMissingKey() throws Exception {
      HttpStore<String, Object> store = createAndStartStore();
      try {
         MarshallableEntry<String, Object> entry = store.load(0, "nonexistent").toCompletableFuture().join();
         assertNull(entry);
      } finally {
         store.stop().toCompletableFuture().join();
      }
   }

   public void testContainsKey() throws Exception {
      serverData.put("/present", "data".getBytes(StandardCharsets.UTF_8));
      HttpStore<String, Object> store = createAndStartStore();
      try {
         assertTrue(store.containsKey(0, "present").toCompletableFuture().join());
         assertFalse(store.containsKey(0, "absent").toCompletableFuture().join());
      } finally {
         store.stop().toCompletableFuture().join();
         serverData.remove("/present");
      }
   }

   public void testCharacteristics() throws Exception {
      HttpStore<String, Object> store = createAndStartStore();
      try {
         assertEquals(EnumSet.of(NonBlockingStore.Characteristic.READ_ONLY, NonBlockingStore.Characteristic.SHAREABLE),
               store.characteristics());
      } finally {
         store.stop().toCompletableFuture().join();
      }
   }

   public void testStartStop() throws Exception {
      HttpStore<String, Object> store = createAndStartStore();
      store.stop().toCompletableFuture().join();
   }

   public void testLoadWithMaxAge() throws Exception {
      serverData.put("/expiring", "temporary".getBytes(StandardCharsets.UTF_8));
      serverCacheControl.put("/expiring", "max-age=300");
      HttpStore<String, Object> store = createAndStartStore();
      try {
         MarshallableEntry<String, Object> entry = store.load(0, "expiring").toCompletableFuture().join();
         assertNotNull(entry);
         byte[] value = (byte[]) entry.getValue();
         assertEquals("temporary", new String(value, StandardCharsets.UTF_8));
         assertEquals(TimeUnit.SECONDS.toMillis(300), entry.getMetadata().lifespan());
         assertTrue(entry.created() > 0);
         assertFalse(entry.isExpired(System.currentTimeMillis()));
      } finally {
         store.stop().toCompletableFuture().join();
         serverData.remove("/expiring");
         serverCacheControl.remove("/expiring");
      }
   }

   public void testLoadWithoutCacheControl() throws Exception {
      serverData.put("/nocache", "persistent".getBytes(StandardCharsets.UTF_8));
      HttpStore<String, Object> store = createAndStartStore();
      try {
         MarshallableEntry<String, Object> entry = store.load(0, "nocache").toCompletableFuture().join();
         assertNotNull(entry);
         assertNotNull(entry.getMetadata());
         assertTrue(entry.getMetadata() instanceof HttpMetadata);
         assertEquals(-1, entry.getMetadata().lifespan());
      } finally {
         store.stop().toCompletableFuture().join();
         serverData.remove("/nocache");
      }
   }

   public void testLoadWithComplexCacheControl() throws Exception {
      serverData.put("/complex", "data".getBytes(StandardCharsets.UTF_8));
      serverCacheControl.put("/complex", "public, max-age=600, must-revalidate");
      HttpStore<String, Object> store = createAndStartStore();
      try {
         MarshallableEntry<String, Object> entry = store.load(0, "complex").toCompletableFuture().join();
         assertNotNull(entry);
         assertEquals(TimeUnit.SECONDS.toMillis(600), entry.getMetadata().lifespan());
      } finally {
         store.stop().toCompletableFuture().join();
         serverData.remove("/complex");
         serverCacheControl.remove("/complex");
      }
   }

   public void testHeadersCapturedInMetadata() throws Exception {
      serverData.put("/headers", "data".getBytes(StandardCharsets.UTF_8));
      serverCacheControl.put("/headers", "max-age=60");
      HttpStore<String, Object> store = createAndStartStore();
      try {
         MarshallableEntry<String, Object> entry = store.load(0, "headers").toCompletableFuture().join();
         assertNotNull(entry);
         assertTrue(entry.getMetadata() instanceof HttpMetadata);
         HttpMetadata metadata = (HttpMetadata) entry.getMetadata();
         assertNotNull(metadata.headers());
         assertFalse(metadata.headers().isEmpty());
         assertEquals("max-age=60", metadata.headers().get("cache-control"));
      } finally {
         store.stop().toCompletableFuture().join();
         serverData.remove("/headers");
         serverCacheControl.remove("/headers");
      }
   }

   public void testHopByHopHeadersFiltered() throws Exception {
      serverData.put("/filtered", "data".getBytes(StandardCharsets.UTF_8));
      HttpStore<String, Object> store = createAndStartStore();
      try {
         MarshallableEntry<String, Object> entry = store.load(0, "filtered").toCompletableFuture().join();
         assertNotNull(entry);
         HttpMetadata metadata = (HttpMetadata) entry.getMetadata();
         assertNull(metadata.headers().get("content-length"));
         assertNull(metadata.headers().get("connection"));
         assertNull(metadata.headers().get("transfer-encoding"));
         assertNull(metadata.headers().get("content-encoding"));
      } finally {
         store.stop().toCompletableFuture().join();
         serverData.remove("/filtered");
      }
   }

   public void testHeadersWithoutCacheControl() throws Exception {
      serverData.put("/plain", "data".getBytes(StandardCharsets.UTF_8));
      HttpStore<String, Object> store = createAndStartStore();
      try {
         MarshallableEntry<String, Object> entry = store.load(0, "plain").toCompletableFuture().join();
         assertNotNull(entry);
         HttpMetadata metadata = (HttpMetadata) entry.getMetadata();
         assertNotNull(metadata.headers());
         assertNull(metadata.headers().get("content-length"));
         assertEquals(-1, metadata.lifespan());
      } finally {
         store.stop().toCompletableFuture().join();
         serverData.remove("/plain");
      }
   }

   public void testParseMaxAge() {
      assertEquals(300, HttpStore.parseMaxAge("max-age=300"));
      assertEquals(600, HttpStore.parseMaxAge("public, max-age=600, must-revalidate"));
      assertEquals(3600, HttpStore.parseMaxAge("max-age=3600"));
      assertEquals(-1, HttpStore.parseMaxAge(null));
      assertEquals(-1, HttpStore.parseMaxAge(""));
      assertEquals(-1, HttpStore.parseMaxAge("no-cache"));
      assertEquals(-1, HttpStore.parseMaxAge("max-age=notanumber"));
      assertEquals(0, HttpStore.parseMaxAge("max-age=0"));
   }

   public void testBasicAuthentication() throws Exception {
      String expectedAuth = "Basic " + Base64.getEncoder().encodeToString("admin:secret".getBytes(StandardCharsets.UTF_8));
      requiredAuthorization = expectedAuth;
      serverData.put("/secured", "protected data".getBytes(StandardCharsets.UTF_8));
      HttpStore<String, Object> store = createAndStartStore(b -> b.username("admin").password("secret"));
      try {
         MarshallableEntry<String, Object> entry = store.load(0, "secured").toCompletableFuture().join();
         assertNotNull(entry);
         byte[] value = (byte[]) entry.getValue();
         assertEquals("protected data", new String(value, StandardCharsets.UTF_8));
      } finally {
         store.stop().toCompletableFuture().join();
         serverData.remove("/secured");
         requiredAuthorization = null;
      }
   }

   public void testBearerTokenAuthentication() throws Exception {
      requiredAuthorization = "Bearer my-jwt-token-123";
      serverData.put("/token-secured", "token data".getBytes(StandardCharsets.UTF_8));
      HttpStore<String, Object> store = createAndStartStore(b -> b.bearerToken("my-jwt-token-123"));
      try {
         MarshallableEntry<String, Object> entry = store.load(0, "token-secured").toCompletableFuture().join();
         assertNotNull(entry);
         byte[] value = (byte[]) entry.getValue();
         assertEquals("token data", new String(value, StandardCharsets.UTF_8));
      } finally {
         store.stop().toCompletableFuture().join();
         serverData.remove("/token-secured");
         requiredAuthorization = null;
      }
   }

   public void testAuthenticationFailure() throws Exception {
      requiredAuthorization = "Basic " + Base64.getEncoder().encodeToString("admin:correct".getBytes(StandardCharsets.UTF_8));
      serverData.put("/fail-auth", "data".getBytes(StandardCharsets.UTF_8));
      HttpStore<String, Object> store = createAndStartStore(b -> b.username("admin").password("wrong"));
      try {
         store.load(0, "fail-auth").toCompletableFuture().join();
         assertTrue("Expected PersistenceException", false);
      } catch (Exception e) {
         assertTrue(e.getCause().getMessage().contains("401"));
      } finally {
         store.stop().toCompletableFuture().join();
         serverData.remove("/fail-auth");
         requiredAuthorization = null;
      }
   }

   public void testNoAuthWhenServerRequiresIt() throws Exception {
      requiredAuthorization = "Bearer required-token";
      serverData.put("/needs-auth", "data".getBytes(StandardCharsets.UTF_8));
      HttpStore<String, Object> store = createAndStartStore();
      try {
         store.load(0, "needs-auth").toCompletableFuture().join();
         assertTrue("Expected PersistenceException", false);
      } catch (Exception e) {
         assertTrue(e.getCause().getMessage().contains("401"));
      } finally {
         store.stop().toCompletableFuture().join();
         serverData.remove("/needs-auth");
         requiredAuthorization = null;
      }
   }
}
