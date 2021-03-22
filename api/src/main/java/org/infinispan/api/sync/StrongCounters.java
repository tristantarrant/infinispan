package org.infinispan.api.sync;

import org.infinispan.api.configuration.CounterConfiguration;

/**
 * @since 13.0
 **/
public interface StrongCounters {
   StrongCounter get(String name);

   StrongCounter create(String name, CounterConfiguration counterConfiguration);

   void remove(String name);

   Iterable<String> names();
}
