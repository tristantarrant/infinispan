package org.infinispan.cache;

/**
 * @since 15.0
 **/
public interface CacheSelector {
   String selectCache(CacheSelectionContext context);
}
