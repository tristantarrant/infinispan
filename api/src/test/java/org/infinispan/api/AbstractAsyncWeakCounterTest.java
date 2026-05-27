package org.infinispan.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.infinispan.api.async.AsyncWeakCounter;
import org.junit.jupiter.api.Test;

public abstract class AbstractAsyncWeakCounterTest extends AbstractAPITest {

   @Test
   protected void testName() {
      AsyncWeakCounter counter = ext().asyncWeakCounter();
      assertNotNull(counter.name());
   }

   @Test
   protected void testInitialValue() {
      AsyncWeakCounter counter = ext().asyncWeakCounter();
      assertEquals(0, counter.value().toCompletableFuture().join());
   }

   @Test
   protected void testIncrement() {
      AsyncWeakCounter counter = ext().asyncWeakCounter();
      counter.increment().toCompletableFuture().join();
      assertEquals(1, counter.value().toCompletableFuture().join());
      counter.increment().toCompletableFuture().join();
      assertEquals(2, counter.value().toCompletableFuture().join());
   }

   @Test
   protected void testDecrement() {
      AsyncWeakCounter counter = ext().asyncWeakCounter();
      counter.decrement().toCompletableFuture().join();
      assertEquals(-1, counter.value().toCompletableFuture().join());
      counter.decrement().toCompletableFuture().join();
      assertEquals(-2, counter.value().toCompletableFuture().join());
   }

   @Test
   protected void testAdd() {
      AsyncWeakCounter counter = ext().asyncWeakCounter();
      counter.add(5).toCompletableFuture().join();
      assertEquals(5, counter.value().toCompletableFuture().join());
      counter.add(-2).toCompletableFuture().join();
      assertEquals(3, counter.value().toCompletableFuture().join());
   }
}
