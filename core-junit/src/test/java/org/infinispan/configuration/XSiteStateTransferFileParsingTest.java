package org.infinispan.configuration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.infinispan.configuration.cache.XSiteStateTransferConfiguration.DEFAULT_TIMEOUT;
import static org.infinispan.configuration.cache.XSiteStateTransferConfiguration.DEFAULT_WAIT_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.infinispan.commons.CacheConfigurationException;
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
 * It tests if the cross site replication configuration is correctly parsed and validated.
 *
 * @author Pedro Ruivo
 * @since 7.0
 */
public class XSiteStateTransferFileParsingTest {
   private static final String FILE_NAME = "configs/xsite/xsite-state-transfer-test.xml";
   private static final String XML_FORMAT = """
            <replicated-cache name="default">
               <backups>
                  <backup site="NYC" strategy="SYNC" failure-policy="WARN" timeout="12003">
                     <state-transfer chunk-size="10" timeout="%s" max-retries="30" wait-time="%s" mode="%s"/>
                  </backup>
               </backups>
               <backup-for remote-cache="someCache" remote-site="SFO"/>
            </replicated-cache>
         """;

   @RegisterExtension
   static EmbeddedTestDriver EMBEDDED = EmbeddedTestDriver
         .fromResource(FILE_NAME)
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
      assertEquals(1, dcc.sites().allBackups().size());
      testDefault(dcc);
   }

   @Test
   public void testStateTransferDifferentConfig() {
      Configuration dcc = EMBEDDED.cacheManager().getCacheConfiguration("stateTransferDifferentConfiguration");
      assertEquals(1, dcc.sites().allBackups().size());
      assertTrue(dcc.sites().allBackups().contains(create(98, 7654, 321, 101)));
      assertEquals("someCache", dcc.sites().backupFor().remoteCache());
      assertEquals("SFO", dcc.sites().backupFor().remoteSite());
   }

   @Test
   public void testNegativeTimeout() {
      assertThatThrownBy(() ->
            testInvalidConfiguration(String.format(XML_FORMAT, -1, DEFAULT_WAIT_TIME, "MANUAL"))
      ).isInstanceOf(CacheConfigurationException.class).hasMessageMatching("ISPN000449:.*");
   }

   @Test
   public void testZeroTimeout() {
      assertThatThrownBy(() ->
            testInvalidConfiguration(String.format(XML_FORMAT, 0, DEFAULT_WAIT_TIME, "MANUAL"))
      ).isInstanceOf(CacheConfigurationException.class).hasMessageMatching("ISPN000449:.*");

   }

   @Test
   public void testNegativeWaitTime() {
      assertThatThrownBy(() ->
            testInvalidConfiguration(String.format(XML_FORMAT, DEFAULT_TIMEOUT, -1, "MANUAL"))
      ).isInstanceOf(CacheConfigurationException.class).hasMessageMatching("ISPN000450:.*");

   }

   @Test
   public void testZeroWaitTime() {
      assertThatThrownBy(() ->
            testInvalidConfiguration(String.format(XML_FORMAT, DEFAULT_TIMEOUT, 0, "MANUAL"))
      ).isInstanceOf(CacheConfigurationException.class).hasMessageMatching("ISPN000450:.*");
   }

   @Test
   public void testAutoStateTransferModeWithSync() {
      assertThatThrownBy(() ->
            testInvalidConfiguration(String.format(XML_FORMAT, DEFAULT_TIMEOUT, DEFAULT_WAIT_TIME, "AUTO"))
      ).isInstanceOf(CacheConfigurationException.class).hasMessageMatching("ISPN000634:.*");

   }

   private void testInvalidConfiguration(String xmlConfiguration) {
      EMBEDDED.log().debugf("Creating cache manager with %s", xmlConfiguration);
      EMBEDDED.cache(xmlConfiguration);
   }

   private void testDefault(Configuration dcc) {
      assertTrue(dcc.sites().allBackups().contains(create(123, 4567, 890, 1011)));
      assertEquals("someCache", dcc.sites().backupFor().remoteCache());
      assertEquals("SFO", dcc.sites().backupFor().remoteSite());
   }

   private static BackupConfiguration create(int chunkSize, long timeout, int maxRetries, long waitingTimeBetweenRetries) {
      BackupConfigurationBuilder builder = new BackupConfigurationBuilder(null).site("NYC")
            .strategy(BackupStrategy.SYNC).backupFailurePolicy(BackupFailurePolicy.WARN).failurePolicyClass(null)
            .replicationTimeout(12003).useTwoPhaseCommit(false);
      builder.stateTransfer().chunkSize(chunkSize).timeout(timeout).maxRetries(maxRetries)
            .waitTime(waitingTimeBetweenRetries);
      return builder.create();
   }

   private static BackupConfiguration createDefault() {
      BackupConfigurationBuilder builder = new BackupConfigurationBuilder(null).site("NYC")
            .strategy(BackupStrategy.SYNC).backupFailurePolicy(BackupFailurePolicy.WARN).failurePolicyClass(null)
            .replicationTimeout(12003).useTwoPhaseCommit(false);
      return builder.create();
   }

}
