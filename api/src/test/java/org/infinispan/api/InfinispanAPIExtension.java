package org.infinispan.api;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.infinispan.api.async.AsyncCache;
import org.infinispan.api.configuration.AdminFlag;
import org.infinispan.api.configuration.CacheConfiguration;
import org.infinispan.api.sync.SyncCache;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public abstract class InfinispanAPIExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {
   private static final Pattern SANITIZE = Pattern.compile("[^a-zA-Z0-9_-]");
   private final List<Infinispan> instances = new ArrayList<>();
   private final int numNodes;
   private final boolean clustered;
   private final @Nullable CacheConfiguration cacheConfiguration;
   private @Nullable String cacheName;
   private boolean cacheCreated;
   private @Nullable SyncCache<?, ?> syncCache;
   private @Nullable AsyncCache<?, ?> asyncCache;

   protected InfinispanAPIExtension() {
      this(1, false, null);
   }

   protected InfinispanAPIExtension(int numNodes, boolean clustered, @Nullable CacheConfiguration cacheConfiguration) {
      this.numNodes = numNodes;
      this.clustered = clustered;
      this.cacheConfiguration = cacheConfiguration;
   }

   protected abstract Infinispan createInfinispan(String name, int index);

   protected int numNodes() {
      return numNodes;
   }

   protected boolean clustered() {
      return clustered;
   }

   protected @Nullable CacheConfiguration cacheConfiguration() {
      return cacheConfiguration;
   }

   @Override
   public void beforeAll(ExtensionContext context) throws Exception {
      String name = context.getRequiredTestClass().getSimpleName();
      for (int i = 0; i < numNodes(); i++) {
         instances.add(createInfinispan(name, i));
      }
   }

   @Override
   public void afterAll(ExtensionContext context) {
      for (Infinispan infinispan : instances) {
         if (infinispan != null) {
            infinispan.close();
         }
      }
      instances.clear();
   }

   protected abstract CacheConfiguration defaultCacheConfiguration();

   @Override
   public void beforeEach(ExtensionContext context) {
      cacheName = sanitizeCacheName(context.getRequiredTestMethod().getName());
   }

   @Override
   public void afterEach(ExtensionContext context) {
      syncCache = null;
      asyncCache = null;
      if (cacheName != null) {
         if (cacheCreated) {
            infinispan().sync().caches().remove(cacheName);
            cacheCreated = false;
         }
         cacheName = null;
      }
   }

   public Infinispan infinispan() {
      return infinispan(0);
   }

   public Infinispan infinispan(int index) {
      return instances.get(index);
   }

   private void ensureCache() {
      if (!cacheCreated) {
         CacheConfiguration config = cacheConfiguration != null ? cacheConfiguration : defaultCacheConfiguration();
         syncCache = infinispan().sync().caches().create(cacheName, config, AdminFlag.VOLATILE);
         asyncCache = infinispan().async().caches().get(cacheName).toCompletableFuture().join();
         cacheCreated = true;
      }
   }

   @SuppressWarnings("unchecked")
   public <K, V> SyncCache<K, V> syncCache() {
      ensureCache();
      return (SyncCache<K, V>) syncCache;
   }

   @SuppressWarnings("unchecked")
   public <K, V> AsyncCache<K, V> asyncCache() {
      ensureCache();
      return (AsyncCache<K, V>) asyncCache;
   }

   private static String sanitizeCacheName(String methodName) {
      return SANITIZE.matcher(methodName).replaceAll("-");
   }
}
