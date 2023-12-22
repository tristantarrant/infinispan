package org.infinispan.api.sync;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.infinispan.api.common.CacheEntry;
import org.infinispan.api.common.CacheEntryVersion;
import org.infinispan.api.common.CacheOptions;
import org.infinispan.api.common.CacheWriteOptions;
import org.infinispan.api.common.CloseableIterable;
import org.infinispan.api.common.process.CacheEntryProcessorResult;
import org.infinispan.api.common.process.CacheProcessorOptions;
import org.infinispan.api.configuration.CacheConfiguration;
import org.infinispan.api.sync.events.cache.SyncCacheListener;
import org.jspecify.annotations.Nullable;

/**
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 * @since 14.0
 **/
public interface SyncCache<K, V> {

   /**
    * Returns the name of this cache
    *
    * @return the name of the cache
    */
   String name();

   /**
    * Returns the configuration of this cache
    *
    * @return the cache configuration
    */
   CacheConfiguration configuration();

   /**
    * Return the container of this cache
    *
    * @return the cache container
    */
   SyncContainer container();

   /**
    * Get the value of the Key if such exists
    *
    * @param key the key
    * @return the value
    */
   default @Nullable V get(K key) {
      return get(key, CacheOptions.DEFAULT);
   }

   /**
    * Get the value of the Key if such exists
    *
    * @param key the key
    * @return the value
    */
   default @Nullable V get(K key, CacheOptions options) {
      CacheEntry<K, V> entry = getEntry(key, options);
      return entry == null ? null : entry.value();
   }

   /**
    * Get the entry of the Key if such exists
    *
    * @param key the key
    * @return the entry
    */
   default @Nullable CacheEntry<K, V> getEntry(K key) {
      return getEntry(key, CacheOptions.DEFAULT);
   }

   /**
    * Get the entry of the Key if such exists
    *
    * @param key     the key
    * @param options the options
    * @return the entry
    */
   @Nullable CacheEntry<K, V> getEntry(K key, CacheOptions options);

   /**
    * Insert the key/value pair. Returns the previous value if present.
    *
    * @param key   the key
    * @param value the value
    * @return the previous entry, or {@code null}
    */
   default @Nullable CacheEntry<K, V> put(K key, V value) {
      return put(key, value, CacheWriteOptions.DEFAULT);
   }

   /**
    * Insert the key/value pair. Returns the previous entry if present.
    *
    * @param key     the key
    * @param value   the value
    * @param options the options
    * @return the previous entry, or {@code null}
    */
   @Nullable CacheEntry<K, V> put(K key, V value, CacheWriteOptions options);

   /**
    * Similar to {@link #put(Object, Object)} but does not return the previous value.
    */
   default void set(K key, V value) {
      set(key, value, CacheWriteOptions.DEFAULT);
   }

   /**
    * Similar to {@link #put(Object, Object)} but does not return the previous value.
    *
    * @param key     the key
    * @param value   the value
    * @param options the options
    */
   default void set(K key, V value, CacheWriteOptions options) {
      put(key, value, options);
   }

   /**
    * Save the key/value.
    *
    * @param key   the key
    * @param value the value
    * @return the previous value if present
    */
   default @Nullable CacheEntry<K, V> putIfAbsent(K key, V value) {
      return putIfAbsent(key, value, CacheWriteOptions.DEFAULT);
   }

   /**
    * Insert the key/value if such key does not exist
    *
    * @param key     the key
    * @param value   the value
    * @param options the options
    * @return the previous value if present
    */
   @Nullable CacheEntry<K, V> putIfAbsent(K key, V value, CacheWriteOptions options);

   /**
    * Save the key/value.
    *
    * @param key   the key
    * @param value the value
    * @return true if the entry was set
    */
   default boolean setIfAbsent(K key, V value) {
      return setIfAbsent(key, value, CacheWriteOptions.DEFAULT);
   }

   /**
    * Insert the key/value if such key does not exist
    *
    * @param key     the key
    * @param value   the value
    * @param options the options
    * @return {@code true} if the entry was set
    */
   default boolean setIfAbsent(K key, V value, CacheWriteOptions options) {
      CacheEntry<K, V> ce = putIfAbsent(key, value, options);
      return ce == null;
   }

   /**
    * Replaces the value for the specified key only if the version matches.
    *
    * @param key   the key
    * @param value the value
    * @return {@code true} if the value was replaced
    */
   default boolean replace(K key, V value, CacheEntryVersion version) {
      return replace(key, value, version, CacheWriteOptions.DEFAULT);
   }

   /**
    * Replaces the value for the specified key only if the version matches.
    *
    * @param key     the key
    * @param value   the value
    * @param options the options
    * @return {@code true} if the value was replaced
    */
   default boolean replace(K key, V value, CacheEntryVersion version, CacheWriteOptions options) {
      CacheEntry<K, V> ce = getOrReplaceEntry(key, value, version, options);
      return ce != null && version.equals(ce.metadata().version());
   }

   /**
    * Replaces the entry and returns the previous entry.
    *
    * @param key     the key
    * @param value   the value
    * @param version the expected version
    * @return the previous entry
    */
   default @Nullable CacheEntry<K, V> getOrReplaceEntry(K key, V value, CacheEntryVersion version) {
      return getOrReplaceEntry(key, value, version, CacheWriteOptions.DEFAULT);
   }

   /**
    * Replaces the entry and returns the previous entry.
    *
    * @param key     the key
    * @param value   the value
    * @param options the options
    * @param version the expected version
    * @return the previous entry
    */
   @Nullable CacheEntry<K, V> getOrReplaceEntry(K key, V value, CacheEntryVersion version, CacheWriteOptions options);

   /**
    * Delete the key
    *
    * @param key the key
    * @return true if the entry was removed
    */
   default boolean remove(K key) {
      return remove(key, CacheOptions.DEFAULT);
   }

   /**
    * Delete the key
    *
    * @param key     the key
    * @param options the options
    * @return true if the entry was removed
    */
   default boolean remove(K key, CacheOptions options) {
      CacheEntry<K, V> ce = getAndRemove(key, options);
      return ce != null;
   }

   /**
    * Delete the key only if the version matches
    *
    * @param key     the key
    * @param version the expected version
    * @return whether the entry was removed.
    */
   default boolean remove(K key, CacheEntryVersion version) {
      return remove(key, version, CacheOptions.DEFAULT);
   }

   /**
    * Delete the key only if the version matches
    *
    * @param key     the key
    * @param version the expected version
    * @param options the options
    * @return whether the entry was removed.
    */
   boolean remove(K key, CacheEntryVersion version, CacheOptions options);

   /**
    * Removes the key and returns its value if present.
    *
    * @param key the key
    * @return the value of the key before removal. Returns null if the key didn't exist.
    */
   default @Nullable CacheEntry<K, V> getAndRemove(K key) {
      return getAndRemove(key, CacheOptions.DEFAULT);
   }

   /**
    * Removes the key and returns its value if present.
    *
    * @param key     the key
    * @param options the options
    * @return the value of the key before removal. Returns null if the key didn't exist.
    */
   @Nullable CacheEntry<K, V> getAndRemove(K key, CacheOptions options);

   /**
    * Retrieve all keys
    *
    * @return the keys
    */
   default CloseableIterable<K> keys() {
      return keys(CacheOptions.DEFAULT);
   }

   /**
    * Retrieve all keys
    *
    * @param options the options
    * @return the keys
    */
   CloseableIterable<K> keys(CacheOptions options);

   /**
    * Retrieve all entries
    *
    * @return the entries
    */
   default CloseableIterable<CacheEntry<K, V>> entries() {
      return entries(CacheOptions.DEFAULT);
   }

   /**
    * Retrieve all entries
    *
    * @param options the options
    * @return the entries
    */
   CloseableIterable<CacheEntry<K, V>> entries(CacheOptions options);

   /**
    * Puts all entries
    *
    * @param entries the entries
    */
   default void putAll(Map<K, V> entries) {
      putAll(entries, CacheWriteOptions.DEFAULT);
   }

   /**
    * Puts all entries.
    *
    * @param entries the entries
    * @param options the options
    */
   void putAll(Map<K, V> entries, CacheWriteOptions options);

   /**
    * Retrieves all entries for the supplied keys
    *
    * @param keys the keys
    * @return the entries
    */
   default Map<K, V> getAll(Set<K> keys) {
      return getAll(keys, CacheOptions.DEFAULT);
   }

   /**
    * Retrieves all entries for the supplied keys
    *
    * @param keys    the keys
    * @param options the options
    * @return the entries
    */
   Map<K, V> getAll(Set<K> keys, CacheOptions options);

   /**
    * Retrieves all entries for the supplied keys
    *
    * @param keys the keys
    * @return the entries
    */
   default Map<K, V> getAll(K... keys) {
      return getAll(CacheOptions.DEFAULT, keys);
   }

   /**
    * Retrieves all entries for the supplied keys
    *
    * @param keys    the keys
    * @param options the options
    * @return the entries
    */
   Map<K, V> getAll(CacheOptions options, K... keys);

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys the keys
    * @return the removed keys
    */
   default Set<K> removeAll(Set<K> keys) {
      return removeAll(keys, CacheWriteOptions.DEFAULT);
   }

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys    the keys
    * @param options the options
    * @return the removed keys
    */
   Set<K> removeAll(Set<K> keys, CacheWriteOptions options);

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys the keys
    * @return the removed entries
    */
   default Map<K, CacheEntry<K, V>> getAndRemoveAll(Set<K> keys) {
      return getAndRemoveAll(keys, CacheWriteOptions.DEFAULT);
   }

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys the keys
    * @return the removed entries
    */
   default Map<K, CacheEntry<K, V>> getAndRemoveAll(Set<K> keys, CacheWriteOptions options) {
      Map<K, CacheEntry<K, V>> map = new HashMap<>(keys.size());
      for (K key : keys) {
         map.put(key, getAndRemove(key));
      }
      return map;
   }

   /**
    * Estimate the size of the store
    *
    * @return Long, estimated size
    */
   default long estimateSize() {
      return estimateSize(CacheOptions.DEFAULT);
   }

   /**
    * Estimate the size of the store
    *
    * @return Long, estimated size
    */
   long estimateSize(CacheOptions options);

   /**
    * Clear the store. If a concurrent operation puts data in the store the clear might not properly work.
    */
   default void clear() {
      clear(CacheOptions.DEFAULT);
   }

   /**
    * Clear the store. If a concurrent operation puts data in the store the clear might not properly work.
    */
   void clear(CacheOptions options);

   /**
    * Find by query
    *
    * @param query the query string
    * @return a query builder
    */
   default <R> SyncQuery<K, V, R> query(String query) {
      return query(query, CacheOptions.DEFAULT);
   }

   /**
    * Find by query
    *
    * @param query   the query string
    * @param options the options
    * @param <R>
    * @return a query builder
    */
   <R> SyncQuery<K, V, R> query(String query, CacheOptions options);

   /**
    * Returns a listener builder for this cache. Register callbacks for the desired event types and call
    * {@link SyncCacheListener#install()} to activate the listener.
    *
    * <pre>{@code
    * AutoCloseable registration = cache.listen()
    *    .onCreate(event -> System.out.println("Created: " + event.newEntry().key()))
    *    .onRemove(event -> System.out.println("Removed: " + event.previousEntry().key()))
    *    .install();
    * }</pre>
    *
    * @return a {@link SyncCacheListener} builder
    */
   SyncCacheListener<K, V> listen();

   /**
    * Process entries using the supplied processor
    *
    * @param <T>
    * @param keys      the keys
    * @param processor the entry processor
    */
   default <T> Set<CacheEntryProcessorResult<K, T>> process(Set<K> keys, SyncCacheEntryProcessor<K, V, T> processor) {
      return process(keys, processor, CacheProcessorOptions.DEFAULT);
   }

   /**
    * Process entries using the supplied processor
    *
    * @param <T>
    * @param keys      the keys
    * @param processor the entry processor
    * @param options   the options
    */
   <T> Set<CacheEntryProcessorResult<K, T>> process(Set<K> keys, SyncCacheEntryProcessor<K, V, T> processor, CacheProcessorOptions options);

   /**
    * Process entries using the supplied processor
    *
    * @param <T>
    * @param processor the entry processor
    */
   default <T> Set<CacheEntryProcessorResult<K, T>> processAll(SyncCacheEntryProcessor<K, V, T> processor) {
      return processAll(processor, CacheProcessorOptions.DEFAULT);
   }

   /**
    * Process entries using the supplied processor
    *
    * @param <T>
    * @param processor the entry processor
    * @param options   the options
    */
   <T> Set<CacheEntryProcessorResult<K, T>> processAll(SyncCacheEntryProcessor<K, V, T> processor, CacheProcessorOptions options);

   /**
    * Returns the streaming cache.
    *
    * @return the streaming cache
    */
   SyncStreamingCache<K> streaming();

   /**
    * Returns the {@link SyncTransactionManager} associated with this cache, or {@code null} if the cache is not
    * transactional.
    *
    * @return the transaction manager, or {@code null}
    */
   default @Nullable SyncTransactionManager transactionManager() {
      return null;
   }

   /**
    * Returns a cache instance that performs operations using the specified {@link Subject}. Only applies to caches
    * with authorization enabled.
    *
    * @param subject the subject to impersonate
    * @return a cache instance that uses the specified subject for authorization
    */
   SyncCache<K, V> as(Subject subject);
}
