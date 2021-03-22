package org.infinispan.api;

import org.infinispan.api.mutiny.MutinyCache;

import io.smallrye.mutiny.Uni;

/**
 * @since 13.0
 **/
public class MutinyAPITest {
   public void testAPI() {
      try (Infinispan infinispan = Infinispan.create("file:///path/to/infinispan.xml")) {
         Uni<MutinyCache<String, String>> uni = infinispan.mutiny().caches().cache("mycache");
         uni.onItem().invoke(c -> c.put("k", "v")).subscribe();
         uni.onItem().transformToMulti(c -> c.query("...").limit(100).find());
      }
   }
}
