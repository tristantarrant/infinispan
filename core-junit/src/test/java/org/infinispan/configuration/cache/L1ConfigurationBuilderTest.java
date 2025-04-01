package org.infinispan.configuration.cache;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.eviction.EvictionStrategy;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class L1ConfigurationBuilderTest {
   @Test
   public void testDefaultsWhenEnabledOnly() {
      Configuration config = new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).l1().enable().build();

      L1Configuration l1Config = config.clustering().l1();

      assertTrue(l1Config.enabled());
      assertEquals(TimeUnit.MINUTES.toMillis(1), l1Config.cleanupTaskFrequency());
      assertEquals(0, l1Config.invalidationThreshold());
      assertEquals(TimeUnit.MINUTES.toMillis(10), l1Config.lifespan());
   }

   @Test
   public void testL1WithExceptionEviction() {
      ConfigurationBuilder config = new ConfigurationBuilder();
      config
            .clustering()
               .cacheMode(CacheMode.DIST_SYNC)
               .l1().enable()
            .memory()
               .evictionStrategy(EvictionStrategy.EXCEPTION)
               .size(10)
            .transaction()
               .transactionMode(org.infinispan.transaction.TransactionMode.TRANSACTIONAL);
      assertThatThrownBy(config::build).isInstanceOf(CacheConfigurationException.class).hasMessageMatching("ISPN000352:.*");

   }
}
