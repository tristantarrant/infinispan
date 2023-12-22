package org.infinispan.api.sync;

import org.infinispan.api.common.CloseableIterable;
import org.infinispan.api.configuration.MultimapConfiguration;

/**
 * @since 14.0
 **/
public interface SyncMultimaps {

   /**
    * Returns an existing multimap with the given name.
    *
    * @param name the multimap name
    * @return the multimap
    */
   <K, V> SyncMultimap<K, V> get(String name);

   /**
    * Creates a multimap with the given name and configuration. If the multimap already exists, it is returned.
    *
    * @param name               the multimap name
    * @param cacheConfiguration the multimap configuration
    * @return the multimap
    */
   <K, V> SyncMultimap<K, V> create(String name, MultimapConfiguration cacheConfiguration);

   /**
    * Creates a multimap with the given name using the specified template. If the multimap already exists, it is
    * returned.
    *
    * @param name     the multimap name
    * @param template the template name
    * @return the multimap
    */
   <K, V> SyncMultimap<K, V> create(String name, String template);

   /**
    * Removes the multimap with the given name.
    *
    * @param name the multimap name
    */
   void remove(String name);

   /**
    * Returns the names of all available multimaps.
    *
    * @return an iterable of multimap names
    */
   CloseableIterable<String> names();

   /**
    * Creates a multimap template with the given name and configuration.
    *
    * @param name               the template name
    * @param cacheConfiguration the template configuration
    */
   void createTemplate(String name, MultimapConfiguration cacheConfiguration);

   /**
    * Removes the multimap template with the given name.
    *
    * @param name the template name
    */
   void removeTemplate(String name);

   /**
    * Returns the names of all available multimap templates.
    *
    * @return an iterable of template names
    */
   CloseableIterable<String> templateNames();
}
