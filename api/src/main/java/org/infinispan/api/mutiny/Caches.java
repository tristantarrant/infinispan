package org.infinispan.api.mutiny;

import org.infinispan.api.configuration.CacheConfiguration;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 *
 * @since 13.0
 **/
public interface Caches {
   <K, V> Uni<Cache<K, V>> create(String name, CacheConfiguration cacheConfiguration);

   <K, V> Uni<Cache<K, V>> create(String name, String template);

   <K, V> Uni<Cache<K, V>> cache(String name);

   Uni<Void> remove(String name);

   Multi<String> names();

   <K, V> Uni<Void> createTemplate(String name, CacheConfiguration cacheConfiguration);

   Uni<Void> removeTemplate(String name);

   Multi<String> templateNames();
}
