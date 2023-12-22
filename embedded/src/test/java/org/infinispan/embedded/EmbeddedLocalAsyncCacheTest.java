package org.infinispan.embedded;

import org.junit.jupiter.api.extension.RegisterExtension;

public class EmbeddedLocalAsyncCacheTest extends EmbeddedAsyncCacheTest {
   @RegisterExtension
   static EmbeddedInfinispanAPIExtension ext = new EmbeddedInfinispanAPIExtension();

   @Override
   protected EmbeddedInfinispanAPIExtension ext() {
      return ext;
   }
}
