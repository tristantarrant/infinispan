package org.infinispan.api.mutiny;

import org.infinispan.api.configuration.CounterConfiguration;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * @since 13.0
 **/
public interface WeakCounters {
   Uni<WeakCounter> get(String name);

   Uni<WeakCounter> create(String name, CounterConfiguration configuration);

   Uni<Void> remove(String name);

   Multi<String> names();
}
