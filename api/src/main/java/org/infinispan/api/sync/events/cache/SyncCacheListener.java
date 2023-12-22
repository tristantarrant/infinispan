package org.infinispan.api.sync.events.cache;

import java.io.Closeable;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.infinispan.api.common.events.cache.CacheEntryEvent;
import org.infinispan.api.common.events.cache.CacheListenerOptions;
import org.jspecify.annotations.Nullable;

/**
 * A builder for registering cache entry event listeners. Obtained via {@link
 * org.infinispan.api.sync.SyncCache#listen()}. Call one or more of the {@code onCreate}, {@code onUpdate}, {@code
 * onRemove}, {@code onExpired} methods to register callbacks for the desired event types, then call {@link #install()}
 * to activate the listener.
 *
 * <pre>{@code
 * AutoCloseable registration = cache.listen()
 *    .onCreate(event -> System.out.println("Created: " + event.newEntry().key()))
 *    .onRemove(event -> System.out.println("Removed: " + event.previousEntry().key()))
 *    .install();
 * }</pre>
 *
 * @since 15.1
 */
public abstract class SyncCacheListener<K, V> {
   protected Consumer<CacheEntryEvent<K, V>> onCreate;
   protected Consumer<CacheEntryEvent<K, V>> onUpdate;
   protected Consumer<CacheEntryEvent<K, V>> onRemove;
   protected Consumer<CacheEntryEvent<K, V>> onExpired;
   protected Predicate<CacheEntryEvent<K, V>> filter;
   // Should this be in CacheListenerOptions instead?
   protected String filterName;
   protected Map<String, Object> filterParams;
   protected Function<?, ?> converter;
   // Should this be in CacheListenerOptions instead?
   protected String converterName;
   protected Map<String, Object> converterParams;
   protected CacheListenerOptions options = new CacheListenerOptions();

   public SyncCacheListener<K, V> onCreate(Consumer<CacheEntryEvent<K, V>> listener) {
      this.onCreate = listener;
      return this;
   }

   public SyncCacheListener<K, V> onUpdate(Consumer<CacheEntryEvent<K, V>> listener) {
      this.onUpdate = listener;
      return this;
   }

   public SyncCacheListener<K, V> onRemove(Consumer<CacheEntryEvent<K, V>> listener) {
      this.onRemove = listener;
      return this;
   }

   public SyncCacheListener<K, V> onExpired(Consumer<CacheEntryEvent<K, V>> listener) {
      this.onExpired = listener;
      return this;
   }

   public SyncCacheListener<K, V> filter(Predicate<CacheEntryEvent<K, V>> filter) {
      this.filter = filter;
      return this;
   }

   // Should this be in CacheListenerOptions instead?
   public SyncCacheListener<K, V> filter(String name, @Nullable Map<String, Object> params) {
      this.filterName = name;
      this.filterParams = params;
      return this;
   }

   @SuppressWarnings("unchecked")
   public <C> SyncCacheListener<K, C> converter(Function<V, C> converter) {
      this.converter = converter;
      return (SyncCacheListener<K, C>) this;
   }

   // Should this be in CacheListenerOptions instead?
   @SuppressWarnings("unchecked")
   public <C> SyncCacheListener<K, C> converter(String name, Class<C> type, @Nullable Map<String, Object> params) {
      this.converterName = name;
      this.converterParams = params;
      return (SyncCacheListener<K, C>) this;
   }

   public SyncCacheListener<K, V> options(CacheListenerOptions options) {
      this.options = options;
      return this;
   }

   /**
    * Activates the listener and returns a handle that can be used to remove it.
    *
    * @return a {@link Closeable} whose {@code close()} method removes the listener
    */
   public abstract Closeable install();
}
