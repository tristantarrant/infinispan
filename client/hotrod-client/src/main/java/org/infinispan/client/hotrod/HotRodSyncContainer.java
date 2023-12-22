package org.infinispan.client.hotrod;

import java.util.function.Function;

import org.infinispan.api.Experimental;
import org.infinispan.api.async.AsyncContainer;
import org.infinispan.api.sync.SyncCaches;
import org.infinispan.api.sync.SyncContainer;
import org.infinispan.api.sync.SyncLocks;
import org.infinispan.api.sync.SyncMultimaps;
import org.infinispan.api.sync.SyncSchemas;
import org.infinispan.api.sync.SyncStrongCounters;
import org.infinispan.api.sync.SyncWeakCounters;
import org.infinispan.api.sync.events.container.SyncContainerListener;

/**
 * @since 14.0
 **/
@Experimental
final class HotRodSyncContainer implements SyncContainer {
   private final HotRod hotrod;

   public HotRodSyncContainer(HotRod hotrod) {
      this.hotrod = hotrod;
   }

   @Override
   public SyncContainer sync() {
      return this;
   }

   @Override
   public AsyncContainer async() {
      return hotrod.async();
   }

   @Override
   public void close() {
      hotrod.close();
   }

   @Override
   public SyncCaches caches() {
      return new HotRodSyncCaches(hotrod);
   }

   @Override
   public SyncMultimaps multimaps() {
      return new HotRodSyncMultimaps(hotrod);
   }

   @Override
   public SyncStrongCounters strongCounters() {
      return new HotRodSyncStrongCounters(hotrod);
   }

   @Override
   public SyncWeakCounters weakCounters() {
      return new HotRodSyncWeakCounters(hotrod);
   }

   @Override
   public SyncLocks locks() {
      return new HotRodSyncLocks(hotrod);
   }

   @Override
   public SyncSchemas schemas() {
      return new HotRodSyncSchemas(hotrod);
   }

   @Override
   public SyncContainerListener listen() {
      return new SyncContainerListener() {
         @Override
         public java.io.Closeable install() {
            throw new UnsupportedOperationException();
         }
      };
   }

   @Override
   public <T> T batch(Function<SyncContainer, T> function) {
      throw new UnsupportedOperationException();
   }
}
