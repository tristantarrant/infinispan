package org.infinispan.api.sync;

import org.infinispan.api.configuration.MultiMapConfiguration;

/**
 * @since 13.0
 **/
public interface MultiMaps {
   <K, V> Cache<K, V> get(String name);

   <K, V> Cache<K, V> create(String name, MultiMapConfiguration cacheConfiguration);

   <K, V> Cache<K, V> create(String name, String template);

   void remove(String name);

   Iterable<String> names();

   void createTemplate(String name, MultiMapConfiguration cacheConfiguration);

   void removeTemplate(String name);

   Iterable<String> templateNames();
}
