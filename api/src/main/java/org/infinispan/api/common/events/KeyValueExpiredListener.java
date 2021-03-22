package org.infinispan.api.common.events;

/**
 *
 * @since 13.0
 **/
public interface KeyValueExpiredListener<K, V> extends KeyValueListener<K, V> {
   void onExpired(KeyValueEvent<K, V> event);
}
