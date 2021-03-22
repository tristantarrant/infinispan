package org.infinispan.api.sync;

import org.infinispan.api.configuration.MultiMapConfiguration;

/**
 *
 * @param <K>
 * @param <V>
 * @since 13.0
 */
public interface MultiMap<K, V> {

   String name();

   MultiMapConfiguration configuration();

   void add(K key, V value);

   Iterable<V> get(K key);

   boolean remove(K key);

   boolean remove(K key, V value);

   boolean containsKey(K key);

   boolean containsValue(V value);

   boolean containsEntry(K key, V value);

   long estimateSize();
}
