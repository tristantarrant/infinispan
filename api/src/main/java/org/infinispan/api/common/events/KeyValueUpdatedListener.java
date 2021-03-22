package org.infinispan.api.common.events;

/**
 *
 * @since 13.0
 **/
public interface KeyValueUpdatedListener<K, V> extends KeyValueListener<K, V> {
   void onUpdate(KeyValueEvent<K, V> event);
}
