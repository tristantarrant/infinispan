package org.infinispan.api.mutiny;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Flow;

import org.infinispan.api.Experimental;
import org.infinispan.api.common.Flag;
import org.infinispan.api.common.KeyValueEntry;
import org.infinispan.api.common.events.KeyValueListener;
import org.infinispan.api.configuration.CacheConfiguration;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * A Reactive Cache provides a highly concurrent and distributed data structure, non blocking and using reactive
 * streams.
 * <p>
 *
 * </p>
 *
 * @author Katia Aresti, karesti@redhat.com
 * @see <a href="http://www.infinispan.org">Infinispan documentation</a>
 * @since 10.0
 */
@Experimental
public interface Cache<K, V> {
   /**
    * The name of the cache.
    *
    * @return
    */
   String name();

   /**
    * The configuration for this cache.
    *
    * @return
    */
   Uni<CacheConfiguration> configuration();

   /**
    * Returns a new instance of this cache with the supplied {@link Flag}s
    * @param flags
    * @return
    */
   Cache<K, V> withFlags(EnumSet<Flag> flags);

   /**
    * Returns a new instance of this cache with the supplied {@link Flag}s
    * @param flags
    * @return
    */
   Cache<K, V> withFlags(Flag... flags);

   /**
    * Get the value of the Key if such exists
    *
    * @param key
    * @return the value
    */
   Uni<V> get(K key);

   /**
    * Insert the key/value if such key does not exist
    *
    * @param key
    * @param value
    * @return Void
    */
   Uni<Boolean> putIfAbsent(K key, V value);

   /**
    * Save the key/value. If the key exists will replace the value
    *
    * @param key
    * @param value
    * @return Void
    */
   Uni<V> put(K key, V value);

   /**
    *
    * @param key
    * @param value
    * @return
    */
   Uni<Void> set(K key, V value);

   /**
    * Delete the key
    *
    * @param key
    * @return
    */
   Uni<Boolean> remove(K key);

   /**
    * Retrieve all keys
    *
    * @return Publisher
    */
   Multi<K> keys();

   /**
    * Retrieve all entries
    *
    * @return
    */
   Multi<? extends Map.Entry<K, V>> entries();

   /**
    * Retrieve all the entries for the specified keys.
    *
    * @param keys
    * @return
    */
   Multi<? extends Map.Entry<K, V>> getAll(Multi<K> keys);

   /**
    * Put multiple entries from a {@link Multi}
    *
    * @param pairs
    * @return Void
    */
   Multi<WriteResult<K>> put(Multi<Map.Entry<K, V>> pairs);

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys
    * @return
    */
   Multi<K> remove(Multi<K> keys);

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys
    * @return
    */
   Multi<? extends Map.Entry<K, V>> getAndRemoveAll(Multi<K> keys);

   /**
    * Estimate the size of the store
    *
    * @return Long, estimated size
    */
   Uni<Long> estimateSize();

   /**
    * Clear the store. If a concurrent operation puts data in the store the clear might not properly work
    *
    * @return Void
    */
   Uni<Void> clear();

   /**
    * Executes the query and returns a reactive streams Publisher with the results
    *
    * @param query query String
    * @return
    */
   <R> Multi<R> find(String query);

   /**
    * Find by QueryRequest.
    *
    * @param query
    * @return
    */
   <R> Query<R> query(String query);

   /**
    * Executes the query and returns a reactive streams Publisher with the results
    *
    * @param query query String
    * @return Publisher reactive streams
    */
   <R> Multi<R> findContinuously(String query);

   /**
    * Listens to the {@link KeyValueListener}
    *
    * @param listener
    * @return Publisher reactive streams
    */
   Multi<KeyValueEntry<K, V>> listen(KeyValueListener listener);
}
