package org.infinispan.api.sync;

import org.infinispan.api.configuration.CounterConfiguration;

/**
 * @since 13.0
 **/
public interface WeakCounters {
   WeakCounter get(String name);

   WeakCounter create(String name, CounterConfiguration counterConfiguration);

   void remove(String name);

   Iterable<String> names();
}
