package org.infinispan.configuration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.infinispan.configuration.cache.CacheMode.DIST_SYNC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.infinispan.Cache;
import org.infinispan.commons.configuration.Combine;
import org.infinispan.configuration.cache.ClusteringConfiguration;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.dummy.DummyInMemoryStoreConfigurationBuilder;
import org.infinispan.test.EmbeddedTestDriver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ConfigurationOverrideTest {

   @RegisterExtension
   static EmbeddedTestDriver EMBEDDED = EmbeddedTestDriver.clustered().build();

   @Test
   public void testConfigurationOverride() {
      EmbeddedCacheManager cm = EMBEDDED.cacheManager();
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.memory().size(200).storageType(StorageType.BINARY);
      ConfigurationBuilder cacheCfgBuilder = new ConfigurationBuilder().read(builder.build(), Combine.DEFAULT);
      Cache<Object, Object> cache = EMBEDDED.cache(cacheCfgBuilder);
      assertEquals(200, cache.getCacheConfiguration().memory().size());
      assertEquals(StorageType.BINARY, cache.getCacheConfiguration().memory().storageType());
   }

   @Test
   public void testSimpleDistributedClusterModeDefault() {
      EmbeddedCacheManager cm = EMBEDDED.cacheManager();
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.clustering().cacheMode(DIST_SYNC)
            .hash().numOwners(3).numSegments(51);
      Cache<?, ?> cache = EMBEDDED.cache(builder);
      // These are all overridden values
      ClusteringConfiguration clusteringCfg =
            cache.getCacheConfiguration().clustering();
      assertEquals(DIST_SYNC, clusteringCfg.cacheMode());
      assertEquals(3, clusteringCfg.hash().numOwners());
      assertEquals(51, clusteringCfg.hash().numSegments());
   }

   @Test
   public void testSimpleDistributedClusterModeNamedCache() {
      EmbeddedCacheManager cm = EMBEDDED.cacheManager();
      String cacheName = EMBEDDED.cacheName();
      Configuration config = new ConfigurationBuilder()
            .clustering().cacheMode(DIST_SYNC)
            .hash().numOwners(3).numSegments(51).build();
      cm.defineConfiguration(cacheName, config);
      Cache<?, ?> cache = cm.getCache(cacheName);
      ClusteringConfiguration clusteringCfg =
            cache.getCacheConfiguration().clustering();
      assertEquals(DIST_SYNC, clusteringCfg.cacheMode());
      assertEquals(3, clusteringCfg.hash().numOwners());
      assertEquals(51, clusteringCfg.hash().numSegments());
   }

   @Test
   public void testOverrideWithStore() {
      EmbeddedCacheManager cm = EMBEDDED.cacheManager();
      ConfigurationBuilder builder1 = new ConfigurationBuilder();
      builder1.persistence().addStore(DummyInMemoryStoreConfigurationBuilder.class);
      ConfigurationBuilder builder2 = new ConfigurationBuilder();
      builder2.read(cm.getDefaultCacheConfiguration(), Combine.DEFAULT);
      builder2.memory().size(1000);
      Configuration configuration = builder2.build();
      assertEquals(1, configuration.persistence().stores().size());
   }

   @Test
   public void testPartialOverride() {
      ConfigurationBuilder baseBuilder = new ConfigurationBuilder();
      baseBuilder.memory().size(200).storageType(StorageType.BINARY);
      Configuration base = baseBuilder.build();
      ConfigurationBuilder overrideBuilder = new ConfigurationBuilder();
      overrideBuilder.read(base, Combine.DEFAULT).locking().concurrencyLevel(31);
      Configuration override = overrideBuilder.build();
      assertEquals(200, base.memory().size());
      assertEquals(200, override.memory().size());
      assertEquals(StorageType.BINARY, base.memory().storageType());
      assertEquals(StorageType.BINARY, override.memory().storageType());
      assertEquals(32, base.locking().concurrencyLevel());
      assertEquals(31, override.locking().concurrencyLevel());
   }

   @Test
   public void testConfigurationUndefine() {
      EmbeddedCacheManager cm = EMBEDDED.cacheManager();
      String name = EMBEDDED.cacheName();
      cm.defineConfiguration(name, new ConfigurationBuilder().build());
      cm.undefineConfiguration(name);
      assertNull(cm.getCacheConfiguration(name));
   }

   @Test
   public void testConfigurationUndefineWhileInUse() {
      EmbeddedCacheManager cm = EMBEDDED.cacheManager();
      String name = EMBEDDED.cacheName();
      cm.defineConfiguration(name, new ConfigurationBuilder().build());
      cm.getCache(name);
      assertThatThrownBy(() -> cm.undefineConfiguration(name)).isInstanceOf(IllegalStateException.class);
   }
}
