package org.infinispan.api.sync;

import org.infinispan.api.common.CloseableIterable;
import org.infinispan.api.configuration.MultimapConfiguration;

/**
 * @param <K> the type of keys maintained by this multimap
 * @param <V> the type of mapped values
 * @since 14.0
 */
public interface SyncMultimap<K, V> {

   /**
    * Returns the name of this multimap.
    *
    * @return the name
    */
   String name();

   /**
    * Returns the configuration of this multimap.
    *
    * @return the configuration
    */
   MultimapConfiguration configuration();

   /**
    * Returns the container of this multimap.
    *
    * @return the container
    */
   SyncContainer container();

   /**
    * Adds a value to the collection associated with the given key. The entry is created if it doesn't exist.
    *
    * @param key   the key
    * @param value the value to add
    */
   void add(K key, V value);

   /**
    * Returns all values associated with the given key.
    *
    * @param key the key
    * @return an iterable of values
    */
   CloseableIterable<V> get(K key);

   /**
    * Removes the entry and all its values for the given key.
    *
    * @param key the key
    * @return {@code true} if the entry existed and was removed
    */
   boolean remove(K key);

   /**
    * Removes a specific value from the collection associated with the given key.
    *
    * @param key   the key
    * @param value the value to remove
    * @return {@code true} if the value was found and removed
    */
   boolean remove(K key, V value);

   /**
    * Returns whether this multimap contains an entry for the given key.
    *
    * @param key the key
    * @return {@code true} if the key exists
    */
   boolean containsKey(K key);

   /**
    * Returns whether this multimap contains the given key-value pair.
    *
    * @param key   the key
    * @param value the value
    * @return {@code true} if the key-value pair exists
    */
   boolean containsEntry(K key, V value);

   /**
    * Returns an estimate of the number of entries in this multimap.
    *
    * @return the estimated size
    */
   long estimateSize();
}
