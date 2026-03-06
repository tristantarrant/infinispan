package org.infinispan.cli.commands.transfer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.aesh.command.CommandException;
import org.infinispan.cli.commands.Bookmark;
import org.infinispan.cli.impl.ContextAwareCommandInvocation;
import org.infinispan.client.hotrod.DataFormat;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.HotRodURI;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.marshall.IdentityMarshaller;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.commons.util.Util;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Transfers cache data between two servers using the Hot Rod protocol.
 *
 * @since 16.2
 */
public class HotRodTransferEngine implements TransferEngine {

   private static final DataFormat RAW_FORMAT = DataFormat.builder()
         .keyMarshaller(IdentityMarshaller.INSTANCE)
         .valueMarshaller(IdentityMarshaller.INSTANCE)
         .build();

   private RemoteCacheManager source;
   private RemoteCacheManager target;

   @Override
   public void connect(ContextAwareCommandInvocation invocation, String sourceArg, String targetArg,
                       String sourceUrl, String targetUrl) throws CommandException {
      invocation.println("Connecting to source: " + sourceUrl);
      source = createCacheManager(invocation, sourceArg);

      invocation.println("Connecting to target: " + targetUrl);
      target = createCacheManager(invocation, targetArg);
   }

   @Override
   public Set<String> getCacheNames() {
      return source.getCacheNames();
   }

   @Override
   public void transferCache(ContextAwareCommandInvocation invocation, String cacheName,
                             int batchSize, long maxEntries) {
      invocation.println("Transferring cache: " + cacheName);

      RemoteCache<Object, Object> sourceCache = source.getCache(cacheName);
      if (sourceCache == null) {
         throw new IllegalArgumentException("Cache '" + cacheName + "' not found on source");
      }

      RemoteCache<Object, Object> targetCache = target.administration()
            .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE).getOrCreateCache(cacheName, (String) null);
      if (targetCache == null) {
         throw new IllegalArgumentException("Could not get or create cache '" + cacheName + "' on target");
      }

      RemoteCache<byte[], byte[]> rawSourceCache = sourceCache.withDataFormat(RAW_FORMAT);
      RemoteCache<byte[], byte[]> rawTargetCache = targetCache.withDataFormat(RAW_FORMAT);

      long totalEntries = maxEntries > 0 ? maxEntries : sourceCache.size();
      long startTime = System.nanoTime();
      AtomicLong count = new AtomicLong();

      CloseableIterator<Map.Entry<Object, MetadataValue<Object>>> iterator =
            rawSourceCache.retrieveEntriesWithMetadata(null, batchSize);

      Flowable.generate(emitter -> {
               if (iterator.hasNext() && (maxEntries < 0 || count.get() < maxEntries)) {
                  emitter.onNext(iterator.next());
               } else {
                  emitter.onComplete();
               }
            })
            .cast(Map.Entry.class)
            .flatMapCompletable(entry -> {
               MetadataValue<Object> metadata = (MetadataValue<Object>) entry.getValue();
               int lifespan = metadata.getLifespan();
               int maxIdle = metadata.getMaxIdle();
               Completable put;
               if (lifespan > 0 || maxIdle > 0) {
                  put = Completable.fromCompletionStage(rawTargetCache.putAsync(
                        (byte[]) entry.getKey(), (byte[]) metadata.getValue(),
                        lifespan > 0 ? lifespan : -1, TimeUnit.SECONDS,
                        maxIdle > 0 ? maxIdle : -1, TimeUnit.SECONDS));
               } else {
                  put = Completable.fromCompletionStage(rawTargetCache.putAsync(
                        (byte[]) entry.getKey(), (byte[]) metadata.getValue()));
               }
               return put.doOnComplete(() -> {
                  long c = count.incrementAndGet();
                  if (c % 1_000 == 0) {
                     long pct = totalEntries > 0 ? (c * 100) / totalEntries : 0;
                     invocation.println("  " + cacheName + ": transferred " + c + "/" + totalEntries
                           + " entries (" + pct + "%)" + TransferEngine.eta(c, totalEntries, startTime));
                  }
               });
            }, false, batchSize)
            .doFinally(iterator::close)
            .blockingAwait();

      long elapsed = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
      invocation.println("  " + cacheName + ": transferred " + count.get() + " entries in "
            + TransferEngine.formatDuration(elapsed) + " (complete)");
   }

   @Override
   public void close() {
      Util.close(source);
      Util.close(target);
   }

   private static RemoteCacheManager createCacheManager(ContextAwareCommandInvocation invocation,
                                                        String uriOrBookmark) throws CommandException {
      if (uriOrBookmark.contains("://")) {
         return new RemoteCacheManager(uriOrBookmark);
      }
      Bookmark.ResolvedBookmark bookmark = Bookmark.resolve(invocation, uriOrBookmark);
      if (bookmark == null) {
         throw new CommandException("Bookmark '" + uriOrBookmark + "' not found and argument is not a valid URI");
      }
      ConfigurationBuilder builder = HotRodURI.create(bookmark.url()).toConfigurationBuilder();
      if (bookmark.username() != null) {
         builder.security().authentication().username(bookmark.username());
      }
      if (bookmark.password() != null) {
         builder.security().authentication().password(bookmark.password());
      }
      if (bookmark.truststore() != null) {
         builder.security().ssl().enable().trustStoreFileName(bookmark.truststore());
         if (bookmark.truststorePassword() != null) {
            builder.security().ssl().trustStorePassword(bookmark.truststorePassword().toCharArray());
         }
      }
      if (bookmark.keystore() != null) {
         builder.security().ssl().enable().keyStoreFileName(bookmark.keystore());
         if (bookmark.keystorePassword() != null) {
            builder.security().ssl().keyStorePassword(bookmark.keystorePassword().toCharArray());
         }
      }
      return new RemoteCacheManager(builder.build());
   }
}
