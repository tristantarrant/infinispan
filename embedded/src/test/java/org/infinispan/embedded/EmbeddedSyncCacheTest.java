package org.infinispan.embedded;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.infinispan.api.AbstractSyncCacheTest;
import org.infinispan.api.common.events.container.CacheStartEvent;
import org.infinispan.api.common.events.container.CacheStopEvent;
import org.infinispan.api.sync.SyncCache;
import org.infinispan.api.sync.SyncContainer;
import org.junit.jupiter.api.Test;

public abstract class EmbeddedSyncCacheTest extends AbstractSyncCacheTest {

   protected abstract EmbeddedInfinispanAPIExtension ext();

   @Test
   void testContainerListenerCacheStarted() throws InterruptedException, IOException {
      SyncContainer container = ext().infinispan().sync();
      BlockingQueue<CacheStartEvent> events = new ArrayBlockingQueue<>(4);
      try (Closeable ignored = container.listen().onCacheStarted(events::add).install()) {
         SyncCache<Object, Object> cache = ext().syncCache();// Trigger cache creation
         CacheStartEvent event = events.poll(5, TimeUnit.SECONDS);
         assertEquals(cache.name(), event.cacheName());
      }
   }

   @Test
   void testContainerListenerCacheStopped() throws InterruptedException, IOException {
      SyncContainer container = ext().infinispan().sync();
      BlockingQueue<CacheStopEvent> events = new ArrayBlockingQueue<>(4);
      SyncCache<Object, Object> cache = ext().syncCache();// Trigger cache creation
      try (Closeable ignored = container.listen().onCacheStopped(events::add).install()) {
         container.caches().remove(cache.name());
         CacheStopEvent event = events.poll(5, TimeUnit.SECONDS);
         assertInstanceOf(CacheStopEvent.class, event);
      }
   }

   @Test
   void testContainerListenerCloseUnregisters() throws InterruptedException, IOException {
      SyncContainer container = ext().infinispan().sync();
      BlockingQueue<CacheStartEvent> events = new ArrayBlockingQueue<>(4);
      Closeable listener = container.listen().onCacheStarted(events::add).install();
      listener.close();
      ext().syncCache(); // Trigger cache creation
      CacheStartEvent event = events.poll(500, TimeUnit.MILLISECONDS);
      assertNull(event, "Listener should not receive events after close");
   }
}
