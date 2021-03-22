package org.infinispan.api.mutiny;

import org.infinispan.api.configuration.CounterConfiguration;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * @since 13.0
 **/
public interface StrongCounters {
   Uni<StrongCounter> get(String name);

   Uni<StrongCounter> create(String name, CounterConfiguration configuration);

   Uni<Void> remove(String name);

   Multi<String> names();
}
