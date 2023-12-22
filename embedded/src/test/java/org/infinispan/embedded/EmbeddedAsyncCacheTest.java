package org.infinispan.embedded;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.Closeable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.infinispan.api.AbstractAsyncCacheTest;
import org.infinispan.api.async.AsyncCache;
import org.infinispan.api.async.AsyncContainer;
import org.infinispan.api.common.events.container.CacheStartEvent;
import org.infinispan.api.common.events.container.CacheStopEvent;
import org.junit.jupiter.api.Test;

public abstract class EmbeddedAsyncCacheTest extends AbstractAsyncCacheTest {

   protected abstract EmbeddedInfinispanAPIExtension ext();

   @Test
   void testContainerListenerCacheStarted() throws Exception {
      AsyncContainer container = ext().infinispan().async();
      BlockingQueue<CacheStartEvent> events = new ArrayBlockingQueue<>(4);
      try (Closeable listener = container.listen()
            .onCacheStarted(e -> {
               events.add(e);
               return CompletableFuture.completedFuture(null);
            })
            .install().toCompletableFuture().join()) {
         AsyncCache<Object, Object> cache = ext().asyncCache();
         CacheStartEvent event = events.poll(5, TimeUnit.SECONDS);
         assertEquals(cache.name(), event.cacheName());
      }
   }

   @Test
   void testContainerListenerCacheStopped() throws Exception {
      AsyncContainer container = ext().infinispan().async();
      BlockingQueue<CacheStopEvent> events = new ArrayBlockingQueue<>(4);
      AsyncCache<Object, Object> cache = ext().asyncCache();
      try (Closeable listener = container.listen()
            .onCacheStopped(e -> {
               events.add(e);
               return CompletableFuture.completedFuture(null);
            })
            .install().toCompletableFuture().join()) {
         container.caches().remove(cache.name()).toCompletableFuture().join();
         CacheStopEvent event = events.poll(5, TimeUnit.SECONDS);
         assertInstanceOf(CacheStopEvent.class, event);
      }
   }

   @Test
   void testContainerListenerCloseUnregisters() throws Exception {
      AsyncContainer container = ext().infinispan().async();
      BlockingQueue<CacheStartEvent> events = new ArrayBlockingQueue<>(4);
      Closeable listener = container.listen()
            .onCacheStarted(e -> {
               events.add(e);
               return CompletableFuture.completedFuture(null);
            })
            .install().toCompletableFuture().join();
      listener.close();
      ext().asyncCache();
      CacheStartEvent event = events.poll(500, TimeUnit.MILLISECONDS);
      assertNull(event, "Listener should not receive events after close");
   }
}
