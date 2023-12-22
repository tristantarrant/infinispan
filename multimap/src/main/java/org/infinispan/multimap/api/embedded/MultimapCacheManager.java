package org.infinispan.multimap.api.embedded;

import org.infinispan.commons.util.Experimental;
import org.infinispan.configuration.cache.Configuration;

@Experimental
public interface MultimapCacheManager {

   /**
    * Defines a named multimap cache's configuration by using the provided configuration
    * If this cache was already configured, either declaratively or programmatically, this method will throw a
    * {@link org.infinispan.commons.CacheConfigurationException}.
    * Currently, the MultimapCache with the given name "foo" can be also accessed as a regular cache named "foo".
    *
    * @param name          name of multimap cache whose configuration is being defined
    * @param configuration configuration overrides to use
    * @return a cloned configuration instance
    */
   Configuration defineConfiguration(String name, Configuration configuration);

   /**
    * Retrieves a named multimap cache from the system.
    *
    * @param name, name of multimap cache to retrieve
    * @return null if no configuration exists as per rules set above, otherwise returns a multimap cache instance
    * identified by cacheName and doesn't support duplicates
    */
   default <K, V> MultimapCache<K, V> get(String name) {
      return get(name, false);
   }

   /**
    * Retrieves a named multimap cache from the system.
    *
    * @param name, name of multimap cache to retrieve
    * @param supportsDuplicates, boolean check to see whether duplicates are supported or not
    * @return null if no configuration exists as per rules set above, otherwise returns a multimap cache instance
    * identified by cacheName
    */
   <K,V> MultimapCache<K, V> get(String name, boolean supportsDuplicates);
}
