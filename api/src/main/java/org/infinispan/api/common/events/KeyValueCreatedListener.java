package org.infinispan.api.common.events;

/**
 *
 * @since 13.0
 **/
public interface KeyValueCreatedListener<K, V> extends KeyValueListener<K, V> {
   void onCreate(KeyValueEvent<K, V> event);
}
