package org.infinispan.embedded.impl;

import org.infinispan.AdvancedCache;
import org.infinispan.api.common.CacheEntryMetadata;
import org.infinispan.api.common.MutableCacheEntry;

/**
 * @since 16.3
 */
public class EmbeddedMutableCacheEntry<K, V> implements MutableCacheEntry<K, V> {
   private final AdvancedCache<K, V> cache;
   private final K key;
   private V value;
   private final CacheEntryMetadata metadata;

   public EmbeddedMutableCacheEntry(AdvancedCache<K, V> cache, org.infinispan.container.entries.CacheEntry<K, V> entry) {
      this.cache = cache;
      this.key = entry.getKey();
      this.value = entry.getValue();
      this.metadata = new EmbeddedCacheEntry<>(entry).metadata();
   }

   @Override
   public K key() {
      return key;
   }

   @Override
   public V value() {
      return value;
   }

   @Override
   public CacheEntryMetadata metadata() {
      return metadata;
   }

   @Override
   public void setValue(V newValue) {
      this.value = newValue;
      cache.put(key, newValue);
   }
}
