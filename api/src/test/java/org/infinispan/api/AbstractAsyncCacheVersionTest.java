package org.infinispan.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.infinispan.api.async.AsyncCache;
import org.infinispan.api.common.CacheEntry;
import org.infinispan.api.common.CacheEntryVersion;
import org.junit.jupiter.api.Test;

public abstract class AbstractAsyncCacheVersionTest extends AbstractAPITest {

   @Test
   protected void testReplaceWithVersion() {
      AsyncCache<String, String> cache = ext().asyncCache();
      cache.set(k(), v(1)).toCompletableFuture().join();
      CacheEntry<String, String> entry = cache.getEntry(k()).toCompletableFuture().join();
      assertNotNull(entry);
      CacheEntryVersion version = entry.metadata().version();
      assertTrue(cache.replace(k(), v(2), version).toCompletableFuture().join());
      assertEquals(v(2), cache.get(k()).toCompletableFuture().join());
   }

   @Test
   protected void testReplaceWithWrongVersion() {
      AsyncCache<String, String> cache = ext().asyncCache();
      cache.set(k(), v(1)).toCompletableFuture().join();
      CacheEntry<String, String> entry1 = cache.getEntry(k()).toCompletableFuture().join();
      assertNotNull(entry1);
      CacheEntryVersion oldVersion = entry1.metadata().version();
      cache.set(k(), v(2)).toCompletableFuture().join();
      assertFalse(cache.replace(k(), v(3), oldVersion).toCompletableFuture().join());
      assertEquals(v(2), cache.get(k()).toCompletableFuture().join());
   }

   @Test
   protected void testGetOrReplaceEntry() {
      AsyncCache<String, String> cache = ext().asyncCache();
      cache.set(k(), v(1)).toCompletableFuture().join();
      CacheEntry<String, String> entry = cache.getEntry(k()).toCompletableFuture().join();
      assertNotNull(entry);
      CacheEntryVersion version = entry.metadata().version();
      CacheEntry<String, String> previous = cache.getOrReplaceEntry(k(), v(2), version).toCompletableFuture().join();
      assertNotNull(previous);
      assertEquals(v(2), cache.get(k()).toCompletableFuture().join());
   }

   @Test
   protected void testRemoveWithVersion() {
      AsyncCache<String, String> cache = ext().asyncCache();
      cache.set(k(), v(1)).toCompletableFuture().join();
      CacheEntry<String, String> entry = cache.getEntry(k()).toCompletableFuture().join();
      assertNotNull(entry);
      CacheEntryVersion version = entry.metadata().version();
      assertTrue(cache.remove(k(), version).toCompletableFuture().join());
      assertNull(cache.get(k()).toCompletableFuture().join());
   }

   @Test
   protected void testRemoveWithWrongVersion() {
      AsyncCache<String, String> cache = ext().asyncCache();
      cache.set(k(), v(1)).toCompletableFuture().join();
      CacheEntry<String, String> entry1 = cache.getEntry(k()).toCompletableFuture().join();
      assertNotNull(entry1);
      CacheEntryVersion oldVersion = entry1.metadata().version();
      cache.set(k(), v(2)).toCompletableFuture().join();
      assertFalse(cache.remove(k(), oldVersion).toCompletableFuture().join());
      assertEquals(v(2), cache.get(k()).toCompletableFuture().join());
   }
}
