package org.infinispan.api.sync.events.cache;

import java.io.Closeable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A builder for registering continuous query listeners. Obtained via {@link
 * org.infinispan.api.sync.SyncQuery#findContinuously()}. Call one or more of the {@code onJoin}, {@code onUpdate},
 * {@code onLeave} methods to register callbacks, then call {@link #install()} to activate the listener.
 *
 * <pre>{@code
 * Closeable handle = cache.<String, Book, Book>query("FROM Book WHERE price > 10")
 *    .findContinuously()
 *    .onJoin((key, book) -> System.out.println("New match: " + book.getTitle()))
 *    .onLeave(key -> System.out.println("Left: " + key))
 *    .install();
 * }</pre>
 *
 * @param <K> key type
 * @param <R> query result type (entity or projection)
 * @since 15.1
 */
public abstract class SyncCacheContinuousQueryListener<K, R> {
   protected BiConsumer<K, R> onJoin;
   protected BiConsumer<K, R> onUpdate;
   protected Consumer<K> onLeave;

   public SyncCacheContinuousQueryListener<K, R> onJoin(BiConsumer<K, R> listener) {
      this.onJoin = listener;
      return this;
   }

   public SyncCacheContinuousQueryListener<K, R> onUpdate(BiConsumer<K, R> listener) {
      this.onUpdate = listener;
      return this;
   }

   public SyncCacheContinuousQueryListener<K, R> onLeave(Consumer<K> listener) {
      this.onLeave = listener;
      return this;
   }

   /**
    * Activates the continuous query listener and returns a handle that can be used to remove it.
    *
    * @return a {@link Closeable} whose {@code close()} method removes the listener
    */
   public abstract Closeable install();
}
