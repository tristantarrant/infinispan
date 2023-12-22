package org.infinispan.embedded;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.api.async.AsyncCacheEntryProcessor;
import org.infinispan.api.async.AsyncQuery;
import org.infinispan.api.async.AsyncQueryResult;
import org.infinispan.api.async.events.cache.AsyncCacheContinuousQueryListener;
import org.infinispan.api.common.CacheOptions;
import org.infinispan.api.common.process.CacheEntryProcessorResult;
import org.infinispan.api.common.process.CacheProcessor;
import org.infinispan.api.common.process.CacheProcessorOptions;
import org.infinispan.commons.api.query.ContinuousQuery;
import org.infinispan.commons.api.query.ContinuousQueryListener;
import org.infinispan.commons.api.query.Query;

/**
 * @since 16.3
 */
public class EmbeddedAsyncQuery<K, V, R> implements AsyncQuery<K, V, R> {
   private final Cache<K, V> cache;
   private final Query<R> query;

   EmbeddedAsyncQuery(Cache<K, V> cache, Query<R> query, CacheOptions options) {
      this.cache = cache;
      this.query = query;
      options.timeout().ifPresent(d -> query.timeout(d.toNanos(), TimeUnit.NANOSECONDS));
   }

   @Override
   public AsyncQuery<K, V, R> param(String name, Object value) {
      query.setParameter(name, value);
      return this;
   }

   @Override
   public AsyncQuery<K, V, R> skip(long skip) {
      query.startOffset(skip);
      return this;
   }

   @Override
   public AsyncQuery<K, V, R> limit(int limit) {
      query.maxResults(limit);
      return this;
   }

   @Override
   public CompletionStage<AsyncQueryResult<R>> find() {
      return null;
   }

   @Override
   public AsyncCacheContinuousQueryListener<K, R> findContinuously() {
      return new AsyncCacheContinuousQueryListener<>() {
         @Override
         public CompletionStage<Closeable> install() {
            ContinuousQuery<K, V> cq = cache.continuousQuery();
            ContinuousQueryListener<K, R> wrapped = new ContinuousQueryListener<>() {
               @Override
               public void resultJoining(K key, R value) {
                  if (onJoin != null) onJoin.apply(key, value);
               }

               @Override
               public void resultUpdated(K key, R value) {
                  if (onUpdate != null) onUpdate.apply(key, value);
               }

               @Override
               public void resultLeaving(K key) {
                  if (onLeave != null) onLeave.apply(key);
               }
            };
            cq.addContinuousQueryListener(query, wrapped);
            Closeable handle = () -> cq.removeContinuousQueryListener(wrapped);
            return CompletableFuture.completedFuture(handle);
         }
      };
   }

   @Override
   public CompletionStage<Long> execute() {
      return null;
   }

   @Override
   public <T> Flow.Publisher<CacheEntryProcessorResult<K, T>> process(AsyncCacheEntryProcessor<K, V, T> processor, CacheProcessorOptions options) {
      return null;
   }

   @Override
   public <T> Flow.Publisher<CacheEntryProcessorResult<K, T>> process(CacheProcessor processor, CacheProcessorOptions options) {
      return null;
   }
}
