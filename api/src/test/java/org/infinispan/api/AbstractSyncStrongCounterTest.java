package org.infinispan.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.infinispan.api.sync.SyncStrongCounter;
import org.junit.jupiter.api.Test;

public abstract class AbstractSyncStrongCounterTest extends AbstractAPITest {

   @Test
   protected void testName() {
      SyncStrongCounter counter = ext().syncStrongCounter();
      assertNotNull(counter.name());
   }

   @Test
   protected void testInitialValue() {
      SyncStrongCounter counter = ext().syncStrongCounter();
      assertEquals(0, counter.value());
   }

   @Test
   protected void testIncrementAndGet() {
      SyncStrongCounter counter = ext().syncStrongCounter();
      assertEquals(1, counter.incrementAndGet());
      assertEquals(2, counter.incrementAndGet());
      assertEquals(2, counter.value());
   }

   @Test
   protected void testDecrementAndGet() {
      SyncStrongCounter counter = ext().syncStrongCounter();
      assertEquals(-1, counter.decrementAndGet());
      assertEquals(-2, counter.decrementAndGet());
      assertEquals(-2, counter.value());
   }

   @Test
   protected void testAddAndGet() {
      SyncStrongCounter counter = ext().syncStrongCounter();
      assertEquals(5, counter.addAndGet(5));
      assertEquals(3, counter.addAndGet(-2));
      assertEquals(3, counter.value());
   }

   @Test
   protected void testReset() {
      SyncStrongCounter counter = ext().syncStrongCounter();
      counter.addAndGet(10);
      assertEquals(10, counter.value());
      counter.reset().join();
      assertEquals(0, counter.value());
   }

   @Test
   protected void testCompareAndSet() {
      SyncStrongCounter counter = ext().syncStrongCounter();
      assertTrue(counter.compareAndSet(0, 5));
      assertEquals(5, counter.value());
      assertFalse(counter.compareAndSet(0, 10));
      assertEquals(5, counter.value());
   }

   @Test
   protected void testCompareAndSwap() {
      SyncStrongCounter counter = ext().syncStrongCounter();
      assertEquals(0, counter.compareAndSwap(0, 5));
      assertEquals(5, counter.value());
      assertEquals(5, counter.compareAndSwap(0, 10));
      assertEquals(5, counter.value());
   }

   @Test
   protected void testGetAndSet() {
      SyncStrongCounter counter = ext().syncStrongCounter();
      assertEquals(0, counter.getAndSet(5));
      assertEquals(5, counter.getAndSet(10));
      assertEquals(10, counter.value());
   }

}
