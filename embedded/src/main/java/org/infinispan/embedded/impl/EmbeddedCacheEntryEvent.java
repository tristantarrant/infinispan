package org.infinispan.embedded.impl;

import org.infinispan.api.common.CacheEntry;
import org.infinispan.api.common.events.cache.CacheEntryEventType;
import org.infinispan.notifications.cachelistener.event.impl.EventImpl;

/**
 * Adapts the internal {@link EventImpl} to the new API's
 * {@link org.infinispan.api.common.events.cache.CacheEntryEvent} interface.
 *
 * @since 16.3
 */
public class EmbeddedCacheEntryEvent<K, V> implements org.infinispan.api.common.events.cache.CacheEntryEvent<K, V> {
   private final EventImpl<K, V> delegate;

   public EmbeddedCacheEntryEvent(EventImpl<K, V> delegate) {
      this.delegate = delegate;
   }

   @Override
   public CacheEntry<K, V> newEntry() {
      V value = delegate.getNewValue() != null ? delegate.getNewValue() : delegate.getValue();
      if (value == null && delegate.getKey() == null) {
         return null;
      }
      return new SimpleCacheEntry<>(delegate.getKey(), value);
   }

   @Override
   public CacheEntry<K, V> previousEntry() {
      V oldValue = delegate.getOldValue();
      if (oldValue == null) {
         return null;
      }
      return new SimpleCacheEntry<>(delegate.getKey(), oldValue);
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
      return switch (delegate.getType()) {
         case CACHE_ENTRY_CREATED -> CacheEntryEventType.CREATED;
         case CACHE_ENTRY_MODIFIED -> CacheEntryEventType.UPDATED;
         case CACHE_ENTRY_REMOVED -> CacheEntryEventType.REMOVED;
         case CACHE_ENTRY_EXPIRED -> CacheEntryEventType.EXPIRED;
         default -> throw new IllegalStateException("Unexpected event type: " + delegate.getType());
      };
   }

   private record SimpleCacheEntry<K, V>(K key, V value) implements CacheEntry<K, V> {
      @Override
      public org.infinispan.api.common.CacheEntryMetadata metadata() {
         return null;
      }
   }
}
