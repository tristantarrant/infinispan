package org.infinispan.distribution;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

/**
 * @author Mircea Markus
 * @since 5.1
 */
@Test (groups = "functional", testName = "distribution.InvalidationNoReplicationNoTxTest")
public class InvalidationNoReplicationNoTxTest extends InvalidationNoReplicationTest {

   public InvalidationNoReplicationNoTxTest() {
      transactional = false;
   }

   public void testInvalidation() {
      cache(1).put(k0, "v0");
      assert advancedCache(0).getDataContainer().containsKey(k0);
      assert !advancedCache(1).getDataContainer().containsKey(k0);

      assertEquals(cache(1).get(k0), "v0");
      assert advancedCache(0).getDataContainer().containsKey(k0);
      assert advancedCache(1).getDataContainer().containsKey(k0);

      log.info("Here is the put!");
      log.infof("Cache 0=%s cache 1=%s", address(0), address(1));
      cache(0).put(k0, "v1");

      log.info("before assertions!");
      assertNull(advancedCache(1).getDataContainer().peek(k0));
      assertEquals(advancedCache(0).getDataContainer().peek(k0).getValue(), "v1");
   }

}
