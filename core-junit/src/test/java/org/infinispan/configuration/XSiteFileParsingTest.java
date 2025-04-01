package org.infinispan.configuration;

import static org.infinispan.test.TestingUtil.extractGlobalComponent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.BackupConfiguration.BackupStrategy;
import org.infinispan.configuration.cache.BackupConfigurationBuilder;
import org.infinispan.configuration.cache.BackupFailurePolicy;
import org.infinispan.configuration.cache.BackupForConfiguration;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.XSiteStateTransferMode;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.infinispan.test.EmbeddedTestDriver;
import org.infinispan.test.TransportFlags;
import org.infinispan.test.xsite.CountingCustomFailurePolicy;
import org.infinispan.test.xsite.CustomXSiteEntryMergePolicy;
import org.infinispan.xsite.spi.AlwaysRemoveXSiteEntryMergePolicy;
import org.infinispan.xsite.spi.DefaultXSiteEntryMergePolicy;
import org.infinispan.xsite.spi.PreferNonNullXSiteEntryMergePolicy;
import org.infinispan.xsite.spi.PreferNullXSiteEntryMergePolicy;
import org.infinispan.xsite.spi.XSiteEntryMergePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author Mircea Markus
 * @since 5.2
 */
public class XSiteFileParsingTest {
   @RegisterExtension
   static EmbeddedTestDriver EMBEDDED = EmbeddedTestDriver
         .fromResource("configs/xsite/xsite-test.xml")
         .transportFlags(TransportFlags.minimalXsiteFlags())
         .build();

   @Test
   public void testLocalSiteName() {
      JGroupsTransport transport = (JGroupsTransport) extractGlobalComponent(EMBEDDED.cacheManager(), Transport.class);
      transport.checkCrossSiteAvailable();
      assertEquals("LON-1", transport.localSiteName());
   }

   @Test
   public void testDefaultCache() {
      Configuration dcc = EMBEDDED.cacheManager().getDefaultCacheConfiguration();
      testDefault(dcc);
   }

   @Test
   public void testBackupNyc() {
      Configuration dcc = EMBEDDED.cacheManager().getCacheConfiguration("backupNyc");
      assertEquals(0, dcc.sites().allBackups().size());
      BackupForConfiguration backupForConfiguration = dcc.sites().backupFor();
      assertEquals("someCache", backupForConfiguration.remoteCache());
      assertEquals("NYC", backupForConfiguration.remoteSite());
   }

   @Test
   public void testInheritor() {
      Configuration dcc = EMBEDDED.cacheManager().getCacheConfiguration("inheritor");
      testDefault(dcc);
   }

   @Test
   public void testNoBackups() {
      Configuration dcc = EMBEDDED.cacheManager().getCacheConfiguration("noBackups");
      assertEquals(0, dcc.sites().allBackups().size());
      assertNull(dcc.sites().backupFor().remoteCache());
      assertNull(dcc.sites().backupFor().remoteSite());
   }

   @Test
   public void testCustomBackupPolicy() {
      Configuration dcc = EMBEDDED.cacheManager().getCacheConfiguration("customBackupPolicy");
      assertEquals(1, dcc.sites().allBackups().size());
      BackupConfigurationBuilder nyc2 = new BackupConfigurationBuilder(null).site("NYC2").strategy(BackupStrategy.SYNC)
            .backupFailurePolicy(BackupFailurePolicy.CUSTOM)
            .failurePolicyClass(CountingCustomFailurePolicy.class.getName()).replicationTimeout(160000)
            .useTwoPhaseCommit(false);

      assertTrue(dcc.sites().allBackups().contains(nyc2.create()));
      assertNull(dcc.sites().backupFor().remoteCache());
   }

   @Test
   public void testXSiteMergePolicy() {
      Configuration dcc = EMBEDDED.cacheManager().getCacheConfiguration("conflictResolver");
      assertEquals(1, dcc.sites().allBackups().size());
      assertEquals(PreferNonNullXSiteEntryMergePolicy.getInstance(), dcc.sites().mergePolicy());
   }

   @Test
   public void testXSiteMergePolicy2() {
      Configuration dcc = EMBEDDED.cacheManager().getCacheConfiguration("conflictResolver2");
      assertEquals(1, dcc.sites().allBackups().size());
      assertEquals(PreferNullXSiteEntryMergePolicy.getInstance(), dcc.sites().mergePolicy());
   }

   @Test
   public void testXSiteMergePolicy3() {
      Configuration dcc = EMBEDDED.cacheManager().getCacheConfiguration("conflictResolver3");
      assertEquals(1, dcc.sites().allBackups().size());
      assertEquals(AlwaysRemoveXSiteEntryMergePolicy.getInstance(), dcc.sites().mergePolicy());
   }

   @Test
   public void testCustomXSiteMergePolicy() {
      Configuration dcc = EMBEDDED.cacheManager().getCacheConfiguration("customConflictResolver");
      assertEquals(1, dcc.sites().allBackups().size());
      assertEquals(CustomXSiteEntryMergePolicy.class, dcc.sites().mergePolicy().getClass());
      Cache<?, ?> cache = EMBEDDED.cacheManager().getCache("customConflictResolver");
      XSiteEntryMergePolicy<?, ?> resolver = ComponentRegistry.componentOf(cache, XSiteEntryMergePolicy.class);
      assertEquals(CustomXSiteEntryMergePolicy.class, resolver.getClass());
   }

   @Test
   public void testAutoStateTransfer() {
      Configuration dcc = EMBEDDED.cacheManager().getCacheConfiguration("autoStateTransfer");
      assertEquals(1, dcc.sites().allBackups().size());
      assertEquals(XSiteStateTransferMode.AUTO, dcc.sites().allBackups().get(0).stateTransfer().mode());
   }

   @Test
   public void testTombstoneConfiguration() {
      Configuration dcc = EMBEDDED.cacheManager().getCacheConfiguration("tombstoneCleanup");
      assertEquals(3000, dcc.sites().maxTombstoneCleanupDelay());
      assertEquals(4000, dcc.sites().tombstoneMapSize());
   }

   private void testDefault(Configuration dcc) {
      assertEquals(2, dcc.sites().allBackups().size());
      assertEquals(DefaultXSiteEntryMergePolicy.getInstance(), dcc.sites().mergePolicy());
      BackupConfigurationBuilder nyc = new BackupConfigurationBuilder(null).site("NYC").strategy(BackupStrategy.SYNC)
            .backupFailurePolicy(BackupFailurePolicy.IGNORE).failurePolicyClass(null).replicationTimeout(12003)
            .useTwoPhaseCommit(false);
      assertTrue(dcc.sites().allBackups().contains(nyc.create()));
      BackupConfigurationBuilder sfo = new BackupConfigurationBuilder(null).site("SFO").strategy(BackupStrategy.ASYNC)
            .backupFailurePolicy(BackupFailurePolicy.WARN).failurePolicyClass(null).replicationTimeout(15000)
            .useTwoPhaseCommit(false);
      assertTrue(dcc.sites().allBackups().contains(sfo.create()));
   }
}
