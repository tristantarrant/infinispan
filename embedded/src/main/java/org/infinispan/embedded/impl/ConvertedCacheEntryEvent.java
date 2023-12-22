package org.infinispan.embedded.impl;

import java.util.function.Function;

import org.infinispan.api.common.CacheEntry;
import org.infinispan.api.common.CacheEntryMetadata;
import org.infinispan.api.common.events.cache.CacheEntryEvent;
import org.infinispan.api.common.events.cache.CacheEntryEventType;

/**
 * Wraps a {@link CacheEntryEvent} and applies a converter function to entry values.
 *
 * @since 16.3
 */
class ConvertedCacheEntryEvent<K, V, C> implements CacheEntryEvent<K, C> {
   private final CacheEntryEvent<K, V> delegate;
   private final Function<V, C> converter;

   ConvertedCacheEntryEvent(CacheEntryEvent<K, V> delegate, Function<V, C> converter) {
      this.delegate = delegate;
      this.converter = converter;
   }

   @Override
   public CacheEntry<K, C> newEntry() {
      CacheEntry<K, V> entry = delegate.newEntry();
      if (entry == null) {
         return null;
      }
      return new SimpleCacheEntry<>(entry.key(), converter.apply(entry.value()));
   }

   @Override
   public CacheEntry<K, C> previousEntry() {
      CacheEntry<K, V> entry = delegate.previousEntry();
      if (entry == null) {
         return null;
      }
      return new SimpleCacheEntry<>(entry.key(), converter.apply(entry.value()));
   }

   @Override
   public boolean isCurrentState() {
      return delegate.isCurrentState();
   }

   @Override
   public Object getSource() {
      return delegate.getSource();
   }

   @Override
   public boolean isOriginLocal() {
      return delegate.isOriginLocal();
   }

   @Override
   public CacheEntryEventType type() {
      return delegate.type();
   }

   private record SimpleCacheEntry<K, V>(K key, V value) implements CacheEntry<K, V> {
      @Override
      public CacheEntryMetadata metadata() {
         return null;
      }
   }
}
