package org.infinispan.server.resp.dist;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.server.resp.TDigestTest;
import org.infinispan.server.resp.test.TestSetup;
import org.testng.annotations.Test;

@Test(groups = "functional", testName = "dist.server.resp.TDigestClusteredTest")
public class TDigestClusteredTest extends TDigestTest {

   private CacheMode mode;

   private TDigestClusteredTest withCacheMode(CacheMode mode) {
      this.mode = mode;
      return this;
   }

   @Override
   public Object[] factory() {
      return new Object[] {
            new TDigestClusteredTest().withCacheMode(CacheMode.DIST_SYNC),
            new TDigestClusteredTest().withCacheMode(CacheMode.REPL_SYNC),
      };
   }

   @Override
   protected String parameters() {
      return "[mode=" + mode + "]";
   }

   @Override
   protected void amendConfiguration(ConfigurationBuilder configurationBuilder) {
      configurationBuilder.clustering().cacheMode(mode);
   }

   @Override
   protected TestSetup setup() {
      return TestSetup.clusteredTestSetup(3);
   }
}
