package org.infinispan.embedded;

import org.infinispan.api.Infinispan;
import org.infinispan.api.InfinispanAPIExtension;
import org.infinispan.api.configuration.CacheConfiguration;
import org.infinispan.api.sync.SyncStrongCounter;
import org.infinispan.api.sync.SyncWeakCounter;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.counter.EmbeddedCounterManagerFactory;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.jspecify.annotations.Nullable;

public class EmbeddedInfinispanAPIExtension extends InfinispanAPIExtension {
   private final @Nullable CounterConfiguration strongCounterConfiguration;
   private final @Nullable CounterConfiguration weakCounterConfiguration;

   public EmbeddedInfinispanAPIExtension() {
      this(1, false, null, null, null);
   }

   public EmbeddedInfinispanAPIExtension(int numNodes, boolean clustered, @Nullable CacheConfiguration cacheConfiguration) {
      this(numNodes, clustered, cacheConfiguration, null, null);
   }

   public EmbeddedInfinispanAPIExtension(int numNodes, boolean clustered, @Nullable CacheConfiguration cacheConfiguration,
                                         @Nullable CounterConfiguration strongCounterConfiguration,
                                         @Nullable CounterConfiguration weakCounterConfiguration) {
      super(numNodes, clustered, cacheConfiguration);
      this.strongCounterConfiguration = strongCounterConfiguration;
      this.weakCounterConfiguration = weakCounterConfiguration;
   }

   @Override
   protected Infinispan createInfinispan(String name, int index) {
      String scheme = clustered() ? "cluster" : "local";
      return Infinispan.create("infinispan:" + scheme + "://" + name + "-" + index);
   }

   @Override
   protected CacheConfiguration defaultCacheConfiguration() {
      return new ConfigurationBuilder().build();
   }

   @Override
   protected SyncStrongCounter createStrongCounter(String name) {
      counterManager().defineCounter(name, strongCounterConfiguration);
      return infinispan().sync().strongCounters().get(name);
   }

   @Override
   protected SyncWeakCounter createWeakCounter(String name) {
      counterManager().defineCounter(name, weakCounterConfiguration);
      return infinispan().sync().weakCounters().get(name);
   }

   private CounterManager counterManager() {
      return EmbeddedCounterManagerFactory.asCounterManager(((Embedded) infinispan()).cacheManager);
   }
}
