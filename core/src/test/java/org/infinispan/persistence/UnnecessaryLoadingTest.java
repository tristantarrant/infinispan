package org.infinispan.persistence;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNull;

import java.util.concurrent.CompletionStage;

import org.infinispan.Cache;
import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.ConfigurationFor;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.IsolationLevel;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.InvocationContextFactory;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.TestObjectStreamMarshaller;
import org.infinispan.marshall.persistence.impl.MarshalledEntryUtil;
import org.infinispan.persistence.dummy.DummyInMemoryStore;
import org.infinispan.persistence.dummy.DummyInMemoryStoreConfigurationBuilder;
import org.infinispan.persistence.dummy.Element;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.manager.PersistenceManagerImpl;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.infinispan.persistence.spi.NonBlockingStore;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.test.SingleCacheManagerTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.CleanupAfterMethod;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.Test;

/**
 * A test to ensure stuff from a cache store is not loaded unnecessarily if it already exists in memory, or if the
 * Flag.SKIP_CACHE_LOAD is applied.
 *
 * @author Manik Surtani
 * @author Sanne Grinovero
 * @version 4.1
 */
@Test(testName = "persistence.UnnecessaryLoadingTest", groups = "functional", singleThreaded = true)
@CleanupAfterMethod
public class UnnecessaryLoadingTest extends SingleCacheManagerTest {
   DummyInMemoryStore<Object, Object> store;
   private PersistenceManagerImpl persistenceManager;

   @Override
   protected EmbeddedCacheManager createCacheManager() throws Exception {
      ConfigurationBuilder cfg = getDefaultStandaloneCacheConfig(true);
      cfg
            .invocationBatching().enable()
            .persistence()
            .addStore(CountingStoreConfigurationBuilder.class)
            .persistence()
            .addStore(DummyInMemoryStoreConfigurationBuilder.class)
            .locking().isolationLevel(IsolationLevel.READ_COMMITTED); //avoid versioning since we are storing directly in CacheStore
      return TestCacheManagerFactory.createCacheManager(cfg);
   }

   @Override
   protected void setup() throws Exception {
      super.setup();
      persistenceManager = (PersistenceManagerImpl) TestingUtil.extractComponent(cache, PersistenceManager.class);
      store = TestingUtil.getStore(cache, 1, false);

   }

   public void testRepeatedLoads() throws PersistenceException {
      CountingStore countingCS = getCountingCacheStore();
      store.write(MarshalledEntryUtil.create("k1", "v1", cache));
      assertEquals(countingCS.numLoads, 0);
      assertEquals(cache.get("k1"), "v1");
      assertEquals(countingCS.numLoads, 1);
      assertEquals(cache.get("k1"), "v1");
      assertEquals(countingCS.numLoads, 1);
   }


   public void testSkipCacheFlagUsage() throws PersistenceException {
      CountingStore countingCS = getCountingCacheStore();

      store.write(MarshalledEntryUtil.create("k1", "v1", cache));

      assertEquals(countingCS.numLoads, 0);
      //load using SKIP_CACHE_LOAD should not find the object in the store
      assertNull(cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD).get("k1"));
      assertEquals(countingCS.numLoads, 0);

      // counter-verify that the object was actually in the store:
      assertEquals(cache.get("k1"), "v1");
      assertEquals(countingCS.numLoads, 1);

      // now check that put won't return the stored value
      store.write(MarshalledEntryUtil.create("k2", "v2", cache));
      Object putReturn = cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD).put("k2", "v2-second");
      assertNull(putReturn);
      assertEquals(countingCS.numLoads, 1);
      // but it inserted it in the cache:
      assertEquals(cache.get("k2"), "v2-second");
      // perform the put in the cache & store, using same value:
      putReturn = cache.put("k2", "v2-second");
      //returned value from the cache:
      assertEquals(putReturn, "v2-second");
      //and verify that the put operation updated the store too:
      InvocationContextFactory icf = TestingUtil.extractComponent(cache, InvocationContextFactory.class);
      InvocationContext context = icf.createSingleKeyNonTxInvocationContext();
      assertEquals(CompletionStages.join(persistenceManager.loadFromAllStores("k2", context.isOriginLocal(), true)).getValue(), "v2-second");
      assertEquals(countingCS.numLoads, 2, "Expected 2, was " + countingCS.numLoads);

      assertTrue(cache.containsKey("k1"));
      assertFalse(cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD).containsKey("k3"));
      assertEquals(countingCS.numLoads, 2);

      //now with batching:
      boolean batchStarted = cache.getAdvancedCache().startBatch();
      assertTrue(batchStarted);
      assertNull(cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD).get("k1batch"));
      assertEquals(countingCS.numLoads, 2);
      assertNull(cache.getAdvancedCache().get("k2batch"));
      assertEquals(countingCS.numLoads, 3);
      cache.endBatch(true);
   }

   private CountingStore getCountingCacheStore() {
      CountingStore countingCS = TestingUtil.getStore(cache, 0, true);
      reset(cache, countingCS);
      return countingCS;
   }

   public void testSkipCacheLoadFlagUsage() throws PersistenceException {
      CountingStore countingCS = getCountingCacheStore();

      TestObjectStreamMarshaller sm = new TestObjectStreamMarshaller();
      try {
         store.write(MarshalledEntryUtil.create("home", "Vermezzo", sm));
         store.write(MarshalledEntryUtil.create("home-second", "Newcastle Upon Tyne", sm));

         assertEquals(countingCS.numLoads, 0);
         //load using SKIP_CACHE_LOAD should not find the object in the store
         assertNull(cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD).get("home"));
         assertEquals(countingCS.numLoads, 0);

         assertNull(cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD).put("home", "Newcastle"));
         assertEquals(countingCS.numLoads, 0);

         final Object put = cache.getAdvancedCache().put("home-second", "Newcastle Upon Tyne, second");
         assertEquals(put, "Newcastle Upon Tyne");
         assertEquals(countingCS.numLoads, 1);
      } finally {
         sm.stop();
      }
   }

   private void reset(Cache<?, ?> cache, CountingStore countingCS) {
      cache.clear();
      countingCS.numLoads = 0;
   }

   public static class CountingStore implements NonBlockingStore<Object, Object> {
      public int numLoads;

      @Override
      public CompletionStage<Void> start(InitializationContext ctx) {
         return null;
      }

      @Override
      public CompletionStage<Void> stop() {
         return null;
      }

      @Override
      public CompletionStage<MarshallableEntry<Object, Object>> load(int segment, Object key) {
         ++numLoads;
         return null;
      }

      @Override
      public CompletionStage<Void> write(int segment, MarshallableEntry<?, ?> entry) {
         return null;
      }

      @Override
      public CompletionStage<Boolean> delete(int segment, Object key) {
         return null;
      }

      @Override
      public CompletionStage<Void> clear() {
         return null;
      }
   }

   @BuiltBy(CountingStoreConfigurationBuilder.class)
   @ConfigurationFor(CountingStore.class)
   public static class CountingStoreConfiguration extends AbstractStoreConfiguration<CountingStoreConfiguration> {

      public CountingStoreConfiguration(AttributeSet attributes, AsyncStoreConfiguration async) {
         super(Element.DUMMY_STORE, attributes, async);
      }
   }

   public static class CountingStoreConfigurationBuilder extends AbstractStoreConfigurationBuilder<CountingStoreConfiguration, CountingStoreConfigurationBuilder> {

      public CountingStoreConfigurationBuilder(PersistenceConfigurationBuilder builder) {
         super(builder, CountingStoreConfiguration.attributeDefinitionSet());
      }

      @Override
      public CountingStoreConfiguration create() {
         return new CountingStoreConfiguration(attributes.protect(), async.create());
      }

      @Override
      public CountingStoreConfigurationBuilder self() {
         return this;
      }

   }
}
