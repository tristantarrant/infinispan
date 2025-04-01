package org.infinispan.configuration.parsing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileNotFoundException;

import org.infinispan.test.EmbeddedTestDriver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class LegacyXmlFileParsingTest {
   @Test
   public void testUnsupportedConfiguration() {
      assertThatThrownBy(() ->
            EmbeddedTestDriver.fromFile("configs/legacy/6.0.xml").build())
            .hasCauseInstanceOf(FileNotFoundException.class);
   }
}
