package org.infinispan.api.sync;

import java.util.Map;

import org.infinispan.api.common.process.CacheProcessor;
import org.infinispan.api.common.process.CacheProcessorOptions;
import org.infinispan.api.sync.events.cache.SyncCacheContinuousQueryListener;
import org.jspecify.annotations.Nullable;

/**
 * Parameterized Query builder
 *
 * @param <K>
 * @param <V>
 * @param <R> the result type for the query
 */
public interface SyncQuery<K, V, R> {
   /**
    * Sets the named parameter to the specified value
    *
    * @param name  the parameter name
    * @param value the value
    * @return this query builder
    */
   SyncQuery<K, V, R> param(String name, @Nullable Object value);

   /**
    * Skips the first specified number of results
    *
    * @param skip the number of results to skip
    * @return this query builder
    */
   SyncQuery<K, V, R> skip(long skip);

   /**
    * Limits the number of results
    *
    * @param limit the maximum number of results
    * @return this query builder
    */
   SyncQuery<K, V, R> limit(int limit);

   /**
    * Executes the query
    */
   SyncQueryResult<R> find();

   /**
    * Returns a builder for registering continuous query listeners.
    *
    * @return a {@link SyncCacheContinuousQueryListener} builder
    */
   SyncCacheContinuousQueryListener<K, R> findContinuously();

   /**
    * Executes the manipulation statement (UPDATE, REMOVE)
    *
    * @return the number of entries that were processed
    */
   int execute();

   /**
    * Processes entries using an {@link SyncCacheEntryProcessor}. If the cache is embedded, the consumer will be executed
    * locally on the owner of the entry. If the cache is remote, entries will be retrieved, manipulated locally and put
    * back. The query <b>MUST NOT</b> use projections.
    *
    * @param processor the entry processor
    */
   default <T> Map<K, T> process(SyncCacheEntryProcessor<K, V, T> processor) {
      return process(processor, CacheProcessorOptions.DEFAULT);
   }

   /**
    * Processes entries using a {@link SyncCacheEntryProcessor}. If the cache is embedded, the consumer will be executed
    * locally on the owner of the entry. If the cache is remote, entries will be retrieved, manipulated locally and put
    * back. The query <b>MUST NOT</b> use projections.
    *
    * @param <T>
    * @param processor the entry processor
    * @param options   the options
    */
   <T> Map<K, T> process(SyncCacheEntryProcessor<K, V, T> processor, CacheProcessorOptions options);

   /**
    * Processes entries matched by the query using a named {@link CacheProcessor}. The query <b>MUST NOT</b> use projections.
    * If the cache processor returns a non-null value for an entry, it will be returned as an entry of a {@link Map}.
    *
    * @param processor the entry processor
    * @return the processing results
    */
   default <T> Map<K, T> process(CacheProcessor processor) {
      return process(processor, CacheProcessorOptions.DEFAULT);
   }

   /**
    * Processes entries matched by the query using a named {@link CacheProcessor}. The query <b>MUST NOT</b> use projections.
    * If the cache processor returns a non-null value for an entry, it will be returned as an entry of a {@link Map}.
    *
    * @param <T>
    * @param processor the named entry processor
    * @param options   the options
    * @return the processing results
    */
   <T> Map<K, T> process(CacheProcessor processor, CacheProcessorOptions options);
}
