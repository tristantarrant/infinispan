package org.infinispan.api.async;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.infinispan.api.common.tasks.EntryConsumerTask;
import org.infinispan.api.common.Flag;
import org.infinispan.api.common.events.AsyncListenerHandle;
import org.infinispan.api.common.events.KeyValueListener;
import org.infinispan.api.configuration.CacheConfiguration;

/**
 * @since 13.0
 **/
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
   CompletionStage<CacheConfiguration> configuration();

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
   CompletionStage<V> get(K key);

   /**
    * Insert the key/value if such key does not exist
    *
    * @param key
    * @param value
    * @return Void
    */
   CompletionStage<Boolean> putIfAbsent(K key, V value);

   /**
    * @param key
    * @param value
    * @return Void
    */
   CompletionStage<V> put(K key, V value);

   /**
    * @param key
    * @param value
    * @return
    */
   CompletionStage<Void> set(K key, V value);

   /**
    * Delete the key
    *
    * @param key
    * @return whether the entry was removed.
    */
   CompletionStage<Boolean> remove(K key);

   /**
    * Delete the key, returning its value
    *
    * @param key
    * @return whether the entry was removed.
    */
   CompletionStage<V> getAndRemove(K key);

   /**
    * Retrieve all keys
    *
    * @return
    */
   Flow.Publisher<K> keys();

   /**
    * Retrieve all entries
    *
    * @return
    */
   Flow.Publisher<? extends Map.Entry<K, V>> entries();

   /**
    * @param entries
    * @return Void
    */
   CompletionStage<Void> putAll(Map<K, V> entries);

   /**
    * Retrieves the entries for the specified keys.
    *
    * @param keys
    * @return
    */
   Flow.Publisher<? extends Map.Entry<K, V>> getAll(Set<K> keys);

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys
    * @return
    */
   Flow.Publisher<K> removeAll(Set<K> keys);

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys
    * @return
    */
   Flow.Publisher<? extends Map.Entry<K, V>> getAndRemoveAll(Set<K> keys);

   /**
    * Estimate the size of the store
    *
    * @return Long, estimated size
    */
   CompletionStage<Long> estimateSize();

   /**
    * Clear the store. If a concurrent operation puts data in the store the clear might not properly work
    *
    * @return Void
    */
   CompletionStage<Void> clear();

   /**
    * Executes the query and returns an iterable with the entries
    *
    * @param query query String
    * @return Publisher reactive streams
    */
   <R> CompletionStage<QueryResult<R>> find(String query);

   /**
    * @param listener
    * @return
    */
   CompletionStage<AsyncListenerHandle<KeyValueListener>> listen(KeyValueListener listener);

   /**
    * @param key
    * @param remappingFunction
    * @return
    */
   CompletionStage<V> compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

   /**
    * @param key
    * @param mappingFunction
    * @return
    */
   CompletionStage<V> computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction);

   CompletionStage<Void> process(EntryConsumerTask<K, V> task);

   /**
    * Returns a sync version of this cache
    *
    * @return
    */
   org.infinispan.api.sync.Cache sync();

   /**
    * Returns a reactive version of this cache
    *
    * @return
    */
   org.infinispan.api.mutiny.Cache reactive();
}
