package org.infinispan.configuration.module;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.infinispan.Cache;
import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.test.EmbeddedTestDriver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ExtendedParserTest {

   @RegisterExtension
   static EmbeddedTestDriver EMBEDDED = EmbeddedTestDriver.local().build();

   @Test
   public void testExtendedParserBareExtension() throws IOException {
      ConfigurationBuilderHolder holder = new ParserRegistry().parse("""
               <local-cache name="extra-module">
                   <sample-element xmlns="urn:infinispan:config:mymodule" sample-attribute="test-value" />
               </local-cache>
            """);
      Cache<Object, Object> cache = EMBEDDED.cache(holder.getCurrentConfigurationBuilder());
      assertEquals("test-value", cache.getCacheConfiguration().module(MyModuleConfiguration.class).attribute());
   }


   @Test
   public void testExtendedParserWrongScope() {
      assertThatThrownBy(() -> new ParserRegistry().parse("""
            <infinispan>
               <cache-container name="container-extra-modules" default-cache="extra-module">
                  <local-cache name="extra-module">
                  </local-cache>
                  <sample-element xmlns="urn:infinispan:config:mymodule" sample-attribute="test-value" />
               </cache-container>
            </infinispan>""")).isInstanceOf(CacheConfigurationException.class).hasMessage("WRONG SCOPE");
   }
}
