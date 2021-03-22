package org.infinispan.api.common.events.cache;

/**
 * @since 14.0
 */
public interface CacheContinuousQueryEvent<K, V> {

   EventType type();

   K key();

   V value();

   enum EventType {
      JOIN,
      UPDATE,
      LEAVE
   }
}
