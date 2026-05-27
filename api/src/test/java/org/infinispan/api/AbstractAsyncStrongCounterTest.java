package org.infinispan.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.infinispan.api.async.AsyncStrongCounter;
import org.junit.jupiter.api.Test;

public abstract class AbstractAsyncStrongCounterTest extends AbstractAPITest {

   @Test
   protected void testName() {
      AsyncStrongCounter counter = ext().asyncStrongCounter();
      assertNotNull(counter.name());
   }

   @Test
   protected void testInitialValue() {
      AsyncStrongCounter counter = ext().asyncStrongCounter();
      assertEquals(0, counter.value().toCompletableFuture().join());
   }

   @Test
   protected void testIncrementAndGet() {
      AsyncStrongCounter counter = ext().asyncStrongCounter();
      assertEquals(1, counter.incrementAndGet().toCompletableFuture().join());
      assertEquals(2, counter.incrementAndGet().toCompletableFuture().join());
      assertEquals(2, counter.value().toCompletableFuture().join());
   }

   @Test
   protected void testDecrementAndGet() {
      AsyncStrongCounter counter = ext().asyncStrongCounter();
      assertEquals(-1, counter.decrementAndGet().toCompletableFuture().join());
      assertEquals(-2, counter.decrementAndGet().toCompletableFuture().join());
      assertEquals(-2, counter.value().toCompletableFuture().join());
   }

   @Test
   protected void testAddAndGet() {
      AsyncStrongCounter counter = ext().asyncStrongCounter();
      assertEquals(5, counter.addAndGet(5).toCompletableFuture().join());
      assertEquals(3, counter.addAndGet(-2).toCompletableFuture().join());
      assertEquals(3, counter.value().toCompletableFuture().join());
   }

   @Test
   protected void testReset() {
      AsyncStrongCounter counter = ext().asyncStrongCounter();
      counter.addAndGet(10).toCompletableFuture().join();
      assertEquals(10, counter.value().toCompletableFuture().join());
      counter.reset().toCompletableFuture().join();
      assertEquals(0, counter.value().toCompletableFuture().join());
   }

   @Test
   protected void testCompareAndSet() {
      AsyncStrongCounter counter = ext().asyncStrongCounter();
      assertTrue(counter.compareAndSet(0, 5).toCompletableFuture().join());
      assertEquals(5, counter.value().toCompletableFuture().join());
      assertFalse(counter.compareAndSet(0, 10).toCompletableFuture().join());
      assertEquals(5, counter.value().toCompletableFuture().join());
   }

   @Test
   protected void testCompareAndSwap() {
      AsyncStrongCounter counter = ext().asyncStrongCounter();
      assertEquals(0, counter.compareAndSwap(0, 5).toCompletableFuture().join());
      assertEquals(5, counter.value().toCompletableFuture().join());
      assertEquals(5, counter.compareAndSwap(0, 10).toCompletableFuture().join());
      assertEquals(5, counter.value().toCompletableFuture().join());
   }

   @Test
   protected void testGetAndSet() {
      AsyncStrongCounter counter = ext().asyncStrongCounter();
      assertEquals(0, counter.getAndSet(5).toCompletableFuture().join());
      assertEquals(5, counter.getAndSet(10).toCompletableFuture().join());
      assertEquals(10, counter.value().toCompletableFuture().join());
   }

}
