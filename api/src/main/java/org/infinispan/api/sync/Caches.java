package org.infinispan.api.sync;

import org.infinispan.api.configuration.CacheConfiguration;

/**
 * @since 13.0
 **/
public interface Caches {
   <K, V> Cache<K, V> get(String name);

   <K, V> Cache<K, V> create(String name, CacheConfiguration cacheConfiguration);

   <K, V> Cache<K, V> create(String name, String template);

   void remove(String name);

   Iterable<String> names();

   void createTemplate(String name, CacheConfiguration cacheConfiguration);

   void removeTemplate(String name);

   Iterable<String> templateNames();
}
