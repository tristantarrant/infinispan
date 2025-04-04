package org.infinispan.configuration.parsing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.test.EmbeddedTestDriver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class LegacyXmlFileParsingTest {
   @Test
   public void testUnsupportedConfiguration() {
      assertThatThrownBy(() ->
            EmbeddedTestDriver.fromResource("configs/legacy/6.0.xml").build())
            .isInstanceOf(CacheConfigurationException.class)
            .hasMessageMatching("^ISPN000327:.*");
   }
}
