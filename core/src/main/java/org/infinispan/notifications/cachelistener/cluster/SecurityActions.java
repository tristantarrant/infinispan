package org.infinispan.notifications.cachelistener.cluster;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.manager.ClusterExecutor;
import org.infinispan.security.Security;
import org.infinispan.security.actions.GetCacheComponentRegistryAction;
import org.infinispan.security.actions.GetClusterExecutorAction;

/**
 * SecurityActions for the org.infinispan.notifications.cachelistener.cluster package.
 *
 * Do not move. Do not change class and method visibility to avoid being called from other
 * {@link java.security.CodeSource}s, thus granting privilege escalation to external code.
 *
 * @author Tristan Tarrant
 * @since 7.1
 */
final class SecurityActions {
   private static <T> T doPrivileged(PrivilegedAction<T> action) {
      if (System.getSecurityManager() != null) {
         return AccessController.doPrivileged(action);
      } else {
         return Security.doPrivileged(action);
      }
   }

   static ClusterExecutor getClusterExecutor(final Cache<?, ?> cache) {
      GetClusterExecutorAction action = new GetClusterExecutorAction(cache);
      return doPrivileged(action);
   }

   static <K, V> ComponentRegistry getComponentRegistry(AdvancedCache<K, V> cache) {
      return doPrivileged(new GetCacheComponentRegistryAction(cache));
   }
}
