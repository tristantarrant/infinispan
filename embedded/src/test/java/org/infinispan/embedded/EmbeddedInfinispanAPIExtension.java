package org.infinispan.embedded;

import org.infinispan.api.Infinispan;
import org.infinispan.api.InfinispanAPIExtension;
import org.infinispan.api.configuration.CacheConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jspecify.annotations.Nullable;

public class EmbeddedInfinispanAPIExtension extends InfinispanAPIExtension {

   public EmbeddedInfinispanAPIExtension() {
   }

   public EmbeddedInfinispanAPIExtension(int numNodes, boolean clustered, @Nullable CacheConfiguration cacheConfiguration) {
      super(numNodes, clustered, cacheConfiguration);
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
}
