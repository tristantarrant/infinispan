package org.infinispan.api.common.events;

import org.infinispan.api.common.KeyValueEntry;

/**
 *
 * @since 13.0
 **/
public interface KeyValueEvent<K, V> {
   KeyValueEntry<K, V> entry();
}
