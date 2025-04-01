package org.infinispan.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.test.EmbeddedTestDriver;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.EmbeddedTransactionManagerLookup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class TransactionalCacheConfigTest {

   @RegisterExtension
   static EmbeddedTestDriver EMBEDDED = EmbeddedTestDriver.local().build();

   @Test
   public void testConfig() {
      ConfigurationBuilder c = new ConfigurationBuilder();
      assertFalse(c.build().transaction().transactionMode().isTransactional());
      c.transaction().transactionMode(TransactionMode.TRANSACTIONAL);
      assertTrue(c.build().transaction().transactionMode().isTransactional());
      c.transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL);
      assertFalse(c.build().transaction().transactionMode().isTransactional());
   }

   @Test
   public void testTransactionModeOverride() {
      ConfigurationBuilder tx = new ConfigurationBuilder();
      tx.transaction().transactionMode(TransactionMode.TRANSACTIONAL);
      assertEquals(TransactionMode.TRANSACTIONAL, EMBEDDED.cache(tx).getCacheConfiguration().transaction().transactionMode());
      ConfigurationBuilder nonTx = new ConfigurationBuilder();
      nonTx.transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL);
      assertEquals(TransactionMode.NON_TRANSACTIONAL, EMBEDDED.cache(nonTx).getCacheConfiguration().transaction().transactionMode());
   }

   @Test
   public void testDefaults() {
      Configuration c = new ConfigurationBuilder().build();
      assertFalse(c.transaction().transactionMode().isTransactional());

      c = new ConfigurationBuilder().transaction().transactionMode(TransactionMode.TRANSACTIONAL).build();
      assertTrue(c.transaction().transactionMode().isTransactional());
   }

   @Test
   public void testTransactionalityInduced() {
      ConfigurationBuilder cb = new ConfigurationBuilder();
      Configuration c = cb.build();
      assertFalse(c.transaction().transactionMode().isTransactional());

      c = cb.transaction().transactionManagerLookup(new EmbeddedTransactionManagerLookup()).build();
      assertTrue(c.transaction().transactionMode().isTransactional());

      cb = new ConfigurationBuilder();
      cb.invocationBatching().enable();
      assertTrue(cb.build().transaction().transactionMode().isTransactional());
   }

   @Test
   public void testInvocationBatchingAndInducedTm() {
      ConfigurationBuilder cb = new ConfigurationBuilder();
      cb.invocationBatching().enable();
      assertTrue(cb.build().transaction().transactionMode().isTransactional());
      assertNotNull(EMBEDDED.cache(cb).getAdvancedCache().getTransactionManager());
   }

   @Test
   public void testBatchingAndTransactionalCache() {
      ConfigurationBuilder cb = new ConfigurationBuilder();
      cb.invocationBatching().enable();
      Cache<Object, Object> cache = EMBEDDED.cache(cb);
      assertTrue(cache.getCacheConfiguration().invocationBatching().enabled());
      assertTrue(cache.getCacheConfiguration().transaction().transactionMode().isTransactional());
   }
}
