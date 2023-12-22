package org.infinispan.api.sync;

import org.infinispan.api.common.MutableCacheEntry;
import org.infinispan.api.common.process.CacheEntryProcessorContext;
import org.jspecify.annotations.Nullable;

/**
 * @since 14.0
 **/
@FunctionalInterface
public interface SyncCacheEntryProcessor<K, V, T> {
   @Nullable T process(MutableCacheEntry<K, V> entry, CacheEntryProcessorContext context);
}
