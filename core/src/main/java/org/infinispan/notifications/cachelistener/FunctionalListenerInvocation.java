package org.infinispan.notifications.cachelistener;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.infinispan.notifications.cachelistener.event.Event;
import org.infinispan.notifications.impl.ListenerInvocation;

/**
 * A {@link ListenerInvocation} that calls a lambda directly, bypassing reflection.
 *
 * @since 15.1
 */
public final class FunctionalListenerInvocation<K, V> implements ListenerInvocation<Event<K, V>> {
   private final Object target;
   private final Function<Event<K, V>, CompletionStage<Void>> callback;

   private FunctionalListenerInvocation(Object target, Function<Event<K, V>, CompletionStage<Void>> callback) {
      this.target = target;
      this.callback = callback;
   }

   public static <K, V> FunctionalListenerInvocation<K, V> sync(Object target, Consumer<Event<K, V>> consumer) {
      return new FunctionalListenerInvocation<>(target, event -> {
         try {
            consumer.accept(event);
            return CompletableFutures.completedNull();
         } catch (Throwable t) {
            throw new CacheException(t);
         }
      });
   }

   public static <K, V> FunctionalListenerInvocation<K, V> async(Object target, Function<Event<K, V>, CompletionStage<Void>> function) {
      return new FunctionalListenerInvocation<>(target, function);
   }

   @Override
   public CompletionStage<Void> invoke(Event<K, V> event) {
      return callback.apply(event);
   }

   @Override
   public Object getTarget() {
      return target;
   }
}
