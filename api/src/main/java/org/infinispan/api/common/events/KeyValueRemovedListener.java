package org.infinispan.api.common.events;

/**
 *
 * @since 13.0
 **/
public interface KeyValueRemovedListener<K, V> extends KeyValueListener<K, V> {
   void onRemove(KeyValueEvent<K, V> event);
}
