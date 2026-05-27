package org.infinispan.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.infinispan.api.sync.SyncWeakCounter;
import org.junit.jupiter.api.Test;

public abstract class AbstractSyncWeakCounterTest extends AbstractAPITest {

   @Test
   protected void testName() {
      SyncWeakCounter counter = ext().syncWeakCounter();
      assertNotNull(counter.name());
   }

   @Test
   protected void testInitialValue() {
      SyncWeakCounter counter = ext().syncWeakCounter();
      assertEquals(0, counter.value());
   }

   @Test
   protected void testIncrement() {
      SyncWeakCounter counter = ext().syncWeakCounter();
      counter.increment();
      assertEquals(1, counter.value());
      counter.increment();
      assertEquals(2, counter.value());
   }

   @Test
   protected void testDecrement() {
      SyncWeakCounter counter = ext().syncWeakCounter();
      counter.decrement();
      assertEquals(-1, counter.value());
      counter.decrement();
      assertEquals(-2, counter.value());
   }

   @Test
   protected void testAdd() {
      SyncWeakCounter counter = ext().syncWeakCounter();
      counter.add(5);
      assertEquals(5, counter.value());
      counter.add(-2);
      assertEquals(3, counter.value());
   }
}
