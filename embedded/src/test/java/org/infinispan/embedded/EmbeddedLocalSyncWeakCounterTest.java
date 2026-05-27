package org.infinispan.embedded;

import org.infinispan.api.AbstractSyncWeakCounterTest;
import org.infinispan.api.InfinispanAPIExtension;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterType;
import org.junit.jupiter.api.extension.RegisterExtension;

public class EmbeddedLocalSyncWeakCounterTest extends AbstractSyncWeakCounterTest {

   @RegisterExtension
   static EmbeddedInfinispanAPIExtension ext = new EmbeddedInfinispanAPIExtension(1, false, null,
         null,
         CounterConfiguration.builder(CounterType.WEAK).build());

   @Override
   protected InfinispanAPIExtension ext() {
      return ext;
   }
}
