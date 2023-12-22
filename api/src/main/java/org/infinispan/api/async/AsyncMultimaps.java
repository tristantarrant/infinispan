package org.infinispan.api.async;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import org.infinispan.api.configuration.MultimapConfiguration;

/**
 * @since 14.0
 **/
public interface AsyncMultimaps {

   /**
    * Creates a multimap with the given name and configuration. If the multimap already exists, it is returned.
    *
    * @param name               the multimap name
    * @param cacheConfiguration the multimap configuration
    * @return a {@link CompletionStage} completing with the multimap
    */
   <K, V> CompletionStage<AsyncMultimap<K, V>> create(String name, MultimapConfiguration cacheConfiguration);

   /**
    * Creates a multimap with the given name using the specified template. If the multimap already exists, it is
    * returned.
    *
    * @param name     the multimap name
    * @param template the template name
    * @return a {@link CompletionStage} completing with the multimap
    */
   <K, V> CompletionStage<AsyncMultimap<K, V>> create(String name, String template);

   /**
    * Returns an existing multimap with the given name.
    *
    * @param name the multimap name
    * @return a {@link CompletionStage} completing with the multimap
    */
   <K, V> CompletionStage<AsyncMultimap<K, V>> get(String name);

   /**
    * Removes the multimap with the given name.
    *
    * @param name the multimap name
    * @return a {@link CompletionStage} that completes when the multimap is removed
    */
   CompletionStage<Void> remove(String name);

   /**
    * Returns the names of all available multimaps.
    *
    * @return a publisher of multimap names
    */
   Flow.Publisher<String> names();

   /**
    * Creates a multimap template with the given name and configuration.
    *
    * @param name               the template name
    * @param cacheConfiguration the template configuration
    * @return a {@link CompletionStage} that completes when the template is created
    */
   CompletionStage<Void> createTemplate(String name, MultimapConfiguration cacheConfiguration);

   /**
    * Removes the multimap template with the given name.
    *
    * @param name the template name
    * @return a {@link CompletionStage} that completes when the template is removed
    */
   CompletionStage<Void> removeTemplate(String name);

   /**
    * Returns the names of all available multimap templates.
    *
    * @return a publisher of template names
    */
   Flow.Publisher<String> templateNames();
}
