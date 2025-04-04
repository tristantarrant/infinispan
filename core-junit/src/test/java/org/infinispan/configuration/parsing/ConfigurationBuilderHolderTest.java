package org.infinispan.configuration.parsing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.infinispan.configuration.cache.StorageType.HEAP;
import static org.infinispan.eviction.EvictionStrategy.REMOVE;

import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class ConfigurationBuilderHolderTest {
   @Test
   public void testConfigNotValid() {
      ConfigurationBuilderHolder holder = new ConfigurationBuilderHolder();

      ConfigurationBuilder concreteConfigBuilder = holder.newConfigurationBuilder("concrete");
      // This isn't valid as we need a binary encoding
      concreteConfigBuilder.memory().maxSize("1.5 GB").storage(HEAP).whenFull(REMOVE);

      assertThatThrownBy(holder::resolveConfigurations).isInstanceOf(CacheConfigurationException.class).hasMessageMatching(".*needs either off-heap or a binary compatible.*");
   }

   @Test
   public void testTemplateHasRequiredConfigForValidation() {
      ConfigurationBuilderHolder holder = new ConfigurationBuilderHolder();
      ConfigurationBuilder templateConfigBuilder = holder.newConfigurationBuilder("template");

      // The concreteConfig below is not valid without the media type being set on the template
      templateConfigBuilder.encoding().mediaType(MediaType.APPLICATION_PROTOSTREAM);
      templateConfigBuilder.template(true);

      ConfigurationBuilder concreteConfigBuilder = holder.newConfigurationBuilder("concrete");
      concreteConfigBuilder.configuration("template");
      concreteConfigBuilder.memory().maxSize("1.5 GB").storage(HEAP).whenFull(REMOVE);

      holder.resolveConfigurations();
   }
}
