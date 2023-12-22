package org.infinispan.api.async;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import org.infinispan.api.configuration.MultimapConfiguration;

/**
 * @param <K> the type of keys maintained by this multimap
 * @param <V> the type of mapped values
 * @since 14.0
 **/
public interface AsyncMultimap<K, V> {

   /**
    * Returns the name of this multimap.
    *
    * @return the name
    */
   String name();

   /**
    * Returns the configuration of this multimap.
    *
    * @return a {@link CompletionStage} completing with the configuration
    */
   CompletionStage<MultimapConfiguration> configuration();

   /**
    * Returns the container of this multimap.
    *
    * @return the container
    */
   AsyncContainer container();

   /**
    * Adds a value to the collection associated with the given key. The entry is created if it doesn't exist.
    *
    * @param key   the key
    * @param value the value to add
    * @return a {@link CompletionStage} that completes when the value is added
    */
   CompletionStage<Void> add(K key, V value);

   /**
    * Returns all values associated with the given key.
    *
    * @param key the key
    * @return a publisher of values
    */
   Flow.Publisher<V> get(K key);

   /**
    * Removes the entry and all its values for the given key.
    *
    * @param key the key
    * @return a {@link CompletionStage} completing with {@code true} if the entry existed and was removed
    */
   CompletionStage<Boolean> remove(K key);

   /**
    * Removes a specific value from the collection associated with the given key.
    *
    * @param key   the key
    * @param value the value to remove
    * @return a {@link CompletionStage} completing with {@code true} if the value was found and removed
    */
   CompletionStage<Boolean> remove(K key, V value);

   /**
    * Returns whether this multimap contains an entry for the given key.
    *
    * @param key the key
    * @return a {@link CompletionStage} completing with {@code true} if the key exists
    */
   CompletionStage<Boolean> containsKey(K key);

   /**
    * Returns whether this multimap contains the given key-value pair.
    *
    * @param key   the key
    * @param value the value
    * @return a {@link CompletionStage} completing with {@code true} if the key-value pair exists
    */
   CompletionStage<Boolean> containsEntry(K key, V value);

   /**
    * Returns an estimate of the number of entries in this multimap.
    *
    * @return a {@link CompletionStage} completing with the estimated size
    */
   CompletionStage<Long> estimateSize();
}
