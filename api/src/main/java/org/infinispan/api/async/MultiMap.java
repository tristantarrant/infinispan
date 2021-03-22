package org.infinispan.api.async;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import org.infinispan.api.configuration.MultiMapConfiguration;

public interface MultiMap<K, V> {
   String name();

   CompletionStage<MultiMapConfiguration> configuration();

   CompletionStage<Void> add(K key, V value);

   Flow.Publisher<V> get(K key);

   CompletionStage<Boolean> remove(K key);

   CompletionStage<Boolean> remove(K key, V value);

   CompletionStage<Boolean> containsKey(K key);

   CompletionStage<Boolean> containsValue(V value);

   CompletionStage<Boolean> containsEntry(K key, V value);

   CompletionStage<Long> estimateSize();
}
