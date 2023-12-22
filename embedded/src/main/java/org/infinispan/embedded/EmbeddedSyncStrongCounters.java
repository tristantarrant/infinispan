package org.infinispan.embedded;

import org.infinispan.api.configuration.CounterConfiguration;
import org.infinispan.api.sync.SyncStrongCounter;
import org.infinispan.api.sync.SyncStrongCounters;
import org.infinispan.counter.EmbeddedCounterManagerFactory;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.StrongCounter;

/**
 * @since 16.3
 */
public class EmbeddedSyncStrongCounters implements SyncStrongCounters {
   private final Embedded embedded;
   private final CounterManager counterManager;

   EmbeddedSyncStrongCounters(Embedded embedded) {
      this.embedded = embedded;
      this.counterManager = EmbeddedCounterManagerFactory.asCounterManager(embedded.cacheManager);
   }

   @Override
   public SyncStrongCounter get(String name) {
      StrongCounter counter = counterManager.getStrongCounter(name);
      return new EmbeddedSyncStrongCounter(embedded, counter);
   }

   @Override
   public SyncStrongCounter create(String name, CounterConfiguration counterConfiguration) {
      counterManager.defineCounter(name, (org.infinispan.counter.api.CounterConfiguration) counterConfiguration);
      return get(name);
   }

   @Override
   public void remove(String name) {
      counterManager.remove(name);
   }

   @Override
   public Iterable<String> names() {
      return counterManager.getCounterNames().stream()
            .filter(name -> {
               org.infinispan.counter.api.CounterConfiguration config = counterManager.getConfiguration(name);
               return config != null && config.type() != CounterType.WEAK;
            })
            .toList();
   }
}
