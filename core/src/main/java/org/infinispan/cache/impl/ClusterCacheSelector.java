package org.infinispan.cache.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.Cache;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.globalstate.GlobalConfigurationManager;
import org.infinispan.globalstate.ScopedState;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.security.AuthorizationPermission;
import org.infinispan.security.actions.SecurityActions;

/**
 * @since 15.0
 **/
@Scope(Scopes.GLOBAL)
public class ClusterCacheSelector extends RuleBasedCacheSelector {
   private static final String CACHE_SELECTOR_STATE = "cache-selector-state";
   private static final ScopedState CACHE_SELECTOR_SCOPE = new ScopedState(CACHE_SELECTOR_STATE, null);
   @Inject
   EmbeddedCacheManager cacheManager;
   @Inject
   GlobalConfigurationManager globalConfigurationManager;
   private Cache<ScopedState, List<CacheSelectionRule>> stateCache;

   public ClusterCacheSelector() {

   }

   @Start
   void start() {
      stateCache = globalConfigurationManager.getStateCache();
      stateCache.putIfAbsent(CACHE_SELECTOR_SCOPE, new ArrayList<>());
   }


   public CompletionStage<Void> addRule(CacheSelectionRule rule) {
      SecurityActions.checkPermission(cacheManager, AuthorizationPermission.ADMIN);
      return stateCache.computeAsync(CACHE_SELECTOR_SCOPE, (__, rules) -> {
         rules.add(rule);
         return rules;
      }).thenApply(__ -> null);
   }

   public CompletionStage<Void> deleteAllRules() {
      SecurityActions.checkPermission(cacheManager, AuthorizationPermission.ADMIN);
      return stateCache.putAsync(CACHE_SELECTOR_SCOPE, new ArrayList<>()).thenApply(__ -> null);
   }

   public CompletionStage<Void> deleteRule(int index) {
      SecurityActions.checkPermission(cacheManager, AuthorizationPermission.ADMIN);
      return stateCache.computeAsync(CACHE_SELECTOR_SCOPE, (__, rules) -> {
         rules.remove(index);
         return rules;
      }).thenApply(__ -> null);
   }


   @Override
   public Collection<CacheSelectionRule> rules() {
      return stateCache.get(CACHE_SELECTOR_SCOPE);
   }
}
