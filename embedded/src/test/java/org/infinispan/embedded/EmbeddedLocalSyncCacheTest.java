package org.infinispan.embedded;

import org.junit.jupiter.api.extension.RegisterExtension;

public class EmbeddedLocalSyncCacheTest extends EmbeddedSyncCacheTest {
   @RegisterExtension
   static EmbeddedInfinispanAPIExtension ext = new EmbeddedInfinispanAPIExtension();

   @Override
   protected EmbeddedInfinispanAPIExtension ext() {
      return ext;
   }
}
