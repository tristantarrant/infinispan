package org.infinispan.api.async.events.container;

import java.io.Closeable;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.infinispan.api.common.events.container.CacheStartEvent;
import org.infinispan.api.common.events.container.CacheStopEvent;
import org.infinispan.api.common.events.container.ViewChangeEvent;

/**
 * A builder for registering asynchronous container event listeners. Obtained via {@link
 * org.infinispan.api.async.AsyncContainer#listen()}. Call one or more of the {@code onCacheStarted}, {@code
 * onCacheStopped}, {@code onViewChanged} methods to register callbacks for the desired event types, then call {@link
 * #install()} to activate the listener.
 *
 * @since 15.1
 */
public abstract class AsyncContainerListener {
   protected Function<CacheStartEvent, CompletionStage<Void>> onCacheStarted;
   protected Function<CacheStopEvent, CompletionStage<Void>> onCacheStopped;
   protected Function<ViewChangeEvent, CompletionStage<Void>> onViewChanged;

   public AsyncContainerListener onCacheStarted(Function<CacheStartEvent, CompletionStage<Void>> listener) {
      this.onCacheStarted = listener;
      return this;
   }

   public AsyncContainerListener onCacheStopped(Function<CacheStopEvent, CompletionStage<Void>> listener) {
      this.onCacheStopped = listener;
      return this;
   }

   public AsyncContainerListener onViewChanged(Function<ViewChangeEvent, CompletionStage<Void>> listener) {
      this.onViewChanged = listener;
      return this;
   }

   /**
    * Activates the listener and returns a handle that can be used to remove it.
    *
    * @return a {@link CompletionStage} that completes with a {@link Closeable} whose {@code close()} method removes
    * the listener
    */
   public abstract CompletionStage<Closeable> install();
}
