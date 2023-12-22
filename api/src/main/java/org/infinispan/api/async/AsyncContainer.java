package org.infinispan.api.async;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.infinispan.api.Infinispan;
import org.infinispan.api.async.events.container.AsyncContainerListener;

/**
 * @since 14.0
 **/
public interface AsyncContainer extends Infinispan {

   AsyncCaches caches();

   AsyncMultimaps multimaps();

   AsyncStrongCounters strongCounters();

   AsyncWeakCounters weakCounters();

   AsyncLocks locks();

   AsyncSchemas schemas();

   AsyncContainerListener listen();

   <T> CompletionStage<T> batch(Function<AsyncContainer, CompletionStage<T>> function);
}
