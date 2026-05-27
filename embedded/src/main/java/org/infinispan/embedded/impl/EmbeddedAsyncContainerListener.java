package org.infinispan.embedded.impl;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.api.async.events.container.AsyncContainerListener;
import org.infinispan.api.common.events.container.CacheStartEvent;
import org.infinispan.api.common.events.container.CacheStopEvent;
import org.infinispan.api.common.events.container.ViewChangeEvent;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;

/**
 * @since 16.3
 */
public class EmbeddedAsyncContainerListener extends AsyncContainerListener {
   private final DefaultCacheManager cacheManager;

   public EmbeddedAsyncContainerListener(DefaultCacheManager cacheManager) {
      this.cacheManager = cacheManager;
   }

   @Override
   public CompletionStage<Closeable> install() {
      Bridge bridge = new Bridge();
      return cacheManager.addListenerAsync(bridge)
            .thenApply(v -> () -> cacheManager.removeListenerAsync(bridge).toCompletableFuture().join());
   }

   @Listener
   class Bridge {
      @CacheStarted
      public void cacheStarted(CacheStartedEvent event) {
         if (onCacheStarted != null) {
            CompletionStage<Void> stage = onCacheStarted.apply((CacheStartEvent) event::getCacheName);
            if (stage != null) {
               stage.toCompletableFuture().join();
            }
         }
      }

      @CacheStopped
      public void cacheStopped(CacheStoppedEvent event) {
         if (onCacheStopped != null) {
            CompletionStage<Void> stage = onCacheStopped.apply(new CacheStopEvent() {
            });
            if (stage != null) {
               stage.toCompletableFuture().join();
            }
         }
      }

      @ViewChanged
      public void viewChanged(ViewChangedEvent event) {
         if (onViewChanged != null) {
            CompletionStage<Void> stage = onViewChanged.apply(new ViewChangeEvent() {
               @Override
               public List<org.infinispan.api.common.events.container.Address> newMembers() {
                  return EmbeddedContainerEvents.wrapAddresses(event.getNewMembers());
               }

               @Override
               public List<org.infinispan.api.common.events.container.Address> oldMembers() {
                  return EmbeddedContainerEvents.wrapAddresses(event.getOldMembers());
               }

               @Override
               public org.infinispan.api.common.events.container.Address localAddress() {
                  return EmbeddedContainerEvents.wrapAddress(event.getLocalAddress());
               }

               @Override
               public int viewId() {
                  return event.getViewId();
               }

               @Override
               public boolean isMergeView() {
                  return event.isMergeView();
               }
            });
            if (stage != null) {
               stage.toCompletableFuture().join();
            }
         }
      }
   }
}
