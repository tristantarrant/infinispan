package org.infinispan.configuration.parsing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class YamlParsingTest {
   @Test
   public void testSerializationAllowList() throws IOException {
      ParserRegistry parserRegistry = new ParserRegistry(Thread.currentThread().getContextClassLoader(), true, System.getProperties());
      ConfigurationBuilderHolder holder = parserRegistry.parseFile("configs/serialization-test.yaml");
      GlobalConfiguration globalConfiguration = holder.getGlobalConfigurationBuilder().build();
      Set<String> classes = globalConfiguration.serialization().allowList().getClasses();
      assertEquals(3, classes.size());
      List<String> regexps = globalConfiguration.serialization().allowList().getRegexps();
      assertEquals(2, regexps.size());
   }

   @Test
   public void testErrorReporting() {
      ParserRegistry parserRegistry = new ParserRegistry(Thread.currentThread().getContextClassLoader(), true, System.getProperties());
      assertThatThrownBy(() -> parserRegistry.parseFile("configs/broken.yaml")).isInstanceOf(CacheConfigurationException.class).hasMessageMatching("^ISPN000327:.*broken.yaml\\[18,18].*");
   }

   @Test
   public void testInvalidTracingCollector() throws Exception {
      ParserRegistry parserRegistry = new ParserRegistry(Thread.currentThread().getContextClassLoader(), true, System.getProperties());
      ConfigurationBuilderHolder holder = parserRegistry.parseFile("configs/tracing-endpoint-wrong.yaml");
      assertThatThrownBy(() -> holder.getGlobalConfigurationBuilder().build()).isInstanceOf(CacheConfigurationException.class).hasMessageMatching("^ISPN000699:.*Tracing collector endpoint 'sdjsd92k2..21232' is not valid.*");
   }
}
