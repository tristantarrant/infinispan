package org.infinispan.api.common;

/**
 * @param <K>
 * @param <V>
 */
public interface KeyValueEntry<K, V> {
   K key();

   V value();
}
