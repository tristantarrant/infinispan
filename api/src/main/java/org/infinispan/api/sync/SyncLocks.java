package org.infinispan.api.sync;

import org.infinispan.api.common.CloseableIterable;
import org.infinispan.api.configuration.LockConfiguration;

/**
 * @since 14.0
 **/
public interface SyncLocks {

   /**
    * Creates a lock with the given name and configuration. If the lock already exists, it is returned.
    *
    * @param name          the lock name
    * @param configuration the lock configuration
    * @return the lock
    */
   SyncLock create(String name, LockConfiguration configuration);

   /**
    * Returns an existing lock with the given name.
    *
    * @param name the lock name
    * @return the lock
    */
   SyncLock get(String name);

   /**
    * Removes the lock with the given name.
    *
    * @param name the lock name
    */
   void remove(String name);

   /**
    * Returns the names of all available locks.
    *
    * @return an iterable of lock names
    */
   CloseableIterable<String> names();
}
