package org.infinispan.api.common.process;

import org.infinispan.api.Experimental;
import org.jspecify.annotations.Nullable;

/**
 * Write result for process operations on the Cache
 *
 * @param <K> the type of key
 * @param <T> the type of the processing result
 * @since 14.0
 */
@Experimental
public interface CacheEntryProcessorResult<K, T> {
   K key();

   @Nullable T result();

   @Nullable Throwable error();

   static <K, T> CacheEntryProcessorResult<K, T> onResult(K key, @Nullable T result) {
      return new Impl<>(key, result, null);
   }

   static <K, T> CacheEntryProcessorResult<K, T> onError(K key, Throwable throwable) {
      return new Impl<>(key, null, throwable);
   }

   class Impl<K, T> implements CacheEntryProcessorResult<K, T> {
      private final K key;
      private final @Nullable T result;
      private final @Nullable Throwable throwable;

      public Impl(K key, @Nullable T result, @Nullable Throwable throwable) {
         this.key = key;
         this.result = result;
         this.throwable = throwable;
      }

      @Override
      public K key() {
         return key;
      }

      @Override
      public @Nullable T result() {
         return result;
      }

      @Override
      public @Nullable Throwable error() {
         return throwable;
      }
   }
}
