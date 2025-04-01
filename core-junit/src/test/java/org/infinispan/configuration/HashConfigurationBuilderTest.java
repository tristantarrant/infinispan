package org.infinispan.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.distribution.ch.impl.SyncConsistentHashFactory;
import org.junit.jupiter.api.Test;

public class HashConfigurationBuilderTest {

   @Test
   public void testNumOwners() {
      ConfigurationBuilder cb = new ConfigurationBuilder();
      cb.clustering().cacheMode(CacheMode.DIST_SYNC);
      cb.clustering().hash().numOwners(5);
      Configuration c = cb.build();
      assertEquals(5, c.clustering().hash().numOwners());

      try {
         cb.clustering().hash().numOwners(0);
         fail("IllegalArgumentException expected");
      } catch (IllegalArgumentException e) {
      }
   }

   @Test
   public void testNumSegments() {
      ConfigurationBuilder cb = new ConfigurationBuilder();
      cb.clustering().cacheMode(CacheMode.DIST_SYNC);
      cb.clustering().hash().numSegments(5);

      Configuration c = cb.build();
      assertEquals(5, c.clustering().hash().numSegments());

      try {
         cb.clustering().hash().numSegments(0);
         fail("IllegalArgumentException expected");
      } catch (IllegalArgumentException e) {
      }
   }

   @Test
   public void testConsistentHashFactory() {
      ConfigurationBuilder cb = new ConfigurationBuilder();
      cb.clustering().cacheMode(CacheMode.DIST_SYNC);
      Configuration c = cb.build();
      assertNull(c.clustering().hash().consistentHashFactory());
      SyncConsistentHashFactory consistentHashFactory = new SyncConsistentHashFactory();
      cb.clustering().hash().consistentHashFactory(consistentHashFactory);
      c = cb.build();
      assertSame(c.clustering().hash().consistentHashFactory(), consistentHashFactory);
   }
}
