package org.infinispan.api.common.tasks;

import java.util.function.Consumer;

import org.infinispan.api.common.CacheEntry;

/**
 * @since 14.0
 **/
public interface EntryConsumerTask<K, V> extends Consumer<CacheEntry<K, V>> {
}
