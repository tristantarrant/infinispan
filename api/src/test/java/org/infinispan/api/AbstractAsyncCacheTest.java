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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;

import org.infinispan.api.async.AsyncCache;
import org.infinispan.api.common.CacheEntry;
import org.junit.jupiter.api.Test;

public abstract class AbstractAsyncCacheTest extends AbstractAPITest {

   @Test
   protected void testName() {
      AsyncCache<String, String> cache = ext().asyncCache();
      assertNotNull(cache.name());
   }

   @Test
   protected void testContainer() {
      AsyncCache<String, String> cache = ext().asyncCache();
      assertNotNull(cache.container());
   }

   @Test
   protected void testPutAndGet() {
      AsyncCache<String, String> cache = ext().asyncCache();
      cache.set(k(), v()).toCompletableFuture().join();
      assertEquals(v(), cache.get(k()).toCompletableFuture().join());
   }

   @Test
   protected void testGetNonExistent() {
      AsyncCache<String, String> cache = ext().asyncCache();
      assertNull(cache.get(k()).toCompletableFuture().join());
   }

   @Test
   protected void testGetEntry() {
      AsyncCache<String, String> cache = ext().asyncCache();
      cache.set(k(), v()).toCompletableFuture().join();
      CacheEntry<String, String> entry = cache.getEntry(k()).toCompletableFuture().join();
      assertNotNull(entry);
      assertEquals(k(), entry.key());
      assertEquals(v(), entry.value());
   }

   @Test
   protected void testGetEntryNonExistent() {
      AsyncCache<String, String> cache = ext().asyncCache();
      assertNull(cache.getEntry(k()).toCompletableFuture().join());
   }

   @Test
   protected void testPutReturnsOldEntry() {
      AsyncCache<String, String> cache = ext().asyncCache();
      assertNull(cache.put(k(), v(1)).toCompletableFuture().join());
      CacheEntry<String, String> old = cache.put(k(), v(2)).toCompletableFuture().join();
      assertNotNull(old);
      assertEquals(v(1), old.value());
      assertEquals(v(2), cache.get(k()).toCompletableFuture().join());
   }

   @Test
   protected void testPutIfAbsent() {
      AsyncCache<String, String> cache = ext().asyncCache();
      assertNull(cache.putIfAbsent(k(), v(1)).toCompletableFuture().join());
      CacheEntry<String, String> existing = cache.putIfAbsent(k(), v(2)).toCompletableFuture().join();
      assertNotNull(existing);
      assertEquals(v(1), existing.value());
      assertEquals(v(1), cache.get(k()).toCompletableFuture().join());
   }

   @Test
   protected void testSetIfAbsent() {
      AsyncCache<String, String> cache = ext().asyncCache();
      assertTrue(cache.setIfAbsent(k(), v(1)).toCompletableFuture().join());
      assertFalse(cache.setIfAbsent(k(), v(2)).toCompletableFuture().join());
      assertEquals(v(1), cache.get(k()).toCompletableFuture().join());
   }

   @Test
   protected void testRemove() {
      AsyncCache<String, String> cache = ext().asyncCache();
      cache.set(k(), v()).toCompletableFuture().join();
      assertTrue(cache.remove(k()).toCompletableFuture().join());
      assertNull(cache.get(k()).toCompletableFuture().join());
   }

   @Test
   protected void testRemoveNonExistent() {
      AsyncCache<String, String> cache = ext().asyncCache();
      assertFalse(cache.remove(k()).toCompletableFuture().join());
   }

   @Test
   protected void testGetAndRemove() {
      AsyncCache<String, String> cache = ext().asyncCache();
      cache.set(k(), v()).toCompletableFuture().join();
      CacheEntry<String, String> removed = cache.getAndRemove(k()).toCompletableFuture().join();
      assertNotNull(removed);
      assertEquals(v(), removed.value());
      assertNull(cache.get(k()).toCompletableFuture().join());
   }

   @Test
   protected void testGetAndRemoveNonExistent() {
      AsyncCache<String, String> cache = ext().asyncCache();
      assertNull(cache.getAndRemove(k()).toCompletableFuture().join());
   }

   @Test
   protected void testKeys() {
      AsyncCache<String, String> cache = ext().asyncCache();
      cache.set(k(1), v(1)).toCompletableFuture().join();
      cache.set(k(2), v(2)).toCompletableFuture().join();
      cache.set(k(3), v(3)).toCompletableFuture().join();
      Set<String> keys = new HashSet<>(collect(cache.keys()));
      assertEquals(Set.of(k(1), k(2), k(3)), keys);
   }

   @Test
   protected void testEntries() {
      AsyncCache<String, String> cache = ext().asyncCache();
      cache.set(k(1), v(1)).toCompletableFuture().join();
      cache.set(k(2), v(2)).toCompletableFuture().join();
      List<CacheEntry<String, String>> entries = collect(cache.entries());
      assertEquals(2, entries.size());
      Set<String> keys = entries.stream().map(CacheEntry::key).collect(Collectors.toSet());
      assertEquals(Set.of(k(1), k(2)), keys);
   }

   @Test
   protected void testPutAll() {
      AsyncCache<String, String> cache = ext().asyncCache();
      cache.putAll(Map.of(k(1), v(1), k(2), v(2), k(3), v(3))).toCompletableFuture().join();
      assertEquals(v(1), cache.get(k(1)).toCompletableFuture().join());
      assertEquals(v(2), cache.get(k(2)).toCompletableFuture().join());
      assertEquals(v(3), cache.get(k(3)).toCompletableFuture().join());
   }

   @Test
   protected void testGetAll() {
      AsyncCache<String, String> cache = ext().asyncCache();
      cache.set(k(1), v(1)).toCompletableFuture().join();
      cache.set(k(2), v(2)).toCompletableFuture().join();
      cache.set(k(3), v(3)).toCompletableFuture().join();
      List<CacheEntry<String, String>> entries = collect(cache.getAll(Set.of(k(1), k(3))));
      assertEquals(2, entries.size());
      Map<String, String> result = entries.stream().collect(Collectors.toMap(CacheEntry::key, CacheEntry::value));
      assertEquals(v(1), result.get(k(1)));
      assertEquals(v(3), result.get(k(3)));
   }

   @Test
   protected void testGetAllWithVarargs() {
      AsyncCache<String, String> cache = ext().asyncCache();
      cache.set(k(1), v(1)).toCompletableFuture().join();
      cache.set(k(2), v(2)).toCompletableFuture().join();
      List<CacheEntry<String, String>> entries = collect(cache.getAll(k(1), k(2)));
      assertEquals(2, entries.size());
      Map<String, String> result = entries.stream().collect(Collectors.toMap(CacheEntry::key, CacheEntry::value));
      assertEquals(v(1), result.get(k(1)));
      assertEquals(v(2), result.get(k(2)));
   }

   @Test
   protected void testRemoveAll() {
      AsyncCache<String, String> cache = ext().asyncCache();
      cache.set(k(1), v(1)).toCompletableFuture().join();
      cache.set(k(2), v(2)).toCompletableFuture().join();
      cache.set(k(3), v(3)).toCompletableFuture().join();
      Set<String> removed = new HashSet<>(collect(cache.removeAll(Set.of(k(1), k(3)))));
      assertTrue(removed.contains(k(1)));
      assertTrue(removed.contains(k(3)));
      assertNull(cache.get(k(1)).toCompletableFuture().join());
      assertEquals(v(2), cache.get(k(2)).toCompletableFuture().join());
      assertNull(cache.get(k(3)).toCompletableFuture().join());
   }

   @Test
   protected void testGetAndRemoveAll() {
      AsyncCache<String, String> cache = ext().asyncCache();
      cache.set(k(1), v(1)).toCompletableFuture().join();
      cache.set(k(2), v(2)).toCompletableFuture().join();
      List<CacheEntry<String, String>> removed = collect(cache.getAndRemoveAll(Set.of(k(1), k(2))));
      assertEquals(2, removed.size());
      Map<String, String> result = removed.stream().collect(Collectors.toMap(CacheEntry::key, CacheEntry::value));
      assertEquals(v(1), result.get(k(1)));
      assertEquals(v(2), result.get(k(2)));
      assertNull(cache.get(k(1)).toCompletableFuture().join());
      assertNull(cache.get(k(2)).toCompletableFuture().join());
   }

   @Test
   protected void testEstimateSize() {
      AsyncCache<String, String> cache = ext().asyncCache();
      assertEquals(0L, cache.estimateSize().toCompletableFuture().join());
      cache.set(k(1), v(1)).toCompletableFuture().join();
      cache.set(k(2), v(2)).toCompletableFuture().join();
      assertEquals(2L, cache.estimateSize().toCompletableFuture().join());
   }

   @Test
   protected void testClear() {
      AsyncCache<String, String> cache = ext().asyncCache();
      cache.set(k(1), v(1)).toCompletableFuture().join();
      cache.set(k(2), v(2)).toCompletableFuture().join();
      cache.clear().toCompletableFuture().join();
      assertEquals(0L, cache.estimateSize().toCompletableFuture().join());
      assertNull(cache.get(k(1)).toCompletableFuture().join());
   }

   protected static <T> List<T> collect(Flow.Publisher<T> publisher) {
      CompletableFuture<List<T>> future = new CompletableFuture<>();
      List<T> items = new ArrayList<>();
      publisher.subscribe(new Flow.Subscriber<>() {
         @Override
         public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
         }

         @Override
         public void onNext(T item) {
            items.add(item);
         }

         @Override
         public void onError(Throwable throwable) {
            future.completeExceptionally(throwable);
         }

         @Override
         public void onComplete() {
            future.complete(items);
         }
      });
      return future.join();
   }
}
