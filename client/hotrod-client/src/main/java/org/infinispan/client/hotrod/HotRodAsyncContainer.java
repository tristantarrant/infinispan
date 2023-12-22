package org.infinispan.client.hotrod;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.infinispan.api.Experimental;
import org.infinispan.api.async.AsyncCaches;
import org.infinispan.api.async.AsyncContainer;
import org.infinispan.api.async.AsyncLocks;
import org.infinispan.api.async.AsyncMultimaps;
import org.infinispan.api.async.AsyncSchemas;
import org.infinispan.api.async.AsyncStrongCounters;
import org.infinispan.api.async.AsyncWeakCounters;
import org.infinispan.api.async.events.container.AsyncContainerListener;
import org.infinispan.api.sync.SyncContainer;

/**
 * @since 14.0
 **/
@Experimental
final class HotRodAsyncContainer implements AsyncContainer {
   private final HotRod hotrod;

   HotRodAsyncContainer(HotRod hotrod) {
      this.hotrod = hotrod;
   }

   @Override
   public SyncContainer sync() {
      return hotrod.sync();
   }

   @Override
   public AsyncContainer async() {
      return this;
   }

   @Override
   public void close() {
      hotrod.close();
   }

   @Override
   public AsyncCaches caches() {
      return new HotRodAsyncCaches(hotrod);
   }

   @Override
   public AsyncMultimaps multimaps() {
      return new HotRodAsyncMultimaps(hotrod);
   }

   @Override
   public AsyncStrongCounters strongCounters() {
      return new HotRodAsyncStrongCounters(hotrod);
   }

   @Override
   public AsyncWeakCounters weakCounters() {
      return new HotRodAsyncWeakCounters(hotrod);
   }

   @Override
   public AsyncLocks locks() {
      return new HotRodAsyncLocks(hotrod);
   }

   @Override
   public AsyncSchemas schemas() {
      return new HotRodAsyncSchemas(hotrod);
   }

   @Override
   public AsyncContainerListener listen() {
      return new AsyncContainerListener() {
         @Override
         public CompletionStage<java.io.Closeable> install() {
            throw new UnsupportedOperationException();
         }
      };
   }

   @Override
   public <T> CompletionStage<T> batch(Function<AsyncContainer, CompletionStage<T>> function) {
      throw new UnsupportedOperationException();
   }
}
