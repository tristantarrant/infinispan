package org.infinispan.configuration.cache;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class MemoryConfigurationTest {
   @Test
   public void testMemoryConfigurationMatcher() {
      ConfigurationBuilder b1 = new ConfigurationBuilder();
      b1.memory().maxSize("1GB").storage(StorageType.OFF_HEAP);
      ConfigurationBuilder b2 = new ConfigurationBuilder();
      b2.memory().maxSize("1000000000").storage(StorageType.OFF_HEAP);
      assertTrue(b1.build().matches(b2.build()));
   }
}
