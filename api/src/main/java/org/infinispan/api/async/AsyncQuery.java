package org.infinispan.api.async;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import org.infinispan.api.async.events.cache.AsyncCacheContinuousQueryListener;
import org.infinispan.api.common.process.CacheEntryProcessorResult;
import org.infinispan.api.common.process.CacheProcessor;
import org.infinispan.api.common.process.CacheProcessorOptions;
import org.jspecify.annotations.Nullable;

/**
 * Parameterized Query builder
 *
 * @param <K>
 * @param <V>
 * @param <R> the result type for the query
 * @since 14.0
 */
public interface AsyncQuery<K, V, R> {
   /**
    * Sets the named parameter to the specified value
    *
    * @param name  the parameter name
    * @param value the value
    * @return this query builder
    */
   AsyncQuery<K, V, R> param(String name, @Nullable Object value);

   /**
    * Skips the first specified number of results
    *
    * @param skip the number of results to skip
    * @return this query builder
    */
   AsyncQuery<K, V, R> skip(long skip);

   /**
    * Limits the number of results
    *
    * @param limit the maximum number of results
    * @return this query builder
    */
   AsyncQuery<K, V, R> limit(int limit);

   /**
    * Executes the query
    */
   CompletionStage<AsyncQueryResult<R>> find();

   /**
    * Returns a builder for registering continuous query listeners.
    *
    * @return an {@link AsyncCacheContinuousQueryListener} builder
    */
   AsyncCacheContinuousQueryListener<K, R> findContinuously();

   /**
    * Executes the manipulation statement (UPDATE, REMOVE)
    *
    * @return the number of entries that were processed
    */
   CompletionStage<Long> execute();

   /**
    * Processes entries using the supplied entry processor.
    *
    * @param <T>
    * @param processor the entry processor task
    * @return the processing results
    */
   default <T> Flow.Publisher<CacheEntryProcessorResult<K, T>> process(AsyncCacheEntryProcessor<K, V, T> processor) {
      return process(processor, CacheProcessorOptions.DEFAULT);
   }

   /**
    * Processes entries using the supplied entry processor.
    *
    * @param <T>
    * @param processor the entry processor task
    * @param options   the options
    * @return the processing results
    */
   <T> Flow.Publisher<CacheEntryProcessorResult<K, T>> process(AsyncCacheEntryProcessor<K, V, T> processor, CacheProcessorOptions options);

   /**
    * Processes entries matched by the query using a named {@link CacheProcessor}. The query <b>MUST NOT</b> use
    * projections. If the cache processor returns a non-null value for an entry, it will be returned through the
    * publisher.
    *
    * @param <T>
    * @param processor the entry processor
    * @return the processing results
    */
   default <T> Flow.Publisher<CacheEntryProcessorResult<K, T>> process(CacheProcessor processor) {
      return process(processor, CacheProcessorOptions.DEFAULT);
   }

   /**
    * Processes entries matched by the query using a named {@link CacheProcessor}. The query <b>MUST NOT</b> use
    * projections. If the cache processor returns a non-null value for an entry, it will be returned through the
    * publisher.
    *
    * @param <T>
    * @param processor the named entry processor
    * @param options   the options
    * @return the processing results
    */
   <T> Flow.Publisher<CacheEntryProcessorResult<K, T>> process(CacheProcessor processor, CacheProcessorOptions options);
}
