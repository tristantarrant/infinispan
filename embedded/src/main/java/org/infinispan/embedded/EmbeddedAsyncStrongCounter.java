package org.infinispan.embedded;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import org.infinispan.api.async.AsyncContainer;
import org.infinispan.api.async.AsyncStrongCounter;
import org.infinispan.api.common.events.counter.CounterEvent;
import org.infinispan.api.common.events.counter.CounterState;
import org.infinispan.api.configuration.CounterConfiguration;
import org.infinispan.counter.api.Handle;
import org.infinispan.counter.api.StrongCounter;

/**
 * @since 16.3
 */
public class EmbeddedAsyncStrongCounter implements AsyncStrongCounter {
   private final Embedded embedded;
   private final StrongCounter counter;

   EmbeddedAsyncStrongCounter(Embedded embedded, StrongCounter counter) {
      this.embedded = embedded;
      this.counter = counter;
   }

   @Override
   public String name() {
      return counter.getName();
   }

   @Override
   public CompletionStage<CounterConfiguration> configuration() {
      return CompletableFuture.completedFuture((CounterConfiguration) counter.getConfiguration());
   }

   @Override
   public AsyncContainer container() {
      return embedded.async();
   }

   @Override
   public CompletionStage<Long> value() {
      return counter.getValue();
   }

   @Override
   public CompletionStage<Long> addAndGet(long delta) {
      return counter.addAndGet(delta);
   }

   @Override
   public CompletionStage<Void> reset() {
      return counter.reset();
   }

   @Override
   public CompletionStage<AutoCloseable> listen(Consumer<CounterEvent> listener) {
      Handle<?> handle = counter.addListener(event -> listener.accept(new CounterEvent() {
         @Override
         public long getOldValue() {
            return event.getOldValue();
         }

         @Override
         public CounterState getOldState() {
            return CounterState.valueOf(event.getOldState().ordinal());
         }

         @Override
         public long getNewValue() {
            return event.getNewValue();
         }

         @Override
         public CounterState getNewState() {
            return CounterState.valueOf(event.getNewState().ordinal());
         }
      }));
      return CompletableFuture.completedFuture(handle::remove);
   }

   @Override
   public CompletionStage<Long> compareAndSwap(long expect, long update) {
      return counter.compareAndSwap(expect, update);
   }

   @Override
   public CompletionStage<Long> getAndSet(long value) {
      return counter.getAndSet(value);
   }
}
