package org.infinispan.persistence.remote.configuration;

import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.configuration.serializer.AbstractConfigurationSerializerTest;
import org.junit.jupiter.api.Assertions;
import org.testng.annotations.Test;

@Test(testName = "persistence.remote.configuration.ConfigurationSerializerTest", groups="functional")
public class ConfigurationSerializerTest extends AbstractConfigurationSerializerTest {
   @Override
   protected void compareStoreConfiguration(String name, StoreConfiguration beforeStore, StoreConfiguration afterStore) {
      super.compareStoreConfiguration(name, beforeStore, afterStore);
      RemoteStoreConfiguration before = (RemoteStoreConfiguration) beforeStore;
      RemoteStoreConfiguration after = (RemoteStoreConfiguration) afterStore;
      assertEquals("Wrong connection pool for " + name + " configuration.", before.connectionPool(), after.connectionPool());
      assertEquals("Wrong security config for " + name + " configuration.", before.security(), after.security());
      assertEquals("Wrong remote server config for " + name + " configuration.", before.servers(), after.servers());
   }
}
