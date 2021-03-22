package org.infinispan.api.sync;

import org.infinispan.api.configuration.CacheConfiguration;

/**
 * @since 14.0
 **/
public interface SyncCaches {
   <K, V> SyncCache<K, V> get(String name);

   <K, V> SyncCache<K, V> create(String name, CacheConfiguration cacheConfiguration);

   <K, V> SyncCache<K, V> create(String name, String template);

   void remove(String name);

   Iterable<String> names();

   void createTemplate(String name, CacheConfiguration cacheConfiguration);

   void removeTemplate(String name);

   Iterable<String> templateNames();
}
