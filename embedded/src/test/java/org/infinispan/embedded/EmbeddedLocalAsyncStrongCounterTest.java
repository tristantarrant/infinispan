package org.infinispan.embedded;

import org.infinispan.api.AbstractAsyncStrongCounterTest;
import org.infinispan.api.InfinispanAPIExtension;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterType;
import org.junit.jupiter.api.extension.RegisterExtension;

public class EmbeddedLocalAsyncStrongCounterTest extends AbstractAsyncStrongCounterTest {

   @RegisterExtension
   static EmbeddedInfinispanAPIExtension ext = new EmbeddedInfinispanAPIExtension(1, false, null,
         CounterConfiguration.builder(CounterType.UNBOUNDED_STRONG).build(),
         null);

   @Override
   protected InfinispanAPIExtension ext() {
      return ext;
   }
}
