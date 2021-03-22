package org.infinispan.api.async;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import org.infinispan.api.configuration.CounterConfiguration;

/**
 * @since 13.0
 **/
public interface WeakCounters {
   CompletionStage<WeakCounter> get(String name);

   CompletionStage<WeakCounter> create(String name, CounterConfiguration configuration);

   CompletionStage<Void> remove(String name);

   Flow.Publisher<String> names();
}
