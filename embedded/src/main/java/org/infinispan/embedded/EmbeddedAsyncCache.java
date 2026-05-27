package org.infinispan.embedded;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import javax.security.auth.Subject;

import org.infinispan.AdvancedCache;
import org.infinispan.api.async.AsyncCache;
import org.infinispan.api.async.AsyncCacheEntryProcessor;
import org.infinispan.api.async.AsyncContainer;
import org.infinispan.api.async.AsyncQuery;
import org.infinispan.api.async.AsyncStreamingCache;
import org.infinispan.api.async.AsyncTransactionManager;
import org.infinispan.api.async.events.cache.AsyncCacheListener;
import org.infinispan.api.common.CacheEntry;
import org.infinispan.api.common.CacheEntryExpiration;
import org.infinispan.api.common.CacheEntryVersion;
import org.infinispan.api.common.CacheOptions;
import org.infinispan.api.common.CacheWriteOptions;
import org.infinispan.api.common.MutableCacheEntry;
import org.infinispan.api.common.process.CacheEntryProcessorContext;
import org.infinispan.api.common.process.CacheEntryProcessorResult;
import org.infinispan.api.common.process.CacheProcessorOptions;
import org.infinispan.api.configuration.CacheConfiguration;
import org.infinispan.embedded.impl.EmbeddedAsyncCacheListener;
import org.infinispan.embedded.impl.EmbeddedCacheEntry;
import org.infinispan.embedded.impl.EmbeddedCacheEntryProcessorContext;
import org.infinispan.embedded.impl.EmbeddedMutableCacheEntry;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.Metadata;
import org.infinispan.reactive.publisher.PublisherTransformers;
import org.reactivestreams.FlowAdapters;

import io.reactivex.rxjava3.core.Flowable;
import jakarta.transaction.TransactionManager;

/**
 * @since 16.3
 */
public class EmbeddedAsyncCache<K, V> implements AsyncCache<K, V> {
   private final Embedded embedded;
   private final AdvancedCache<K, V> cache;

   EmbeddedAsyncCache(Embedded embedded, AdvancedCache<K, V> cache) {
      this.embedded = embedded;
      this.cache = cache;
   }

   @Override
   public String name() {
      return cache.getName();
   }

   @Override
   public CompletionStage<CacheConfiguration> configuration() {
      return CompletableFuture.completedFuture(cache.getCacheConfiguration());
   }

   @Override
   public AsyncContainer container() {
      return embedded.async();
   }

   @Override
   public CompletionStage<CacheEntry<K, V>> getEntry(K key, CacheOptions options) {
      return cache.getCacheEntryAsync(key).thenApply(EmbeddedAsyncCache::toApiEntry);
   }

   @Override
   public CompletionStage<CacheEntry<K, V>> putIfAbsent(K key, V value, CacheWriteOptions options) {
      return cache.putIfAbsentAsyncEntry(key, value, toMetadata(options))
            .thenApply(EmbeddedAsyncCache::toApiEntry);
   }

   @Override
   public CompletionStage<CacheEntry<K, V>> put(K key, V value, CacheWriteOptions options) {
      return cache.putAsyncEntry(key, value, toMetadata(options))
            .thenApply(EmbeddedAsyncCache::toApiEntry);
   }

   @Override
   public CompletionStage<CacheEntry<K, V>> getOrReplaceEntry(K key, V value, CacheEntryVersion version, CacheWriteOptions options) {
      return cache.getCacheEntryAsync(key).thenCompose(current -> {
         if (current == null) {
            return CompletableFuture.completedFuture(null);
         }
         CacheEntry<K, V> apiEntry = toApiEntry(current);
         if (version.equals(apiEntry.metadata().version())) {
            return cache.replaceAsync(key, value, toMetadata(options))
                  .thenApply(v -> apiEntry);
         }
         return CompletableFuture.completedFuture(apiEntry);
      });
   }

   @Override
   public CompletionStage<Boolean> remove(K key, CacheOptions options) {
      return cache.removeAsync(key).thenApply(Objects::nonNull);
   }

   @Override
   public CompletionStage<Boolean> remove(K key, CacheEntryVersion version, CacheOptions options) {
      return cache.getCacheEntryAsync(key).thenCompose(current -> {
         if (current == null) {
            return CompletableFuture.completedFuture(false);
         }
         CacheEntry<K, V> apiEntry = toApiEntry(current);
         if (version.equals(apiEntry.metadata().version())) {
            return cache.removeAsync(key).thenApply(Objects::nonNull);
         }
         return CompletableFuture.completedFuture(false);
      });
   }

   @Override
   public CompletionStage<CacheEntry<K, V>> getAndRemove(K key, CacheOptions options) {
      return cache.removeAsyncEntry(key).thenApply(EmbeddedAsyncCache::toApiEntry);
   }

   @Override
   public Flow.Publisher<K> keys(CacheOptions options) {
      return FlowAdapters.toFlowPublisher(
            cache.cachePublisher().keyPublisher(PublisherTransformers.identity()).publisherWithoutSegments()
      );
   }

   @Override
   public Flow.Publisher<CacheEntry<K, V>> entries(CacheOptions options) {
      return FlowAdapters.toFlowPublisher(
            Flowable.fromPublisher(
                  cache.cachePublisher().entryPublisher(PublisherTransformers.identity()).publisherWithoutSegments()
            ).map(e -> (CacheEntry<K, V>) new EmbeddedCacheEntry<>(e))
      );
   }

   @Override
   public CompletionStage<Void> putAll(Map<K, V> entries, CacheWriteOptions options) {
      return cache.putAllAsync(entries, toMetadata(options));
   }

   @Override
   public CompletionStage<Void> putAll(Flow.Publisher<CacheEntry<K, V>> entries, CacheWriteOptions options) {
      Metadata metadata = toMetadata(options);
      return Flowable.fromPublisher(FlowAdapters.toPublisher(entries))
            .concatMapCompletable(e -> Flowable.fromCompletionStage(cache.putAsyncEntry(e.key(), e.value(), metadata)).ignoreElements())
            .toCompletionStage(null);
   }

   @Override
   public Flow.Publisher<CacheEntry<K, V>> getAll(Set<K> keys, CacheOptions options) {
      return FlowAdapters.toFlowPublisher(
            Flowable.fromPublisher(
                  cache.cachePublisher().withKeys(keys).entryPublisher(PublisherTransformers.identity()).publisherWithoutSegments()
            ).map(e -> (CacheEntry<K, V>) new EmbeddedCacheEntry<>(e))
      );
   }

   @Override
   public Flow.Publisher<CacheEntry<K, V>> getAll(CacheOptions options, K... keys) {
      return getAll(Set.of(keys), options);
   }

   @Override
   public Flow.Publisher<K> removeAll(Set<K> keys, CacheWriteOptions options) {
      return FlowAdapters.toFlowPublisher(
            Flowable.fromIterable(keys)
                  .concatMapSingle(key -> Flowable.fromCompletionStage(
                        cache.removeAsync(key).thenApply(v -> v != null ? key : null)
                  ).singleOrError())
                  .filter(k -> k != null)
      );
   }

   @Override
   public Flow.Publisher<K> removeAll(Flow.Publisher<K> keys, CacheWriteOptions options) {
      return FlowAdapters.toFlowPublisher(
            Flowable.fromPublisher(FlowAdapters.toPublisher(keys))
                  .concatMapSingle(key -> Flowable.fromCompletionStage(
                        cache.removeAsync(key).thenApply(v -> v != null ? key : null)
                  ).singleOrError())
                  .filter(k -> k != null)
      );
   }

   @Override
   public Flow.Publisher<CacheEntry<K, V>> getAndRemoveAll(Set<K> keys, CacheWriteOptions options) {
      return FlowAdapters.toFlowPublisher(
            Flowable.fromIterable(keys)
                  .concatMapMaybe(key -> Flowable.fromCompletionStage(cache.removeAsyncEntry(key))
                        .filter(e -> e != null)
                        .map(e -> (CacheEntry<K, V>) new EmbeddedCacheEntry<>(e))
                        .singleElement())
      );
   }

   @Override
   public Flow.Publisher<CacheEntry<K, V>> getAndRemoveAll(Flow.Publisher<K> keys, CacheWriteOptions options) {
      return FlowAdapters.toFlowPublisher(
            Flowable.fromPublisher(FlowAdapters.toPublisher(keys))
                  .concatMapMaybe(key -> Flowable.fromCompletionStage(cache.removeAsyncEntry(key))
                        .filter(e -> e != null)
                        .map(e -> (CacheEntry<K, V>) new EmbeddedCacheEntry<>(e))
                        .singleElement())
      );
   }

   @Override
   public CompletionStage<Long> estimateSize(CacheOptions options) {
      return cache.sizeAsync();
   }

   @Override
   public CompletionStage<Void> clear(CacheOptions options) {
      return cache.clearAsync();
   }

   @Override
   public <R> AsyncQuery<K, V, R> query(String query, CacheOptions options) {
      return new EmbeddedAsyncQuery<>(cache, cache.query(query), options);
   }

   @Override
   public AsyncCacheListener<K, V> listen() {
      return new EmbeddedAsyncCacheListener<>(cache);
   }

   @Override
   public <T> Flow.Publisher<CacheEntryProcessorResult<K, T>> process(Set<K> keys, AsyncCacheEntryProcessor<K, V, T> task, CacheOptions options) {
      Flow.Publisher<MutableCacheEntry<K, V>> entries = FlowAdapters.toFlowPublisher(
            cache.cachePublisher().withKeys(keys).<MutableCacheEntry<K, V>>entryPublisher(
                  publisher -> Flowable.fromPublisher(publisher).map(e -> new EmbeddedMutableCacheEntry<>(cache, e))
            ).publisherWithoutSegments()
      );
      CacheEntryProcessorContext context = new EmbeddedCacheEntryProcessorContext(options);
      return task.process(entries, context);
   }

   @Override
   public <T> Flow.Publisher<CacheEntryProcessorResult<K, T>> processAll(AsyncCacheEntryProcessor<K, V, T> processor, CacheProcessorOptions options) {
      Flow.Publisher<MutableCacheEntry<K, V>> entries = FlowAdapters.toFlowPublisher(
            cache.cachePublisher().<MutableCacheEntry<K, V>>entryPublisher(
                  publisher -> Flowable.fromPublisher(publisher).map(e -> new EmbeddedMutableCacheEntry<>(cache, e))
            ).publisherWithoutSegments()
      );
      CacheEntryProcessorContext context = new EmbeddedCacheEntryProcessorContext(options);
      return processor.process(entries, context);
   }

   @Override
   public AsyncStreamingCache<K> streaming() {
      return new EmbeddedAsyncStreamingCache<>(cache);
   }

   private static <K, V> CacheEntry<K, V> toApiEntry(org.infinispan.container.entries.CacheEntry<K, V> entry) {
      return entry == null ? null : new EmbeddedCacheEntry<>(entry);
   }

   @Override
   public EmbeddedAsyncCache<K, V> as(Subject subject) {
      return new EmbeddedAsyncCache<>(embedded, cache.withSubject(subject));
   }

   @Override
   public AsyncTransactionManager transactionManager() {
      TransactionManager tm = cache.getTransactionManager();
      return tm == null ? null : new EmbeddedAsyncTransactionManager(tm);
   }

   private static Metadata toMetadata(CacheWriteOptions options) {
      CacheEntryExpiration expiration = options.expiration();
      if (expiration.isDefault()) {
         return EmbeddedMetadata.EMPTY;
      }
      EmbeddedMetadata.Builder builder = new EmbeddedMetadata.Builder();
      expiration.lifespan().ifPresent(d -> builder.lifespan(d.toMillis(), TimeUnit.MILLISECONDS));
      expiration.maxIdle().ifPresent(d -> builder.maxIdle(d.toMillis(), TimeUnit.MILLISECONDS));
      return builder.build();
   }
}
