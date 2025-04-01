package org.infinispan.configuration;

import static org.infinispan.test.TestingUtil.extractGlobalComponent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.infinispan.configuration.cache.BackupConfiguration.BackupStrategy;
import org.infinispan.configuration.cache.BackupConfigurationBuilder;
import org.infinispan.configuration.cache.BackupFailurePolicy;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.infinispan.test.EmbeddedTestDriver;
import org.infinispan.test.TransportFlags;
import org.jgroups.protocols.relay.RELAY2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class XSiteInlineConfigFileParsingTest {

   @RegisterExtension
   static EmbeddedTestDriver EMBEDDED = EmbeddedTestDriver
         .fromResource("configs/xsite/xsite-inline-test.xml")
         .transportFlags(new TransportFlags().withPreserveConfig(true))
         .build();

   @Test
   public void testInlineConfiguration() {
      JGroupsTransport transport = (JGroupsTransport) extractGlobalComponent(EMBEDDED.cacheManager(), Transport.class);
      RELAY2 relay2 = transport.getChannel().getProtocolStack().findProtocol(RELAY2.class);
      assertEquals(3, relay2.getSites().size());
      assertTrue(relay2.getSites().contains("LON"));
      assertTrue(relay2.getSites().contains("SFO"));
      assertTrue(relay2.getSites().contains("NYC"));

      Configuration dcc = EMBEDDED.cacheManager().getDefaultCacheConfiguration();
      assertEquals(2, dcc.sites().allBackups().size());
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
