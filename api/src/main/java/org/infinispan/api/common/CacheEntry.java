package org.infinispan.api.common;

/**
 * @param <K> the type of key
 * @param <V> the type of value
 * @since 14.0
 **/
public interface CacheEntry<K, V> {
   K key();

   V value();

   CacheEntryMetadata metadata();
}
