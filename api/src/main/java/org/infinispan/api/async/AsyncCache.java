package org.infinispan.api.async;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import javax.security.auth.Subject;

import org.infinispan.api.async.events.cache.AsyncCacheListener;
import org.infinispan.api.common.CacheEntry;
import org.infinispan.api.common.CacheEntryVersion;
import org.infinispan.api.common.CacheOptions;
import org.infinispan.api.common.CacheWriteOptions;
import org.infinispan.api.common.process.CacheEntryProcessorResult;
import org.infinispan.api.common.process.CacheProcessor;
import org.infinispan.api.common.process.CacheProcessorOptions;
import org.infinispan.api.configuration.CacheConfiguration;
import org.jspecify.annotations.Nullable;

/**
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 * @since 14.0
 **/
public interface AsyncCache<K, V> {
   /**
    * The name of the cache.
    *
    * @return the name
    */
   String name();

   /**
    * The configuration for this cache.
    *
    * @return the configuration
    */
   CompletionStage<CacheConfiguration> configuration();

   /**
    * Return the container of this cache
    *
    * @return the container
    */
   AsyncContainer container();

   /**
    * Get the value of the Key if such exists
    *
    * @param key the key
    * @return the value
    */
   default CompletionStage<@Nullable V> get(K key) {
      return get(key, CacheOptions.DEFAULT);
   }

   /**
    * Get the value of the Key if such exists
    *
    * @param key     the key
    * @param options the options
    * @return the value
    */
   default CompletionStage<@Nullable V> get(K key, CacheOptions options) {
      return getEntry(key, options)
            .thenApply(ce -> ce != null ? ce.value() : null);
   }

   /**
    * Get the entry of the Key if such exists
    *
    * @param key the key
    * @return the entry
    */
   default CompletionStage<@Nullable CacheEntry<K, V>> getEntry(K key) {
      return getEntry(key, CacheOptions.DEFAULT);
   }

   /**
    * Get the entry of the Key if such exists
    *
    * @param key     the key
    * @param options the options
    * @return the entry
    */
   CompletionStage<@Nullable CacheEntry<K, V>> getEntry(K key, CacheOptions options);

   /**
    * Insert the key/value if such key does not exist
    *
    * @param key   the key
    * @param value the value
    * @return the previous value if present
    */
   default CompletionStage<@Nullable CacheEntry<K, V>> putIfAbsent(K key, V value) {
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
   CompletionStage<@Nullable CacheEntry<K, V>> putIfAbsent(K key, V value, CacheWriteOptions options);

   /**
    * Insert the key/value if such key does not exist
    *
    * @param key   the key
    * @param value the value
    * @return {@code true} if the entry was set
    */
   default CompletionStage<Boolean> setIfAbsent(K key, V value) {
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
   default CompletionStage<Boolean> setIfAbsent(K key, V value, CacheWriteOptions options) {
      return putIfAbsent(key, value, options)
            .thenApply(Objects::isNull);
   }

   /**
    * Insert the key/value pair. Returns the previous entry if present.
    *
    * @param key   the key
    * @param value the value
    * @return the previous entry, or {@code null}
    */
   default CompletionStage<@Nullable CacheEntry<K, V>> put(K key, V value) {
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
   CompletionStage<@Nullable CacheEntry<K, V>> put(K key, V value, CacheWriteOptions options);

   /**
    * Similar to {@link #put(Object, Object)} but does not return the previous value.
    *
    * @param key   the key
    * @param value the value
    */
   default CompletionStage<Void> set(K key, V value) {
      return set(key, value, CacheWriteOptions.DEFAULT);
   }

   /**
    * Similar to {@link #put(Object, Object)} but does not return the previous value.
    *
    * @param key     the key
    * @param value   the value
    * @param options the options
    */
   default CompletionStage<Void> set(K key, V value, CacheWriteOptions options) {
      return put(key, value, options)
            .thenApply(__ -> null);
   }

   /**
    * Replaces the value for the specified key only if the version matches.
    *
    * @param key   the key
    * @param value the value
    * @return {@code true} if the value was replaced
    */
   default CompletionStage<Boolean> replace(K key, V value, CacheEntryVersion version) {
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
   default CompletionStage<Boolean> replace(K key, V value, CacheEntryVersion version, CacheWriteOptions options) {
      return getOrReplaceEntry(key, value, version, options)
            .thenApply(ce -> ce != null && version.equals(ce.metadata().version()));
   }

   /**
    * Replaces the entry and returns the previous entry.
    *
    * @param key     the key
    * @param value   the value
    * @param version the expected version
    * @return the previous entry
    */
   default CompletionStage<@Nullable CacheEntry<K, V>> getOrReplaceEntry(K key, V value, CacheEntryVersion version) {
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
   CompletionStage<@Nullable CacheEntry<K, V>> getOrReplaceEntry(K key, V value, CacheEntryVersion version, CacheWriteOptions options);

   /**
    * Delete the key
    *
    * @param key the key
    * @return whether the entry was removed.
    */
   default CompletionStage<Boolean> remove(K key) {
      return remove(key, CacheOptions.DEFAULT);
   }

   /**
    * Delete the key
    *
    * @param key     the key
    * @param options the options
    * @return whether the entry was removed.
    */
   CompletionStage<Boolean> remove(K key, CacheOptions options);

   /**
    * Delete the key only if the version matches
    *
    * @param key     the key
    * @param version the expected version
    * @return whether the entry was removed.
    */
   default CompletionStage<Boolean> remove(K key, CacheEntryVersion version) {
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
   CompletionStage<Boolean> remove(K key, CacheEntryVersion version, CacheOptions options);

   /**
    * Removes the key and returns its value if present.
    *
    * @param key the key
    * @return the value of the key before removal. Returns null if the key didn't exist.
    */
   default CompletionStage<@Nullable CacheEntry<K, V>> getAndRemove(K key) {
      return getAndRemove(key, CacheOptions.DEFAULT);
   }

   /**
    * Removes the key and returns its value if present.
    *
    * @param key     the key
    * @param options the options
    * @return the value of the key before removal. Returns null if the key didn't exist.
    */
   CompletionStage<@Nullable CacheEntry<K, V>> getAndRemove(K key, CacheOptions options);

   /**
    * Retrieve all keys
    *
    * @return the keys
    */
   default Flow.Publisher<K> keys() {
      return keys(CacheOptions.DEFAULT);
   }

   /**
    * Retrieve all keys
    *
    * @param options the options
    * @return the keys
    */
   Flow.Publisher<K> keys(CacheOptions options);

   /**
    * Retrieve all entries
    *
    * @return the entries
    */
   default Flow.Publisher<CacheEntry<K, V>> entries() {
      return entries(CacheOptions.DEFAULT);
   }

   /**
    * Retrieve all entries
    *
    * @param options the options
    * @return the entries
    */
   Flow.Publisher<CacheEntry<K, V>> entries(CacheOptions options);

   /**
    * Puts all entries.
    *
    * @param entries the entries
    */
   default CompletionStage<Void> putAll(Map<K, V> entries) {
      return putAll(entries, CacheWriteOptions.DEFAULT);
   }

   /**
    * Puts all entries.
    *
    * @param entries the entries
    * @param options the options
    */
   CompletionStage<Void> putAll(Map<K, V> entries, CacheWriteOptions options);

   /**
    * Puts all entries.
    *
    * @param entries the entries
    */
   default CompletionStage<Void> putAll(Flow.Publisher<CacheEntry<K, V>> entries) {
      return putAll(entries, CacheWriteOptions.DEFAULT);
   }

   /**
    * Puts all entries.
    *
    * @param entries the entries
    * @param options the options
    */
   CompletionStage<Void> putAll(Flow.Publisher<CacheEntry<K, V>> entries, CacheWriteOptions options);

   /**
    * Retrieves the entries for the specified keys.
    *
    * @param keys the keys
    * @return the entries
    */
   default Flow.Publisher<CacheEntry<K, V>> getAll(Set<K> keys) {
      return getAll(keys, CacheOptions.DEFAULT);
   }

   /**
    * Retrieves the entries for the specified keys.
    *
    * @param keys    the keys
    * @param options the options
    * @return the entries
    */
   Flow.Publisher<CacheEntry<K, V>> getAll(Set<K> keys, CacheOptions options);

   /**
    * Retrieves the entries for the specified keys.
    *
    * @param keys the keys
    * @return the entries
    */
   default Flow.Publisher<CacheEntry<K, V>> getAll(K... keys) {
      return getAll(CacheOptions.DEFAULT, keys);
   }

   /**
    * Retrieves the entries for the specified keys.
    *
    * @param keys    the keys
    * @param options the options
    * @return the entries
    */
   Flow.Publisher<CacheEntry<K, V>> getAll(CacheOptions options, K... keys);

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys the keys
    * @return the removed keys
    */
   default Flow.Publisher<K> removeAll(Set<K> keys) {
      return removeAll(keys, CacheWriteOptions.DEFAULT);
   }

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys    the keys
    * @param options the options
    * @return the removed keys
    */
   Flow.Publisher<K> removeAll(Set<K> keys, CacheWriteOptions options);

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys the keys
    * @return the removed keys
    */
   default Flow.Publisher<K> removeAll(Flow.Publisher<K> keys) {
      return removeAll(keys, CacheWriteOptions.DEFAULT);
   }

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys    the keys
    * @param options the options
    * @return the removed keys
    */
   Flow.Publisher<K> removeAll(Flow.Publisher<K> keys, CacheWriteOptions options);

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys the keys
    * @return the removed entries
    */
   default Flow.Publisher<CacheEntry<K, V>> getAndRemoveAll(Set<K> keys) {
      return getAndRemoveAll(keys, CacheWriteOptions.DEFAULT);
   }

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys    the keys
    * @param options the options
    * @return the removed entries
    */
   Flow.Publisher<CacheEntry<K, V>> getAndRemoveAll(Set<K> keys, CacheWriteOptions options);

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys the keys
    * @return the removed entries
    */
   default Flow.Publisher<CacheEntry<K, V>> getAndRemoveAll(Flow.Publisher<K> keys) {
      return getAndRemoveAll(keys, CacheWriteOptions.DEFAULT);
   }

   /**
    * Removes a set of keys. Returns the keys that were removed.
    *
    * @param keys    the keys
    * @param options the options
    * @return the removed entries
    */
   Flow.Publisher<CacheEntry<K, V>> getAndRemoveAll(Flow.Publisher<K> keys, CacheWriteOptions options);

   /**
    * Estimate the size of the store
    *
    * @return Long, estimated size
    */
   default CompletionStage<Long> estimateSize() {
      return estimateSize(CacheOptions.DEFAULT);
   }

   /**
    * Estimate the size of the store
    *
    * @param options the options
    * @return Long, estimated size
    */
   CompletionStage<Long> estimateSize(CacheOptions options);

   /**
    * Clear the cache. If a concurrent operation puts data in the cache the clear might work properly
    *
    */
   default CompletionStage<Void> clear() {
      return clear(CacheOptions.DEFAULT);
   }

   /**
    * Clear the cache. If a concurrent operation puts data in the cache the clear might not properly work
    *
    * @param options the options
    */
   CompletionStage<Void> clear(CacheOptions options);

   /**
    * Find by query.
    *
    * @param <R>
    * @param query query String
    * @return a query builder
    */
   default <R> AsyncQuery<K, V, R> query(String query) {
      return query(query, CacheOptions.DEFAULT);
   }

   /**
    * Executes the query and returns an iterable with the entries
    *
    * @param <R>
    * @param query   query String
    * @param options the options
    * @return a query builder
    */
   <R> AsyncQuery<K, V, R> query(String query, CacheOptions options);

   /**
    * Returns a listener builder for this cache. Register callbacks for the desired event types and call
    * {@link AsyncCacheListener#install()} to activate the listener.
    *
    * @return an {@link AsyncCacheListener} builder
    */
   AsyncCacheListener<K, V> listen();

   /**
    * Process entries using the supplied task
    *
    * @param keys      the keys
    * @param processor the entry processor
    * @return the processing results
    */
   default <T> Flow.Publisher<CacheEntryProcessorResult<K, T>> process(Set<K> keys, AsyncCacheEntryProcessor<K, V, T> processor) {
      return process(keys, processor, CacheOptions.DEFAULT);
   }

   /**
    * Process entries using the supplied task
    *
    * @param keys    the keys
    * @param task    the entry processor task
    * @param options the options
    * @return the processing results
    */
   <T> Flow.Publisher<CacheEntryProcessorResult<K, T>> process(Set<K> keys, AsyncCacheEntryProcessor<K, V, T> task, CacheOptions options);

   /**
    * Execute a {@link CacheProcessor} on a cache
    *
    * @param <T>
    * @param processor the entry processor
    * @return the processing results
    */
   default <T> Flow.Publisher<CacheEntryProcessorResult<K, T>> processAll(AsyncCacheEntryProcessor<K, V, T> processor) {
      return processAll(processor, CacheProcessorOptions.DEFAULT);
   }

   /**
    * Execute a {@link CacheProcessor} on a cache
    *
    * @param <T>
    * @param processor the entry processor
    * @param options   the options
    * @return the processing results
    */
   <T> Flow.Publisher<CacheEntryProcessorResult<K, T>> processAll(AsyncCacheEntryProcessor<K, V, T> processor, CacheProcessorOptions options);

   /**
    * Returns the streaming cache.
    *
    * @return the streaming cache
    */
   AsyncStreamingCache<K> streaming();

   /**
    * Returns the {@link AsyncTransactionManager} associated with this cache, or {@code null} if the cache is not
    * transactional.
    *
    * @return the transaction manager, or {@code null}
    */
   default @Nullable AsyncTransactionManager transactionManager() {
      return null;
   }

   /**
    * Returns a cache instance that performs operations using the specified {@link Subject}. Only applies to caches
    * with authorization enabled.
    *
    * @param subject the subject to impersonate
    * @return a cache instance that uses the specified subject for authorization
    */
   AsyncCache<K, V> as(Subject subject);
}
