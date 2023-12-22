package org.infinispan.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.infinispan.api.common.CacheEntry;
import org.infinispan.api.sync.SyncCache;
import org.junit.jupiter.api.Test;

public abstract class AbstractSyncCacheTest extends AbstractAPITest {

   @Test
   protected void testName() {
      SyncCache<String, String> cache = ext().syncCache();
      assertNotNull(cache.name());
   }

   @Test
   protected void testContainer() {
      SyncCache<String, String> cache = ext().syncCache();
      assertNotNull(cache.container());
   }

   @Test
   protected void testPutAndGet() {
      SyncCache<String, String> cache = ext().syncCache();
      cache.set(k(), v());
      assertEquals(v(), cache.get(k()));
   }

   @Test
   protected void testGetNonExistent() {
      SyncCache<String, String> cache = ext().syncCache();
      assertNull(cache.get(k()));
   }

   @Test
   protected void testGetEntry() {
      SyncCache<String, String> cache = ext().syncCache();
      cache.set(k(), v());
      CacheEntry<String, String> entry = cache.getEntry(k());
      assertNotNull(entry);
      assertEquals(k(), entry.key());
      assertEquals(v(), entry.value());
   }

   @Test
   protected void testGetEntryNonExistent() {
      SyncCache<String, String> cache = ext().syncCache();
      assertNull(cache.getEntry(k()));
   }

   @Test
   protected void testPutReturnsOldEntry() {
      SyncCache<String, String> cache = ext().syncCache();
      assertNull(cache.put(k(), v(1)));
      CacheEntry<String, String> old = cache.put(k(), v(2));
      assertNotNull(old);
      assertEquals(v(1), old.value());
      assertEquals(v(2), cache.get(k()));
   }

   @Test
   protected void testPutIfAbsent() {
      SyncCache<String, String> cache = ext().syncCache();
      assertNull(cache.putIfAbsent(k(), v(1)));
      CacheEntry<String, String> existing = cache.putIfAbsent(k(), v(2));
      assertNotNull(existing);
      assertEquals(v(1), existing.value());
      assertEquals(v(1), cache.get(k()));
   }

   @Test
   protected void testSetIfAbsent() {
      SyncCache<String, String> cache = ext().syncCache();
      assertTrue(cache.setIfAbsent(k(), v(1)));
      assertFalse(cache.setIfAbsent(k(), v(2)));
      assertEquals(v(1), cache.get(k()));
   }

   @Test
   protected void testRemove() {
      SyncCache<String, String> cache = ext().syncCache();
      cache.set(k(), v());
      assertTrue(cache.remove(k()));
      assertNull(cache.get(k()));
   }

   @Test
   protected void testRemoveNonExistent() {
      SyncCache<String, String> cache = ext().syncCache();
      assertFalse(cache.remove(k()));
   }

   @Test
   protected void testGetAndRemove() {
      SyncCache<String, String> cache = ext().syncCache();
      cache.set(k(), v());
      CacheEntry<String, String> removed = cache.getAndRemove(k());
      assertNotNull(removed);
      assertEquals(v(), removed.value());
      assertNull(cache.get(k()));
   }

   @Test
   protected void testGetAndRemoveNonExistent() {
      SyncCache<String, String> cache = ext().syncCache();
      assertNull(cache.getAndRemove(k()));
   }

   @Test
   protected void testKeys() {
      SyncCache<String, String> cache = ext().syncCache();
      cache.set(k(1), v(1));
      cache.set(k(2), v(2));
      cache.set(k(3), v(3));
      Set<String> keys = new HashSet<>();
      for (String key : cache.keys()) {
         keys.add(key);
      }
      assertEquals(Set.of(k(1), k(2), k(3)), keys);
   }

   @Test
   protected void testEntries() {
      SyncCache<String, String> cache = ext().syncCache();
      cache.set(k(1), v(1));
      cache.set(k(2), v(2));
      List<CacheEntry<String, String>> entries = new ArrayList<>();
      for (CacheEntry<String, String> entry : cache.entries()) {
         entries.add(entry);
      }
      assertEquals(2, entries.size());
      Set<String> keys = new HashSet<>();
      for (CacheEntry<String, String> entry : entries) {
         keys.add(entry.key());
      }
      assertEquals(Set.of(k(1), k(2)), keys);
   }

   @Test
   protected void testPutAll() {
      SyncCache<String, String> cache = ext().syncCache();
      cache.putAll(Map.of(k(1), v(1), k(2), v(2), k(3), v(3)));
      assertEquals(v(1), cache.get(k(1)));
      assertEquals(v(2), cache.get(k(2)));
      assertEquals(v(3), cache.get(k(3)));
   }

   @Test
   protected void testGetAllWithSet() {
      SyncCache<String, String> cache = ext().syncCache();
      cache.set(k(1), v(1));
      cache.set(k(2), v(2));
      cache.set(k(3), v(3));
      Map<String, String> result = cache.getAll(Set.of(k(1), k(3)));
      assertEquals(2, result.size());
      assertEquals(v(1), result.get(k(1)));
      assertEquals(v(3), result.get(k(3)));
   }

   @Test
   protected void testGetAllWithVarargs() {
      SyncCache<String, String> cache = ext().syncCache();
      cache.set(k(1), v(1));
      cache.set(k(2), v(2));
      Map<String, String> result = cache.getAll(k(1), k(2));
      assertEquals(2, result.size());
      assertEquals(v(1), result.get(k(1)));
      assertEquals(v(2), result.get(k(2)));
   }

   @Test
   protected void testRemoveAll() {
      SyncCache<String, String> cache = ext().syncCache();
      cache.set(k(1), v(1));
      cache.set(k(2), v(2));
      cache.set(k(3), v(3));
      Set<String> removed = cache.removeAll(Set.of(k(1), k(3)));
      assertTrue(removed.contains(k(1)));
      assertTrue(removed.contains(k(3)));
      assertNull(cache.get(k(1)));
      assertEquals(v(2), cache.get(k(2)));
      assertNull(cache.get(k(3)));
   }

   @Test
   protected void testGetAndRemoveAll() {
      SyncCache<String, String> cache = ext().syncCache();
      cache.set(k(1), v(1));
      cache.set(k(2), v(2));
      Map<String, CacheEntry<String, String>> removed = cache.getAndRemoveAll(Set.of(k(1), k(2)));
      assertEquals(2, removed.size());
      assertEquals(v(1), removed.get(k(1)).value());
      assertEquals(v(2), removed.get(k(2)).value());
      assertNull(cache.get(k(1)));
      assertNull(cache.get(k(2)));
   }

   @Test
   protected void testEstimateSize() {
      SyncCache<String, String> cache = ext().syncCache();
      assertEquals(0, cache.estimateSize());
      cache.set(k(1), v(1));
      cache.set(k(2), v(2));
      assertEquals(2, cache.estimateSize());
   }

   @Test
   protected void testClear() {
      SyncCache<String, String> cache = ext().syncCache();
      cache.set(k(1), v(1));
      cache.set(k(2), v(2));
      cache.clear();
      assertEquals(0, cache.estimateSize());
      assertNull(cache.get(k(1)));
   }
}
