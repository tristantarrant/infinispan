package org.infinispan.xsite.irac.persistence;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.IdentityKeyValueWrapper;
import org.testng.annotations.Test;

/**
 * Tests if the IRAC metadata is properly stored and retrieved from a {@link org.infinispan.persistence.sifs.NonBlockingSoftIndexFileStore}.
 *
 * @author Pedro Ruivo
 * @since 10.1
 */
@Test(groups = "functional", testName = "xsite.irac.persistence.IracSoftIndexFileStoreTest")
public class IracSoftIndexFileStoreTest extends BaseIracPersistenceTest<String> {


   public IracSoftIndexFileStoreTest() {
      super(IdentityKeyValueWrapper.instance());
   }

   @Override
   protected void configure(ConfigurationBuilder builder) {
      builder.persistence().addSoftIndexFileStore();
   }
}
