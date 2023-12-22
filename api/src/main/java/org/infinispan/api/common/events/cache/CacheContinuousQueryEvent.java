package org.infinispan.api.common.events.cache;

import org.jspecify.annotations.Nullable;

/**
 * @param <K> the type of key
 * @param <V> the type of value
 * @since 14.0
 */
public interface CacheContinuousQueryEvent<K, V> { // introduce subinterfaces

   EventType type();

   K key();

   @Nullable V value();

   enum EventType { // Drop the enum and
      JOIN,
      UPDATE,
      LEAVE
   }
}
