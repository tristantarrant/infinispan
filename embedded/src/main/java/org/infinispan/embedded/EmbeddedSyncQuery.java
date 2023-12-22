package org.infinispan.embedded;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.api.common.CacheOptions;
import org.infinispan.api.common.process.CacheProcessor;
import org.infinispan.api.common.process.CacheProcessorOptions;
import org.infinispan.api.sync.SyncCacheEntryProcessor;
import org.infinispan.api.sync.SyncQuery;
import org.infinispan.api.sync.SyncQueryResult;
import org.infinispan.api.sync.events.cache.SyncCacheContinuousQueryListener;
import org.infinispan.commons.api.query.ContinuousQuery;
import org.infinispan.commons.api.query.ContinuousQueryListener;
import org.infinispan.commons.api.query.Query;

/**
 * @since 16.3
 */
public class EmbeddedSyncQuery<K, V, R> implements SyncQuery<K, V, R> {
   private final Cache<K, V> cache;
   private final Query<R> query;

   EmbeddedSyncQuery(Cache<K, V> cache, Query<R> query, CacheOptions options) {
      this.cache = cache;
      this.query = query;
      options.timeout().ifPresent(d -> query.timeout(d.toNanos(), TimeUnit.NANOSECONDS));
   }

   @Override
   public SyncQuery<K, V, R> param(String name, Object value) {
      query.setParameter(name, value);
      return this;
   }

   @Override
   public SyncQuery<K, V, R> skip(long skip) {
      query.startOffset(skip);
      return this;
   }

   @Override
   public SyncQuery<K, V, R> limit(int limit) {
      query.maxResults(limit);
      return this;
   }

   @Override
   public SyncQueryResult<R> find() {
      return new EmbeddedSyncQueryResult<>(query.execute());
   }

   @Override
   public SyncCacheContinuousQueryListener<K, R> findContinuously() {
      return new SyncCacheContinuousQueryListener<>() {
         @Override
         public Closeable install() {
            ContinuousQuery<K, V> cq = cache.continuousQuery();
            ContinuousQueryListener<K, R> wrapped = new ContinuousQueryListener<>() {
               @Override
               public void resultJoining(K key, R value) {
                  if (onJoin != null) onJoin.accept(key, value);
               }

               @Override
               public void resultUpdated(K key, R value) {
                  if (onUpdate != null) onUpdate.accept(key, value);
               }

               @Override
               public void resultLeaving(K key) {
                  if (onLeave != null) onLeave.accept(key);
               }
            };
            cq.addContinuousQueryListener(query, wrapped);
            return () -> cq.removeContinuousQueryListener(wrapped);
         }
      };
   }

   @Override
   public int execute() {
      return query.executeStatement();
   }

   @Override
   public <T> Map<K, T> process(SyncCacheEntryProcessor<K, V, T> processor, CacheProcessorOptions options) {
      return null;
   }

   @Override
   public <T> Map<K, T> process(CacheProcessor processor, CacheProcessorOptions options) {
      return null;
   }
}
