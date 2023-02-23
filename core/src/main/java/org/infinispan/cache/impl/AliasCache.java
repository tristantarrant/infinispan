package org.infinispan.cache.impl;

import org.infinispan.AdvancedCache;

/**
 * @since 15.0
 **/
public class AliasCache<K, V> extends AbstractDelegatingAdvancedCache<K, V> {
   private final String alias;

   public AliasCache(AdvancedCache<K, V> cache, String alias) {
      super(cache);
      this.alias = alias;
   }

   @Override
   public AdvancedCache rewrap(AdvancedCache newDelegate) {
      throw new UnsupportedOperationException("Alias caches should not delegate wrapping operations");
   }

   @Override
   public String getName() {
      return alias;
   }
}
