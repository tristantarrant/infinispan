package org.infinispan.embedded;

import org.infinispan.configuration.cache.Configuration;
import org.jspecify.annotations.Nullable;

public class EmbeddedInfinispanExtensionBuilder {
   private int numNodes = 1;
   private boolean clustered = false;
   private @Nullable Configuration cacheConfiguration;

   public static EmbeddedInfinispanAPIExtension local() {
      return new EmbeddedInfinispanExtensionBuilder().build();
   }

   public static EmbeddedInfinispanExtensionBuilder clustered(int numNodes) {
      return new EmbeddedInfinispanExtensionBuilder()
            .numNodes(numNodes)
            .clustered(true);
   }

   private EmbeddedInfinispanExtensionBuilder() {
   }

   public EmbeddedInfinispanExtensionBuilder numNodes(int numNodes) {
      this.numNodes = numNodes;
      return this;
   }

   public EmbeddedInfinispanExtensionBuilder clustered(boolean clustered) {
      this.clustered = clustered;
      return this;
   }

   public EmbeddedInfinispanExtensionBuilder cacheConfiguration(Configuration cacheConfiguration) {
      this.cacheConfiguration = cacheConfiguration;
      return this;
   }

   public EmbeddedInfinispanAPIExtension build() {
      return new EmbeddedInfinispanAPIExtension(numNodes, clustered, cacheConfiguration);
   }
}
