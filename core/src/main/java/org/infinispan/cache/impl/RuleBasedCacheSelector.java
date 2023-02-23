package org.infinispan.cache.impl;

import java.util.Collection;

import org.infinispan.cache.CacheSelectionContext;
import org.infinispan.cache.CacheSelector;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.registry.InternalCacheRegistry;

/**
 * @since 15.0
 **/
@Scope(Scopes.GLOBAL)
public abstract class RuleBasedCacheSelector implements CacheSelector {
   @Inject
   InternalCacheRegistry internalCacheRegistry;

   public abstract Collection<CacheSelectionRule> rules();


   @Override
   public String selectCache(CacheSelectionContext context) {
      if (!internalCacheRegistry.isInternalCache(context.name())) {
         for (CacheSelectionRule rule : rules()) {
            String name = rule.evaluate(context);
            if (name != null) {
               return name;
            }
         }
      }
      // No matching rules, return the cache name as-is
      return context.name();
   }
}
