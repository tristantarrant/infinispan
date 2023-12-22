package org.infinispan.api.sync.events.container;

import java.io.Closeable;
import java.util.function.Consumer;

import org.infinispan.api.common.events.container.CacheStartEvent;
import org.infinispan.api.common.events.container.CacheStopEvent;
import org.infinispan.api.common.events.container.ViewChangeEvent;

/**
 * A builder for registering container event listeners. Obtained via {@link
 * org.infinispan.api.sync.SyncContainer#listen()}. Call one or more of the {@code onCacheStarted}, {@code
 * onCacheStopped}, {@code onViewChanged} methods to register callbacks for the desired event types, then call {@link
 * #install()} to activate the listener.
 *
 * @since 15.1
 */
public abstract class SyncContainerListener {
   protected Consumer<CacheStartEvent> onCacheStarted;
   protected Consumer<CacheStopEvent> onCacheStopped;
   protected Consumer<ViewChangeEvent> onViewChanged;

   public SyncContainerListener onCacheStarted(Consumer<CacheStartEvent> listener) {
      this.onCacheStarted = listener;
      return this;
   }

   public SyncContainerListener onCacheStopped(Consumer<CacheStopEvent> listener) {
      this.onCacheStopped = listener;
      return this;
   }

   public SyncContainerListener onViewChanged(Consumer<ViewChangeEvent> listener) {
      this.onViewChanged = listener;
      return this;
   }

   /**
    * Activates the listener and returns a handle that can be used to remove it.
    *
    * @return a {@link Closeable} whose {@code close()} method removes the listener
    */
   public abstract Closeable install();
}
