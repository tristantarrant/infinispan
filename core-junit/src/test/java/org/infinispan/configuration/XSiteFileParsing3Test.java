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

/**
 * @author Mircea Markus
 * @since 5.2
 */
public class XSiteFileParsing3Test {

   @RegisterExtension
   static EmbeddedTestDriver EMBEDDED = EmbeddedTestDriver
         .fromResource( "configs/xsite/xsite-offline-test.xml")
         .transportFlags(TransportFlags.minimalXsiteFlags())
         .build();

   @Test
   public void testDefaultCache() {
      Configuration dcc = EMBEDDED.cacheManager().getDefaultCacheConfiguration();
      assertEquals(1, dcc.sites().allBackups().size());
      testDefault(dcc);
   }

   @Test
   public void testInheritor() {
      Configuration dcc = EMBEDDED.cacheManager().getCacheConfiguration("inheritor");
      testDefault(dcc);
   }

   @Test
   public void testNoTakeOffline() {
      Configuration dcc = EMBEDDED.cacheManager().getCacheConfiguration("noTakeOffline");
      assertEquals(1, dcc.sites().allBackups().size());
      BackupConfiguration nyc = new BackupConfigurationBuilder(null).site("NYC").strategy(BackupStrategy.SYNC)
            .backupFailurePolicy(BackupFailurePolicy.WARN).failurePolicyClass(null).replicationTimeout(12003)
            .useTwoPhaseCommit(false).create();
      assertTrue(dcc.sites().allBackups().contains(nyc));
      assertEquals("SFO", dcc.sites().backupFor().remoteSite());
      assertEquals("someCache", dcc.sites().backupFor().remoteCache());
   }

   @Test
   public void testTakeOfflineDifferentConfig() {
      Configuration dcc = EMBEDDED.cacheManager().getCacheConfiguration("takeOfflineDifferentConfig");
      assertEquals(1, dcc.sites().allBackups().size());
      BackupConfigurationBuilder nyc = new BackupConfigurationBuilder(null).site("NYC").strategy(BackupStrategy.SYNC)
            .backupFailurePolicy(BackupFailurePolicy.IGNORE).failurePolicyClass(null).replicationTimeout(12003)
            .useTwoPhaseCommit(false);
      nyc.takeOffline().afterFailures(321).minTimeToWait(3765);
      assertTrue(dcc.sites().allBackups().contains(nyc.create()));

   }

   private void testDefault(Configuration dcc) {
      BackupConfigurationBuilder nyc = new BackupConfigurationBuilder(null).site("NYC").strategy(BackupStrategy.SYNC)
            .backupFailurePolicy(BackupFailurePolicy.IGNORE).failurePolicyClass(null).replicationTimeout(12003)
            .useTwoPhaseCommit(false);
      nyc.takeOffline().afterFailures(123).minTimeToWait(5673);
      assertTrue(dcc.sites().allBackups().contains(nyc.create()));
      assertEquals("someCache", dcc.sites().backupFor().remoteCache());
      assertEquals("SFO", dcc.sites().backupFor().remoteSite());
   }
}
