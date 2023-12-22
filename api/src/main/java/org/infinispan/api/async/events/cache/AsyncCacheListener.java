package org.infinispan.api.async.events.cache;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

import org.infinispan.api.common.events.cache.CacheEntryEvent;
import org.infinispan.api.common.events.cache.CacheListenerOptions;
import org.jspecify.annotations.Nullable;

/**
 * A builder for registering asynchronous cache entry event listeners. Obtained via {@link
 * org.infinispan.api.async.AsyncCache#listen()}. Call one or more of the {@code onCreate}, {@code onUpdate}, {@code
 * onRemove}, {@code onExpired} methods to register callbacks for the desired event types, then call {@link #install()}
 * to activate the listener.
 *
 * <pre>{@code
 * cache.listen()
 *    .onCreate(event -> CompletableFuture.runAsync(() -> log("Created: " + event.newEntry().key())))
 *    .onRemove(event -> CompletableFuture.runAsync(() -> log("Removed: " + event.previousEntry().key())))
 *    .install();
 * }</pre>
 *
 * @since 15.1
 */
public abstract class AsyncCacheListener<K, V> {
   protected Function<CacheEntryEvent<K, V>, CompletionStage<Void>> onCreate;
   protected Function<CacheEntryEvent<K, V>, CompletionStage<Void>> onUpdate;
   protected Function<CacheEntryEvent<K, V>, CompletionStage<Void>> onRemove;
   protected Function<CacheEntryEvent<K, V>, CompletionStage<Void>> onExpired;
   protected Predicate<CacheEntryEvent<K, V>> filter;
   // Should this be in CacheListenerOptions instead?
   protected String filterName;
   protected Map<String, Object> filterParams;
   protected Function<?, ?> converter;
   // Should this be in CacheListenerOptions instead?
   protected String converterName;
   protected Map<String, Object> converterParams;
   protected CacheListenerOptions options = new CacheListenerOptions();

   public AsyncCacheListener<K, V> onCreate(Function<CacheEntryEvent<K, V>, CompletionStage<Void>> listener) {
      this.onCreate = listener;
      return this;
   }

   public AsyncCacheListener<K, V> onUpdate(Function<CacheEntryEvent<K, V>, CompletionStage<Void>> listener) {
      this.onUpdate = listener;
      return this;
   }

   public AsyncCacheListener<K, V> onRemove(Function<CacheEntryEvent<K, V>, CompletionStage<Void>> listener) {
      this.onRemove = listener;
      return this;
   }

   public AsyncCacheListener<K, V> onExpired(Function<CacheEntryEvent<K, V>, CompletionStage<Void>> listener) {
      this.onExpired = listener;
      return this;
   }

   public AsyncCacheListener<K, V> filter(Predicate<CacheEntryEvent<K, V>> filter) {
      this.filter = filter;
      return this;
   }

   // Should this be in CacheListenerOptions instead?
   public AsyncCacheListener<K, V> filter(String name, @Nullable Map<String, Object> params) {
      this.filterName = name;
      this.filterParams = params;
      return this;
   }

   @SuppressWarnings("unchecked")
   public <C> AsyncCacheListener<K, C> converter(Function<V, C> converter) {
      this.converter = converter;
      return (AsyncCacheListener<K, C>) this;
   }

   // Should this be in CacheListenerOptions instead?
   @SuppressWarnings("unchecked")
   public <C> AsyncCacheListener<K, C> converter(String name, Class<C> type, @Nullable Map<String, Object> params) {
      this.converterName = name;
      this.converterParams = params;
      return (AsyncCacheListener<K, C>) this;
   }

   public AsyncCacheListener<K, V> options(CacheListenerOptions options) {
      this.options = options;
      return this;
   }

   /**
    * Activates the listener and returns a handle that can be used to remove it.
    *
    * @return a {@link CompletionStage} that completes with a {@link Closeable} whose {@code close()} method removes
    * the listener
    */
   public abstract CompletionStage<Closeable> install();
}
