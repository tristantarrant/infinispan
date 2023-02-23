package org.infinispan.cache.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;

/**
 * @since 15.0
 **/
@Scope(Scopes.GLOBAL)
public class StaticCacheSelector extends RuleBasedCacheSelector {

   private final List<CacheSelectionRule> rules;

   public StaticCacheSelector(List<CacheSelectionRule> rules) {
      this.rules = Collections.unmodifiableList(rules);
   }

   @Override
   public Collection<CacheSelectionRule> rules() {
      return rules;
   }
}
