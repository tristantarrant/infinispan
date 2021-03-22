package org.infinispan.api.async;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import org.infinispan.api.configuration.MultiMapConfiguration;

/**
 * @since 14.0
 **/
public interface AsyncMultiMaps {
   <K, V> CompletionStage<AsyncMultiMap<K, V>> create(String name, MultiMapConfiguration cacheConfiguration);

   <K, V> CompletionStage<AsyncMultiMap<K, V>> create(String name, String template);

   <K, V> CompletionStage<AsyncMultiMap<K, V>> get(String name);

   CompletionStage<Void> remove(String name);

   Flow.Publisher<String> names();

   <K, V> CompletionStage<Void> createTemplate(String name, MultiMapConfiguration cacheConfiguration);

   CompletionStage<Void> removeTemplate(String name);

   Flow.Publisher<String> templateNames();
}
