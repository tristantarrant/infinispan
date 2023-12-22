package org.infinispan.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.infinispan.api.common.CacheEntry;
import org.infinispan.api.common.CacheEntryVersion;
import org.infinispan.api.sync.SyncCache;
import org.junit.jupiter.api.Test;

public abstract class AbstractSyncCacheVersionTest extends AbstractAPITest {

   @Test
   protected void testReplaceWithVersion() {
      SyncCache<String, String> cache = ext().syncCache();
      cache.set(k(), v(1));
      CacheEntry<String, String> entry = cache.getEntry(k());
      assertNotNull(entry);
      CacheEntryVersion version = entry.metadata().version();
      assertTrue(cache.replace(k(), v(2), version));
      assertEquals(v(2), cache.get(k()));
   }

   @Test
   protected void testReplaceWithWrongVersion() {
      SyncCache<String, String> cache = ext().syncCache();
      cache.set(k(), v(1));
      CacheEntry<String, String> entry1 = cache.getEntry(k());
      assertNotNull(entry1);
      CacheEntryVersion oldVersion = entry1.metadata().version();
      cache.set(k(), v(2));
      assertFalse(cache.replace(k(), "v3", oldVersion));
      assertEquals(v(2), cache.get(k()));
   }

   @Test
   protected void testGetOrReplaceEntry() {
      SyncCache<String, String> cache = ext().syncCache();
      cache.set(k(), v(1));
      CacheEntry<String, String> entry = cache.getEntry(k());
      assertNotNull(entry);
      CacheEntryVersion version = entry.metadata().version();
      CacheEntry<String, String> previous = cache.getOrReplaceEntry(k(), v(2), version);
      assertNotNull(previous);
      assertEquals(v(2), cache.get(k()));
   }

   @Test
   protected void testRemoveWithVersion() {
      SyncCache<String, String> cache = ext().syncCache();
      cache.set(k(), v(1));
      CacheEntry<String, String> entry = cache.getEntry(k());
      assertNotNull(entry);
      CacheEntryVersion version = entry.metadata().version();
      assertTrue(cache.remove(k(), version));
      assertNull(cache.get(k()));
   }

   @Test
   protected void testRemoveWithWrongVersion() {
      SyncCache<String, String> cache = ext().syncCache();
      cache.set(k(), v(1));
      CacheEntry<String, String> entry1 = cache.getEntry(k());
      assertNotNull(entry1);
      CacheEntryVersion oldVersion = entry1.metadata().version();
      cache.set(k(), v(2));
      assertFalse(cache.remove(k(), oldVersion));
      assertEquals(v(2), cache.get(k()));
   }

   @Test
   protected void testTransactionCommit() {
      SyncCache<String, String> cache = ext().syncCache();
      assertNotNull(cache.transactionManager());
      cache.transactionManager().begin();
      cache.set(k(), v());
      cache.transactionManager().commit();
      assertEquals(v(), cache.get(k()));
   }

   @Test
   protected void testTransactionRollback() {
      SyncCache<String, String> cache = ext().syncCache();
      assertNotNull(cache.transactionManager());
      cache.transactionManager().begin();
      cache.set(k(), v());
      cache.transactionManager().rollback();
      assertNull(cache.get(k()));
   }
}
