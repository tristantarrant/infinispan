package org.infinispan.api;

import org.infinispan.api.sync.WeakCounter;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 13.0
 **/
public class SyncCounterAPITest {
   public void testCounterAPI() {
      try (Infinispan infinispan = Infinispan.create("file:///path/to/infinispan.xml")) {
         WeakCounter counter = infinispan.sync().weakCounters().get("counter");
         counter.increment();
         counter.decrement();
         long value = counter.value();
         counter.reset();
      }
   }
}
