package org.infinispan.api.async;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import org.infinispan.api.configuration.CacheConfiguration;

/**
 * @since 13.0
 **/
public interface Caches {
   <K, V> CompletionStage<Cache<K, V>> create(String name, CacheConfiguration cacheConfiguration);

   <K, V> CompletionStage<Cache<K, V>> create(String name, String template);

   <K, V> CompletionStage<Cache<K, V>> get(String name);

   CompletionStage<Void> remove(String name);

   Flow.Publisher<String> names();

   <K, V> CompletionStage<Void> createTemplate(String name, CacheConfiguration cacheConfiguration);

   CompletionStage<Void> removeTemplate(String name);

   Flow.Publisher<String> templateNames();
}
