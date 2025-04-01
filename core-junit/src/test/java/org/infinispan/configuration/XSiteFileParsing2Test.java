package org.infinispan.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.infinispan.configuration.cache.BackupConfiguration;
import org.infinispan.configuration.cache.BackupConfiguration.BackupStrategy;
import org.infinispan.configuration.cache.BackupConfigurationBuilder;
import org.infinispan.configuration.cache.BackupFailurePolicy;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.test.EmbeddedTestDriver;
import org.infinispan.test.TransportFlags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class XSiteFileParsing2Test {
   @RegisterExtension
   static EmbeddedTestDriver EMBEDDED = EmbeddedTestDriver.fromFile("configs/xsite/xsite-test2.xml")
         .transportFlags(TransportFlags.minimalXsiteFlags()).build();

   @Test
   public void testDefaultCache() {
      Configuration dcc = EMBEDDED.cacheManager().getDefaultCacheConfiguration();
      assertEquals(3, dcc.sites().allBackups().size());
      testDefault(dcc);
   }

   @Test
   public void testInheritor() {
      Configuration dcc = EMBEDDED.cacheManager().getCacheConfiguration("inheritor");
      testDefault(dcc);
   }

   private void testDefault(Configuration dcc) {
      BackupConfiguration nyc = new BackupConfigurationBuilder(null).site("NYC").strategy(BackupStrategy.SYNC)
            .backupFailurePolicy(BackupFailurePolicy.IGNORE).failurePolicyClass(null).replicationTimeout(12003)
            .useTwoPhaseCommit(false).create();
      BackupConfiguration sfo = new BackupConfigurationBuilder(null).site("SFO").strategy(BackupStrategy.ASYNC)
            .backupFailurePolicy(BackupFailurePolicy.WARN).failurePolicyClass(null).replicationTimeout(15000)
            .useTwoPhaseCommit(false).create();
      BackupConfiguration lon = new BackupConfigurationBuilder(null).site("LON").strategy(BackupStrategy.SYNC)
            .backupFailurePolicy(BackupFailurePolicy.WARN).failurePolicyClass(null).replicationTimeout(15000)
            .useTwoPhaseCommit(false).create();
      assertTrue(dcc.sites().allBackups().contains(nyc));
      assertTrue(dcc.sites().allBackups().contains(sfo));
      assertTrue(dcc.sites().allBackups().contains(lon));
      assertEquals("someCache", dcc.sites().backupFor().remoteCache());
      assertEquals("SFO", dcc.sites().backupFor().remoteSite());
   }
}
