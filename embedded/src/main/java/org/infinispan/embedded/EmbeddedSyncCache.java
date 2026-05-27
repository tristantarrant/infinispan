package org.infinispan.embedded;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.CacheStream;
import org.infinispan.api.common.CacheEntry;
import org.infinispan.api.common.CacheEntryExpiration;
import org.infinispan.api.common.CacheEntryVersion;
import org.infinispan.api.common.CacheOptions;
import org.infinispan.api.common.CacheWriteOptions;
import org.infinispan.api.common.CloseableIterable;
import org.infinispan.api.common.process.CacheEntryProcessorContext;
import org.infinispan.api.common.process.CacheEntryProcessorResult;
import org.infinispan.api.common.process.CacheProcessorOptions;
import org.infinispan.api.configuration.CacheConfiguration;
import org.infinispan.api.sync.SyncCache;
import org.infinispan.api.sync.SyncCacheEntryProcessor;
import org.infinispan.api.sync.SyncContainer;
import org.infinispan.api.sync.SyncQuery;
import org.infinispan.api.sync.SyncStreamingCache;
import org.infinispan.api.sync.SyncTransactionManager;
import org.infinispan.api.sync.events.cache.SyncCacheListener;
import org.infinispan.embedded.impl.EmbeddedCacheEntry;
import org.infinispan.embedded.impl.EmbeddedCacheEntryProcessorContext;
import org.infinispan.embedded.impl.EmbeddedMutableCacheEntry;
import org.infinispan.embedded.impl.EmbeddedSyncCacheListener;
import org.infinispan.embedded.impl.EmbeddedUtil;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.Metadata;

import jakarta.transaction.TransactionManager;

/**
 * @since 16.3
 */
public class EmbeddedSyncCache<K, V> implements SyncCache<K, V> {
   private final Embedded embedded;
   private final AdvancedCache<K, V> cache;

   EmbeddedSyncCache(Embedded embedded, AdvancedCache<K, V> cache) {
      this.embedded = embedded;
      this.cache = cache;
   }

   @Override
   public String name() {
      return cache.getName();
   }

   @Override
   public CacheConfiguration configuration() {
      return cache.getCacheConfiguration();
   }

   @Override
   public SyncContainer container() {
      return embedded.sync();
   }

   @Override
   public CacheEntry<K, V> getEntry(K key, CacheOptions options) {
      return toApiEntry(cache.getCacheEntry(key));
   }

   @Override
   public CacheEntry<K, V> put(K key, V value, CacheWriteOptions options) {
      return toApiEntry(cache.putAsyncEntry(key, value, toMetadata(options)).join());
   }

   @Override
   public CacheEntry<K, V> putIfAbsent(K key, V value, CacheWriteOptions options) {
      return toApiEntry(cache.putIfAbsentAsyncEntry(key, value, toMetadata(options)).join());
   }

   @Override
   public CacheEntry<K, V> getOrReplaceEntry(K key, V value, CacheEntryVersion version, CacheWriteOptions options) {
      org.infinispan.container.entries.CacheEntry<K, V> current = cache.getCacheEntry(key);
      if (current == null) {
         return null;
      }
      CacheEntry<K, V> apiEntry = toApiEntry(current);
      if (version.equals(apiEntry.metadata().version())) {
         cache.replace(key, value, toMetadata(options));
      }
      return apiEntry;
   }

   @Override
   public boolean remove(K key, CacheEntryVersion version, CacheOptions options) {
      org.infinispan.container.entries.CacheEntry<K, V> current = cache.getCacheEntry(key);
      if (current == null) {
         return false;
      }
      CacheEntry<K, V> apiEntry = toApiEntry(current);
      if (version.equals(apiEntry.metadata().version())) {
         cache.remove(key);
         return true;
      }
      return false;
   }

   @Override
   public CacheEntry<K, V> getAndRemove(K key, CacheOptions options) {
      return toApiEntry(cache.removeAsyncEntry(key).join());
   }

   @Override
   public CloseableIterable<K> keys(CacheOptions options) {
      return EmbeddedUtil.closeableIterable(cache.keySet());
   }

   @Override
   public CloseableIterable<CacheEntry<K, V>> entries(CacheOptions options) {
      List<CacheEntry<K, V>> list = new ArrayList<>(cache.size());
      for (org.infinispan.container.entries.CacheEntry<K, V> e : cache.cacheEntrySet()) {
         list.add(new EmbeddedCacheEntry<>(e));
      }
      return EmbeddedUtil.closeableIterable(list);
   }

   @Override
   public void putAll(Map<K, V> entries, CacheWriteOptions options) {
      cache.putAll(entries, toMetadata(options));
   }

   @Override
   public Map<K, V> getAll(Set<K> keys, CacheOptions options) {
      return cache.getAll(keys);
   }

   @Override
   public Map<K, V> getAll(CacheOptions options, K... keys) {
      return cache.getAll(Set.of(keys));
   }

   @Override
   public Set<K> removeAll(Set<K> keys, CacheWriteOptions options) {
      return keys.stream()
            .filter(key -> cache.remove(key) != null)
            .collect(Collectors.toSet());
   }

   @Override
   public long estimateSize(CacheOptions options) {
      return cache.size();
   }

   @Override
   public void clear(CacheOptions options) {
      cache.clear();
   }

   @Override
   public <R> SyncQuery<K, V, R> query(String query, CacheOptions options) {
      return new EmbeddedSyncQuery<>(cache, cache.query(query), options);
   }

   @Override
   public SyncCacheListener<K, V> listen() {
      return new EmbeddedSyncCacheListener<>(cache);
   }

   @Override
   public <T> Set<CacheEntryProcessorResult<K, T>> process(Set<K> keys, SyncCacheEntryProcessor<K, V, T> processor, CacheProcessorOptions options) {
      CacheEntryProcessorContext context = new EmbeddedCacheEntryProcessorContext(options);
      Set<CacheEntryProcessorResult<K, T>> results = new HashSet<>();
      CacheStream<org.infinispan.container.entries.CacheEntry<K, V>> stream = cache.cacheEntrySet().stream().filterKeys(keys);
      options.timeout().ifPresent(t -> stream.timeout(t.toMillis(), TimeUnit.MILLISECONDS));
      stream.forEach((BiConsumer<Cache<K, V>, ? super org.infinispan.container.entries.CacheEntry<K, V>>) (c, entry) -> {
         K key = entry.getKey();
         try {
            T result = processor.process(new EmbeddedMutableCacheEntry<>(c.getAdvancedCache(), entry), context);
            results.add(CacheEntryProcessorResult.onResult(key, result));
         } catch (Throwable t) {
            results.add(CacheEntryProcessorResult.onError(key, t));
         }
      });
      return results;
   }

   @Override
   public <T> Set<CacheEntryProcessorResult<K, T>> processAll(SyncCacheEntryProcessor<K, V, T> processor, CacheProcessorOptions options) {
      CacheEntryProcessorContext context = new EmbeddedCacheEntryProcessorContext(options);
      Set<CacheEntryProcessorResult<K, T>> results = new HashSet<>();
      CacheStream<org.infinispan.container.entries.CacheEntry<K, V>> stream = cache.cacheEntrySet().stream();
      options.timeout().ifPresent(t -> stream.timeout(t.toMillis(), TimeUnit.MILLISECONDS));
      stream.forEach((BiConsumer<Cache<K, V>, ? super org.infinispan.container.entries.CacheEntry<K, V>>) (c, entry) -> {
         K key = entry.getKey();
         try {
            T result = processor.process(new EmbeddedMutableCacheEntry<>(c.getAdvancedCache(), entry), context);
            results.add(CacheEntryProcessorResult.onResult(key, result));
         } catch (Throwable t) {
            results.add(CacheEntryProcessorResult.onError(key, t));
         }
      });
      return results;
   }

   @Override
   public SyncStreamingCache<K> streaming() {
      return new EmbeddedSyncStreamingCache<>(cache);
   }

   private static <K, V> CacheEntry<K, V> toApiEntry(org.infinispan.container.entries.CacheEntry<K, V> entry) {
      return entry == null ? null : new EmbeddedCacheEntry<>(entry);
   }

   @Override
   public EmbeddedSyncCache<K, V> as(Subject subject) {
      return new EmbeddedSyncCache<>(embedded, cache.withSubject(subject));
   }

   @Override
   public SyncTransactionManager transactionManager() {
      TransactionManager tm = cache.getTransactionManager();
      return tm == null ? null : new EmbeddedSyncTransactionManager(tm);
   }

   private static Metadata toMetadata(CacheWriteOptions options) {
      CacheEntryExpiration expiration = options.expiration();
      if (expiration.isDefault()) {
         return null;
      }
      EmbeddedMetadata.Builder builder = new EmbeddedMetadata.Builder();
      expiration.lifespan().ifPresent(d -> builder.lifespan(d.toMillis(), TimeUnit.MILLISECONDS));
      expiration.maxIdle().ifPresent(d -> builder.maxIdle(d.toMillis(), TimeUnit.MILLISECONDS));
      return builder.build();
   }
}
