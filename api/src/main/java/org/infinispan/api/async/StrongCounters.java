package org.infinispan.api.async;

import java.util.concurrent.CompletionStage;

import io.smallrye.mutiny.Multi;

import org.infinispan.api.configuration.CounterConfiguration;

/**
 * @since 13.0
 **/
public interface StrongCounters {
   CompletionStage<StrongCounter> get(String name);

   CompletionStage<StrongCounter> create(String name, CounterConfiguration configuration);

   CompletionStage<Void> remove(String name);

   Multi<String> names();
}
