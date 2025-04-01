package org.infinispan.configuration.clone;

import static org.assertj.core.api.Assertions.assertThat;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class GlobalTracingConfigCloneTest {

   @Test
   public void testTracingConfigClone() {
      GlobalConfigurationBuilder originBuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
      originBuilder.tracing().collectorEndpoint("file://in-memory-local-process");
      assertThat(originBuilder.tracing().enabled()).isTrue();

      GlobalConfiguration original = originBuilder.build();
      assertThat(original.tracing().enabled()).isTrue();

      GlobalConfigurationBuilder clone = GlobalConfigurationBuilder.defaultClusteredBuilder();
      clone.read(original);

      assertThat(clone.tracing().enabled()).isTrue();
   }
}
