package org.infinispan.api.common;

import java.util.Collection;

/**
 * @param <K> the type of key
 * @param <V> the type of value
 * @since 14.0
 **/
public interface CacheEntryCollection<K, V> {
   K key();

   Collection<V> values();

   CacheEntryMetadata metadata();
}
