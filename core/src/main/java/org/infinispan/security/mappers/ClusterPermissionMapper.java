package org.infinispan.security.mappers;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.context.Flag;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.registry.InternalCacheRegistry;
import org.infinispan.security.AuthorizationMapperContext;
import org.infinispan.security.MutableRolePermissionMapper;
import org.infinispan.security.Role;
import org.infinispan.util.concurrent.BlockingManager;

/**
 * ClusterPermissionMapper. This class implements both a {@link MutableRolePermissionMapper} storing the mappings in a
 * persistent replicated internal cache named <tt>org.infinispan.PERMISSIONS</tt>
 *
 * @author Tristan Tarrant
 * @since 14.0
 */
public class ClusterPermissionMapper implements MutableRolePermissionMapper {
   private static final String CLUSTER_PERMISSION_MAPPER_CACHE = "org.infinispan.PERMISSIONS";
   private EmbeddedCacheManager cacheManager;
   private BlockingManager blockingManager;
   private Cache<String, Role> clusterPermissionMap;

   private Cache<String, Role> getClusterPermissionMap() {
      if (clusterPermissionMap == null) {
         if (cacheManager != null) {
            clusterPermissionMap = cacheManager.getCache(CLUSTER_PERMISSION_MAPPER_CACHE);
            clusterPermissionMap = clusterPermissionMap.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD);
         }
      }
      return clusterPermissionMap;
   }

   @Override
   public void setContext(AuthorizationMapperContext context) {
      this.cacheManager = context.getCacheManager();
      GlobalConfiguration globalConfiguration = SecurityActions.getCacheManagerConfiguration(cacheManager);
      CacheMode cacheMode = globalConfiguration.isClustered() ? CacheMode.REPL_SYNC : CacheMode.LOCAL;
      ConfigurationBuilder cfg = new ConfigurationBuilder();
      cfg.clustering().cacheMode(cacheMode)
            .stateTransfer().fetchInMemoryState(true).awaitInitialTransfer(false)
            .security().authorization().disable();
      GlobalComponentRegistry gcr = SecurityActions.getGlobalComponentRegistry(cacheManager);
      InternalCacheRegistry internalCacheRegistry = gcr.getComponent(InternalCacheRegistry.class);
      internalCacheRegistry.registerInternalCache(CLUSTER_PERMISSION_MAPPER_CACHE, cfg.build(), EnumSet.of(InternalCacheRegistry.Flag.PERSISTENT));
      blockingManager = gcr.getComponent(BlockingManager.class);
   }

   @Override
   public CompletionStage<Void> addRole(Role role) {
      return getClusterPermissionMap().putAsync(role.getName(), role).thenApply(ignore -> null);
   }

   @Override
   public CompletionStage<Void> removeRole(String name) {
      return getClusterPermissionMap().removeAsync(name).thenApply(ignore -> null);
   }

   @Override
   public Map<String, Role> getAllRoles() {
      return isActive() ? getClusterPermissionMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)) : Collections.emptyMap();
   }

   @Override
   public Role getRole(String name) {
      return isActive() ? getClusterPermissionMap().get(name) : null;
   }

   @Override
   public boolean hasRole(String name) {
      return isActive() ? getClusterPermissionMap().containsKey(name) : false;
   }

   private boolean isActive() {
      return cacheManager != null && cacheManager.getStatus().allowInvocations();
   }
}
