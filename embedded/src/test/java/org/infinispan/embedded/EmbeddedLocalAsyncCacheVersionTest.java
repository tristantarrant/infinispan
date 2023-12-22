package org.infinispan.embedded;

import org.infinispan.api.AbstractAsyncCacheVersionTest;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.EmbeddedTransactionManagerLookup;
import org.junit.jupiter.api.extension.RegisterExtension;

public class EmbeddedLocalAsyncCacheVersionTest extends AbstractAsyncCacheVersionTest {
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
