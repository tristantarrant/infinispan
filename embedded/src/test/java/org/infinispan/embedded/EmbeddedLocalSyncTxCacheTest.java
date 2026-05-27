package org.infinispan.embedded;

import org.infinispan.api.AbstractSyncTxCacheTest;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.EmbeddedTransactionManagerLookup;
import org.junit.jupiter.api.extension.RegisterExtension;

public class EmbeddedLocalSyncTxCacheTest extends AbstractSyncTxCacheTest {
   @RegisterExtension
   static EmbeddedInfinispanAPIExtension ext = new EmbeddedInfinispanAPIExtension(1, false,
         new ConfigurationBuilder()
               .transaction().transactionMode(TransactionMode.TRANSACTIONAL)
               .transactionManagerLookup(new EmbeddedTransactionManagerLookup())
               .lockingMode(LockingMode.OPTIMISTIC)
               .build());

   @Override
   protected EmbeddedInfinispanAPIExtension ext() {
      return ext;
   }
}
