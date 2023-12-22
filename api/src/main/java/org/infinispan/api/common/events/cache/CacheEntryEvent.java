package org.infinispan.api.common.events.cache;

import org.infinispan.api.common.CacheEntry;
import org.jspecify.annotations.Nullable;

/**
 * @param <K> the type of key
 * @param <V> the type of value
 * @since 14.0
 **/
public interface CacheEntryEvent<K, V> {
   /**
    * @return The entry after the event
    */
   @Nullable CacheEntry<K, V> newEntry();

   /**
    * @return The entry before the event
    */
   @Nullable CacheEntry<K, V> previousEntry();

   /**
    * @return True if this event is generated from an existing entry as the listener has {@link
    * CacheListenerOptions#includeCurrentState()} set to <code>true</code>.
    */
   default boolean isCurrentState() {
      return false;
   }

   /**
    * @return an identifier of the transaction or cache invocation that triggered the event. In a transactional cache,
    * it is the transaction object associated with the current call. In a non-transactional cache, it is an internal
    * object that identifies the cache invocation.
    */
   default @Nullable Object getSource() {
      return null;
   }

   /**
    * @return true if the call originated on the local cache instance; false if originated from a remote one.
    */
   default boolean isOriginLocal() {
      return false;
   }

   CacheEntryEventType type();
}
