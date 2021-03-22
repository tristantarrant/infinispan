package org.infinispan.api.sync;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Flow;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.infinispan.api.common.Flag;
import org.infinispan.api.common.KeyValueEntry;
import org.infinispan.api.common.events.KeyValueListener;
import org.infinispan.api.configuration.CacheConfiguration;

/**
 *
 * @since 13.0
 **/
public interface Cache<K, V> {

   /**
    * Returns the name of this cache
    *
    * @return the name of the cache
    */
   String name();

   CacheConfiguration configuration();

   Cache<K, V> withFlags(EnumSet<Flag> flags);

   Cache<K, V> withFlags(Flag... flags);

   /**
    * Get the value of the Key if such exists
    *
    * @param key
    * @return the value
    */
   V get(K key);

   /**
    * Insert the key/value pair. Returns the previous value if present.
    *
    * @param key
    * @param value
    * @return Void
    */
   V put(K key, V value);

   /**
    * Similar to {@link #put(Object, Object)} but does not return the previous value.
    */
   void set(K key, V value);

   /**
    * Save the key/value.
    *
    * @param key
    * @param value
    * @return true if the entry was put
    */
   boolean putIfAbsent(K key, V value);

   /**
    * Delete the key
    *
    * @param key
    * @return true if the entry was removed
    */
   boolean remove(K key);

   /**
    * Retrieve all keys
    *
    * @return
    */
   Iterator<K> keys();

   /**
    * Retrieve all entries
    *
    * @return
    */
   Iterator<? extends KeyValueEntry<K, V>> entries();

   /**
    * Puts all entries
    *
    * @param entries
    * @return Void
    */
   void putAll(Map<K, V> entries);

   /**
    * Retrieves all entries for the supplied keys
    *
    * @param keys
    * @return
    */
   Map<K, V> getAll(Set<K> keys);

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys
    * @return
    */
   Set<K> removeAll(Set<K> keys);

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys
    * @return
    */
   Map<K, V> getAndRemoveAll(Set<K> keys);

   /**
    * Estimate the size of the store
    *
    * @return Long, estimated size
    */
   long estimateSize();

   /**
    * Clear the store. If a concurrent operation puts data in the store the clear might not properly work.
    */
   void clear();

   /**
    * Executes the query and returns an {@link Iterable}
    *
    * @param query query String
    * @return
    */
   <R> Iterable<QueryResult<R>> find(String query);

   /**
    * Find by query
    *
    * @param query
    * @return
    */
   <R> Query<R> query(String query);

   /**
    * Listens to the {@link KeyValueListener}
    *
    * @param listener
    */
   void listen(KeyValueListener<K, V> listener);

   /**
    * @param key
    * @param remappingFunction
    * @return
    */
   V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

   /**
    * @param key
    * @param mappingFunction
    * @return
    */
   V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction);

   /**
    * Returns an async version of this cache
    *
    * @return
    */
   org.infinispan.api.async.Cache async();

   /**
    * Returns a reactive version of this cache
    *
    * @return
    */
   org.infinispan.api.mutiny.Cache mutiny();

}
