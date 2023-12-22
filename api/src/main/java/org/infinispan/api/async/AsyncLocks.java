package org.infinispan.api.async;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import org.infinispan.api.configuration.LockConfiguration;

/**
 * @since 14.0
 **/
public interface AsyncLocks {

   /**
    * Creates a lock with the given name and configuration. If the lock already exists, it is returned.
    *
    * @param name          the lock name
    * @param configuration the lock configuration
    * @return a {@link CompletionStage} completing with the lock
    */
   CompletionStage<AsyncLock> create(String name, LockConfiguration configuration);

   /**
    * Returns an existing lock with the given name.
    *
    * @param name the lock name
    * @return a {@link CompletionStage} completing with the lock
    */
   CompletionStage<AsyncLock> lock(String name);

   /**
    * Removes the lock with the given name.
    *
    * @param name the lock name
    * @return a {@link CompletionStage} that completes when the lock is removed
    */
   CompletionStage<Void> remove(String name);

   /**
    * Returns the names of all available locks.
    *
    * @return a publisher of lock names
    */
   Flow.Publisher<String> names();
}
