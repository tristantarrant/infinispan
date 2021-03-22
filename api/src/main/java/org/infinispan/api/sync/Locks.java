package org.infinispan.api.sync;

import org.infinispan.api.configuration.LockConfiguration;

/**
 *
 * @since 13.0
 **/
public interface Locks {
   Lock create(String name, LockConfiguration configuration);

   Lock get(String name);

   void remove(String name);

   Iterable<String> names();
}
