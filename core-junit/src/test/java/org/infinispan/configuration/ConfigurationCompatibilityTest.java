package org.infinispan.configuration;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.junit.jupiter.api.Test;

public class ConfigurationCompatibilityTest {

   @Test
   public void testDocumentationPersistenceConfiguration() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.persistence()
         .passivation(false)
         .addSingleFileStore()
            .fetchPersistentState(true)
            .shared(false)
            .preload(true)
            .ignoreModifications(false)
            .purgeOnStartup(false)
            .location(System.getProperty("java.io.tmpdir"))
            .async()
               .enabled(true);
   }

}
