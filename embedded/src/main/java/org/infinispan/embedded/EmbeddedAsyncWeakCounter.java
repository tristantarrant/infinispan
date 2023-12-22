package org.infinispan.embedded;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.infinispan.api.async.AsyncContainer;
import org.infinispan.api.async.AsyncWeakCounter;
import org.infinispan.api.configuration.CounterConfiguration;
import org.infinispan.counter.api.WeakCounter;

/**
 * @since 16.3
 */
public class EmbeddedAsyncWeakCounter implements AsyncWeakCounter {
   private final Embedded embedded;
   private final WeakCounter counter;

   EmbeddedAsyncWeakCounter(Embedded embedded, WeakCounter counter) {
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
      return CompletableFuture.completedFuture(counter.getValue());
   }

   @Override
   public CompletionStage<Void> add(long delta) {
      return counter.add(delta);
   }
}
