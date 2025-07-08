package org.infinispan.server.configuration.security;

import java.security.Principal;
import java.util.concurrent.TimeUnit;

import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.cache.RealmIdentityCache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class CaffeineRealmIdentityCache implements RealmIdentityCache {

   private final Cache<Principal, RealmIdentity> cache;

   public CaffeineRealmIdentityCache(int maxEntries, long maxAge) {
      cache = Caffeine.newBuilder().maximumSize(maxEntries).expireAfterWrite(maxAge, TimeUnit.MILLISECONDS).build();
   }

   @Override
   public void put(Principal principal, RealmIdentity realmIdentity) {
      cache.put(principal, realmIdentity);
   }

   @Override
   public RealmIdentity get(Principal principal) {
      return cache.getIfPresent(principal);
   }

   @Override
   public void remove(Principal principal) {
      cache.invalidate(principal);
   }

   @Override
   public void clear() {
      cache.invalidateAll();
   }
}
