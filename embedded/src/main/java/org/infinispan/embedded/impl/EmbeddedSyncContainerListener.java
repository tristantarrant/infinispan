package org.infinispan.embedded.impl;

import java.io.Closeable;
import java.util.List;

import org.infinispan.api.common.events.container.CacheStopEvent;
import org.infinispan.api.common.events.container.ViewChangeEvent;
import org.infinispan.api.sync.events.container.SyncContainerListener;
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
public class EmbeddedSyncContainerListener extends SyncContainerListener {
   private final DefaultCacheManager cacheManager;

   public EmbeddedSyncContainerListener(DefaultCacheManager cacheManager) {
      this.cacheManager = cacheManager;
   }

   @Override
   public Closeable install() {
      Bridge bridge = new Bridge();
      cacheManager.addListenerAsync(bridge).toCompletableFuture().join();
      return () -> cacheManager.removeListenerAsync(bridge).toCompletableFuture().join();
   }

   @Listener
   class Bridge {
      @CacheStarted
      public void cacheStarted(CacheStartedEvent event) {
         if (onCacheStarted != null) {
            onCacheStarted.accept(event::getCacheName);
         }
      }

      @CacheStopped
      public void cacheStopped(CacheStoppedEvent event) {
         if (onCacheStopped != null) {
            onCacheStopped.accept(new CacheStopEvent() {
            });
         }
      }

      @ViewChanged
      public void viewChanged(ViewChangedEvent event) {
         if (onViewChanged != null) {
            onViewChanged.accept(new ViewChangeEvent() {
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
         }
      }
   }
}
