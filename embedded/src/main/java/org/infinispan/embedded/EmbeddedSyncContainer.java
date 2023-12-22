package org.infinispan.embedded;

import static org.infinispan.commons.util.concurrent.CompletableFutures.uncheckedAwait;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.infinispan.api.async.AsyncContainer;
import org.infinispan.api.sync.SyncCaches;
import org.infinispan.api.sync.SyncContainer;
import org.infinispan.api.sync.SyncLocks;
import org.infinispan.api.sync.SyncMultimaps;
import org.infinispan.api.sync.SyncSchemas;
import org.infinispan.api.sync.SyncStrongCounters;
import org.infinispan.api.sync.SyncWeakCounters;
import org.infinispan.api.sync.events.container.SyncContainerListener;
import org.infinispan.embedded.impl.EmbeddedSyncContainerListener;

/**
 * @since 16.3
 */
public class EmbeddedSyncContainer implements SyncContainer {
   private final Embedded embedded;

   EmbeddedSyncContainer(Embedded embedded) {
      this.embedded = embedded;
   }

   @Override
   public SyncContainer sync() {
      return this;
   }

   @Override
   public AsyncContainer async() {
      return embedded.async();
   }

   @Override
   public void close() {
      embedded.close();
   }

   @Override
   public SyncCaches caches() {
      return new EmbeddedSyncCaches(embedded);
   }

   @Override
   public SyncMultimaps multimaps() {
      return new EmbeddedSyncMultimaps(embedded);
   }

   @Override
   public SyncStrongCounters strongCounters() {
      return new EmbeddedSyncStrongCounters(embedded);
   }

   @Override
   public SyncWeakCounters weakCounters() {
      return new EmbeddedSyncWeakCounters(embedded);
   }

   @Override
   public SyncLocks locks() {
      return new EmbeddedSyncLocks(embedded);
   }

   @Override
   public SyncSchemas schemas() {
      return new EmbeddedSyncSchemas(embedded);
   }

   @Override
   public SyncContainerListener listen() {
      return new EmbeddedSyncContainerListener(embedded.cacheManager);
   }

   @Override
   public <T> T batch(Function<SyncContainer, T> function) {
      return uncheckedAwait(CompletableFuture.supplyAsync(() -> function.apply(this), embedded.cacheManager.executor()));
   }
}
