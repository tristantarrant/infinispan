package org.infinispan.configuration.module;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.infinispan.commons.configuration.Combine;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class ModuleConfigurationTest {

   @Test
   public void testModuleConfiguration() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.addModule(MyModuleConfigurationBuilder.class).attribute("testValue");

      Configuration configuration = builder.build();
      assertEquals("testValue", configuration.module(MyModuleConfiguration.class).attribute());

      ConfigurationBuilder secondBuilder = new ConfigurationBuilder();
      secondBuilder.read(configuration, Combine.DEFAULT);
      Configuration secondConfiguration = secondBuilder.build();
      assertEquals("testValue", secondConfiguration.module(MyModuleConfiguration.class).attribute());
   }
}
