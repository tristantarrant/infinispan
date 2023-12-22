package org.infinispan.api.async.events.cache;

import java.io.Closeable;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A builder for registering asynchronous continuous query listeners. Obtained via {@link
 * org.infinispan.api.async.AsyncQuery#findContinuously()}. Call one or more of the {@code onJoin}, {@code onUpdate},
 * {@code onLeave} methods to register callbacks, then call {@link #install()} to activate the listener.
 *
 * <pre>{@code
 * CompletionStage<Closeable> handle = cache.<String, Book, Book>query("FROM Book WHERE price > 10")
 *    .findContinuously()
 *    .onJoin((key, book) -> CompletableFuture.runAsync(() -> log("New match: " + book.getTitle())))
 *    .onLeave(key -> CompletableFuture.runAsync(() -> log("Left: " + key)))
 *    .install();
 * }</pre>
 *
 * @param <K> key type
 * @param <R> query result type (entity or projection)
 * @since 15.1
 */
public abstract class AsyncCacheContinuousQueryListener<K, R> {
   protected BiFunction<K, R, CompletionStage<Void>> onJoin;
   protected BiFunction<K, R, CompletionStage<Void>> onUpdate;
   protected Function<K, CompletionStage<Void>> onLeave;

   public AsyncCacheContinuousQueryListener<K, R> onJoin(BiFunction<K, R, CompletionStage<Void>> listener) {
      this.onJoin = listener;
      return this;
   }

   public AsyncCacheContinuousQueryListener<K, R> onUpdate(BiFunction<K, R, CompletionStage<Void>> listener) {
      this.onUpdate = listener;
      return this;
   }

   public AsyncCacheContinuousQueryListener<K, R> onLeave(Function<K, CompletionStage<Void>> listener) {
      this.onLeave = listener;
      return this;
   }

   /**
    * Activates the continuous query listener and returns a handle that can be used to remove it.
    *
    * @return a {@link CompletionStage} that completes with a {@link Closeable} whose {@code close()} method removes
    * the listener
    */
   public abstract CompletionStage<Closeable> install();
}
