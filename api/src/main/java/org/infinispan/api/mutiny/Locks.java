package org.infinispan.api.mutiny;

import org.infinispan.api.configuration.LockConfiguration;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 *
 * @since 13.0
 **/
public interface Locks {
   Uni<Lock> lock(String name);

   Uni<Lock> create(String name, LockConfiguration configuration);

   Uni<Void> remove(String name);

   Multi<String> names();
}
