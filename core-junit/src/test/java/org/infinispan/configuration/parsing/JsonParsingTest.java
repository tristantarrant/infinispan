package org.infinispan.configuration.parsing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class JsonParsingTest {
   @Test
   public void testSerializationAllowList() throws IOException {
      ParserRegistry parserRegistry = new ParserRegistry(Thread.currentThread().getContextClassLoader(), true, System.getProperties());
      ConfigurationBuilderHolder holder = parserRegistry.parseFile("configs/serialization-test.json");
      GlobalConfiguration globalConfiguration = holder.getGlobalConfigurationBuilder().build();
      Set<String> classes = globalConfiguration.serialization().allowList().getClasses();
      assertEquals(3, classes.size());
      List<String> regexps = globalConfiguration.serialization().allowList().getRegexps();
      assertEquals(2, regexps.size());
   }

   @Test
   public void testErrorReporting() {
      ParserRegistry parserRegistry = new ParserRegistry(Thread.currentThread().getContextClassLoader(), true, System.getProperties());
      assertThatThrownBy(() -> parserRegistry.parseFile("configs/broken.json")).isInstanceOf(CacheConfigurationException.class).hasMessageMatching("^ISPN000327:.*broken.json\\[23,15].*");
   }

   @Test
   public void testInvalidTracingCollector() throws Exception {
      ParserRegistry parserRegistry = new ParserRegistry(Thread.currentThread().getContextClassLoader(), true, System.getProperties());
      ConfigurationBuilderHolder holder = parserRegistry.parseFile("configs/tracing-endpoint-wrong.json");
      assertThatThrownBy(() -> holder.getGlobalConfigurationBuilder().build()).isInstanceOf(CacheConfigurationException.class).hasMessageMatching("^ISPN000699:.*Tracing collector endpoint 'sdjsd92k2..21232' is not valid.*");
   }

   @Test
   public void testAliasTest() throws IOException {
      ParserRegistry parserRegistry = new ParserRegistry(Thread.currentThread().getContextClassLoader(), true, System.getProperties());
      ConfigurationBuilderHolder holder = parserRegistry.parseFile("configs/aliases-test.json");
      Configuration cache = holder.getNamedConfigurationBuilders().get("anotherRespCache").build();
      assertEquals(Set.of("1"), cache.aliases());
   }

   @Test
   public void testNaming() {
      ParserRegistry parserRegistry = new ParserRegistry(Thread.currentThread().getContextClassLoader(), true, System.getProperties());
      String configJson = """
            {
                "actionTokens": {
                    "distributed-cache": {
                        "owners": "2",
                        "mode": "SYNC",
                        "encoding": {
                            "key": {
                                "media-type": "application/x-java-object"
                            },
                            "value": {
                                "media-type": "application/x-java-object"
                            }
                        },
                        "expiration": {
                            "lifespan": "-1",
                            "max-idle": "-1",
                            "interval": "300000"
                        },
                        "memory": {
                            "max-count": "100"
                        }
                    }
                }
            }""";

      ConfigurationBuilderHolder configHolder = parserRegistry.parse(configJson, MediaType.APPLICATION_JSON);
      assertThat(configHolder.getNamedConfigurationBuilders()).containsKey("actionTokens");
   }
}
