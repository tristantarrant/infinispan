package org.infinispan.embedded;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import org.infinispan.api.async.AsyncWeakCounter;
import org.infinispan.api.async.AsyncWeakCounters;
import org.infinispan.api.configuration.CounterConfiguration;
import org.infinispan.counter.EmbeddedCounterManagerFactory;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.WeakCounter;
import org.reactivestreams.FlowAdapters;

import io.reactivex.rxjava3.core.Flowable;

/**
 * @since 16.3
 */
public class EmbeddedAsyncWeakCounters implements AsyncWeakCounters {
   private final Embedded embedded;
   private final CounterManager counterManager;

   EmbeddedAsyncWeakCounters(Embedded embedded) {
      this.embedded = embedded;
      this.counterManager = EmbeddedCounterManagerFactory.asCounterManager(embedded.cacheManager);
   }

   @Override
   public CompletionStage<AsyncWeakCounter> get(String name) {
      WeakCounter counter = counterManager.getWeakCounter(name);
      return CompletableFuture.completedFuture(new EmbeddedAsyncWeakCounter(embedded, counter));
   }

   @Override
   public CompletionStage<AsyncWeakCounter> create(String name, CounterConfiguration configuration) {
      counterManager.defineCounter(name, (org.infinispan.counter.api.CounterConfiguration) configuration);
      return get(name);
   }

   @Override
   public CompletionStage<Void> remove(String name) {
      counterManager.remove(name);
      return CompletableFuture.completedFuture(null);
   }

   @Override
   public Flow.Publisher<String> names() {
      return FlowAdapters.toFlowPublisher(
            Flowable.fromIterable(counterManager.getCounterNames())
                  .filter(name -> {
                     org.infinispan.counter.api.CounterConfiguration config = counterManager.getConfiguration(name);
                     return config != null && config.type() == CounterType.WEAK;
                  })
      );
   }
}
