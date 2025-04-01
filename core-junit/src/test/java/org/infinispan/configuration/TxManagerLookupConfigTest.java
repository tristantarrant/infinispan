package org.infinispan.configuration;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.infinispan.commons.tx.lookup.TransactionManagerLookup;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.EmbeddedTestDriver;
import org.infinispan.transaction.tm.EmbeddedBaseTransactionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.transaction.TransactionManager;

public class TxManagerLookupConfigTest {

   @RegisterExtension
   static EmbeddedTestDriver EMBEDDED = EmbeddedTestDriver.clustered().build();

   static TmA tma = new TmA();

   @Test
   public void simpleTest() {
      EmbeddedCacheManager cm = EMBEDDED.cacheManager();
      ConfigurationBuilder customConfiguration = new ConfigurationBuilder();
      customConfiguration.transaction().transactionManagerLookup(new TxManagerLookupA());
      Configuration definedConfiguration = cm.defineConfiguration("aCache", customConfiguration.build());

      // verify the setting was not lost:
      assertInstanceOf(TxManagerLookupA.class, definedConfiguration.transaction().transactionManagerLookup());

      // verify it's actually being used:
      TransactionManager activeTransactionManager = cm.getCache("aCache").getAdvancedCache().getTransactionManager();
      assertNotNull(activeTransactionManager);
      assertInstanceOf(TmA.class, activeTransactionManager);
   }

   private static class TmA extends EmbeddedBaseTransactionManager {
   }

   public static class TxManagerLookupA implements TransactionManagerLookup {
      @Override
      public synchronized TransactionManager getTransactionManager() throws Exception {
         return tma;
      }
   }
}
