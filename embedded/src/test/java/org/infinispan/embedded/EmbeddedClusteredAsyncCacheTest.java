package org.infinispan.embedded;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.junit.jupiter.api.extension.RegisterExtension;

public class EmbeddedClusteredAsyncCacheTest extends EmbeddedAsyncCacheTest {
   @RegisterExtension
   static EmbeddedInfinispanAPIExtension ext = EmbeddedInfinispanExtensionBuilder
         .clustered(2)
         .cacheConfiguration(new ConfigurationBuilder()
               .clustering().cacheMode(CacheMode.DIST_SYNC).hash().numOwners(1)
               .build())
         .build();

   @Override
   protected EmbeddedInfinispanAPIExtension ext() {
      return ext;
   }
}
