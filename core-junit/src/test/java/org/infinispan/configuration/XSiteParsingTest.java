package org.infinispan.configuration;

import static org.infinispan.test.TestingUtil.extractGlobalComponent;
import static org.infinispan.test.TestingUtil.wrapXMLWithSchema;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.remoting.transport.Transport;
import org.junit.jupiter.api.Test;

/**
 * Parsing tests for Cross-Site
 *
 * @since 14.0
 */
public class XSiteParsingTest {

   // https://issues.redhat.com/browse/ISPN-13623 reproducer
   @Test
   public void testMultipleStackParents() throws IOException {
      String config = wrapXMLWithSchema("""
               <jgroups>
                  <stack name="parent" extends="udp">
                     <UDP mcast_port="54444"/>
                  </stack>
                  <stack name="bridge" extends="tcp">
                     <MPING mcast_port="55555" />
                  </stack>
                  <stack name="xsite" extends="parent">
                     <relay.RELAY2 site="a" />
                     <remote-sites default-stack="bridge">
                        <remote-site name="a" />
                        <remote-site name="b" />
                     </remote-sites>
                  </stack>
               </jgroups>
               <cache-container>
                 <transport cluster="multiple-parent-stack" stack="xsite"/>
               </cache-container>
            """);

      try (DefaultCacheManager cm = new DefaultCacheManager(new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8)))) {
         // just to make sure the DefaultCacheManager starts.
         assertTrue(extractGlobalComponent(cm, Transport.class).isSiteCoordinator());
      }
   }

   @Test
   public void testInvalidMaxTombstoneCleanupDelay() {
      String config1 = wrapXMLWithSchema("""
            <cache-container>
               <transport/>
               <distributed-cache name="A">
                  <backups max-cleanup-delay="-1"/>
               </distributed-cache>
            </cache-container>""");
      assertCacheConfigurationException(config1, "ISPN000951: Invalid value -1 for attribute max-cleanup-delay: must be a number greater than zero");
      String config2 = wrapXMLWithSchema("""
            <cache-container>
               <transport/>
               <distributed-cache name="B">
                  <backups max-cleanup-delay="0"/>
               </distributed-cache>
            </cache-container>""");
      assertCacheConfigurationException(config2, "ISPN000951: Invalid value 0 for attribute max-cleanup-delay: must be a number greater than zero");
   }

   private void assertCacheConfigurationException(String config, String messageRegex) {
      ParserRegistry parserRegistry = new ParserRegistry();
      Assertions.assertThatThrownBy(() -> parserRegistry.parse(config)).isInstanceOf(CacheConfigurationException.class).hasMessage(messageRegex);
   }

}
