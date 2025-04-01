package org.infinispan.configuration.parsing;

import static org.infinispan.commons.test.CommonsTestingUtil.tmpDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.invoke.MethodHandles;

import org.infinispan.Cache;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.test.EmbeddedTestDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Tag("functional")
public class SFSToSIFSMigratorTest {

   @AfterAll
   static void clearTempDirectory() {
      Util.recursiveFileRemove(tmpDirectory(MethodHandles.lookup().lookupClass().getSimpleName()));
   }

   private enum StoreType {
      SINGLE_NON_SEGMENTED {
         @Override
         void apply(ConfigurationBuilder configurationBuilder) {
            configurationBuilder.persistence().addSingleFileStore().segmented(false);
         }
      },
      SINGLE_SEGMENTED {
         @Override
         void apply(ConfigurationBuilder configurationBuilder) {
            configurationBuilder.persistence().addSingleFileStore().segmented(true);
         }
      },
      MIGRATING {
         @Override
         void apply(ConfigurationBuilder configurationBuilder) {
            configurationBuilder.persistence().addStore(SFSToSIFSConfigurationBuilder.class);
         }
      },
      SOFT_INDEX {
         @Override
         void apply(ConfigurationBuilder configurationBuilder) {
            configurationBuilder.persistence().addSoftIndexFileStore();
         }
      };

      abstract void apply(ConfigurationBuilder configurationBuilder);
   }

   /*
    * Test that makes sure data can be migrated from two different stores (in this case SingleFileStore and SoftIndex).
    * Note that SIFS only supports being segmented so we don't need a configuration to change that.
    */
   @ParameterizedTest
   @EnumSource(value = StoreType.class, from = "SINGLE_NON_SEGMENTED", to="SINGLE_SEGMENTED")
   public void testStoreMigration(StoreType storeType) {
      String stateDirectory = tmpDirectory(this.getClass().getSimpleName());
      GlobalConfigurationBuilder global = new GlobalConfigurationBuilder();
      global.globalState().enable().persistentLocation(stateDirectory);
      ConfigurationBuilderHolder holder = new ConfigurationBuilderHolder(global);
      EmbeddedTestDriver.local().call(driver -> {
         ConfigurationBuilder builder = new ConfigurationBuilder();
         storeType.apply(builder);
         Cache<String, String> cache = driver.cache(builder);
         cache.put("key", "value");
      });
      EmbeddedTestDriver.local().call(driver -> {
         ConfigurationBuilder builder = new ConfigurationBuilder();
         StoreType.MIGRATING.apply(builder);
         Cache<String, String> cache = driver.cache(builder);
         assertEquals("value", cache.get("key"));
      });
      EmbeddedTestDriver.local().call(driver -> {
         ConfigurationBuilder builder = new ConfigurationBuilder();
         StoreType.SOFT_INDEX.apply(builder);
         Cache<String, String> cache = driver.cache(builder);
         assertEquals("value", cache.get("key"));
      });
   }
}
