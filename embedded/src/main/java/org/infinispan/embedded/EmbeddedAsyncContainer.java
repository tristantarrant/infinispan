package org.infinispan.embedded;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.infinispan.api.async.AsyncCaches;
import org.infinispan.api.async.AsyncContainer;
import org.infinispan.api.async.AsyncLocks;
import org.infinispan.api.async.AsyncMultimaps;
import org.infinispan.api.async.AsyncSchemas;
import org.infinispan.api.async.AsyncStrongCounters;
import org.infinispan.api.async.AsyncWeakCounters;
import org.infinispan.api.async.events.container.AsyncContainerListener;
import org.infinispan.api.sync.SyncContainer;
import org.infinispan.embedded.impl.EmbeddedAsyncContainerListener;

/**
 * @since 16.3
 */
public class EmbeddedAsyncContainer implements AsyncContainer {
   private final Embedded embedded;

   EmbeddedAsyncContainer(Embedded embedded) {
      this.embedded = embedded;
   }

   @Override
   public SyncContainer sync() {
      return embedded.sync();
   }

   @Override
   public AsyncContainer async() {
      return this;
   }

   @Override
   public void close() {
      embedded.close();
   }

   @Override
   public AsyncCaches caches() {
      return new EmbeddedAsyncCaches(embedded);
   }

   @Override
   public AsyncMultimaps multimaps() {
      return new EmbeddedAsyncMultimaps(embedded);
   }

   @Override
   public AsyncStrongCounters strongCounters() {
      return new EmbeddedAsyncStrongCounters(embedded);
   }

   @Override
   public AsyncWeakCounters weakCounters() {
      return new EmbeddedAsyncWeakCounters(embedded);
   }

   @Override
   public AsyncLocks locks() {
      return new EmbeddedAsyncLocks(embedded);
   }

   @Override
   public AsyncSchemas schemas() {
      return new EmbeddedAsyncSchemas(embedded);
   }

   @Override
   public AsyncContainerListener listen() {
      return new EmbeddedAsyncContainerListener(embedded.cacheManager);
   }

   @Override
   public <T> CompletionStage<T> batch(Function<AsyncContainer, CompletionStage<T>> function) {
      return CompletableFuture.supplyAsync(() -> function.apply(this), embedded.cacheManager.executor())
            .thenCompose(Function.identity());
   }
}
