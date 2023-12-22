package org.infinispan.embedded.impl;

import java.time.Instant;
import java.util.Optional;

import org.infinispan.api.common.CacheEntry;
import org.infinispan.api.common.CacheEntryExpiration;
import org.infinispan.api.common.CacheEntryMetadata;
import org.infinispan.api.common.CacheEntryVersion;
import org.infinispan.container.versioning.EntryVersion;
import org.infinispan.metadata.Metadata;
import org.infinispan.metadata.impl.PrivateMetadata;

public class EmbeddedCacheEntry<K, V> implements CacheEntry<K, V> {
   private final org.infinispan.container.entries.CacheEntry<K, V> entry;

   public EmbeddedCacheEntry(org.infinispan.container.entries.CacheEntry<K, V> entry) {
      this.entry = entry;
   }

   @Override
   public K key() {
      return entry.getKey();
   }

   @Override
   public V value() {
      return entry.getValue();
   }

   @Override
   public CacheEntryMetadata metadata() {
      return new CacheEntryMetadata() {
         @Override
         public Optional<Instant> creationTime() {
            long created = entry.getCreated();
            return created > 0 ? Optional.of(Instant.ofEpochMilli(created)) : Optional.empty();
         }

         @Override
         public Optional<Instant> lastAccessTime() {
            long lastUsed = entry.getLastUsed();
            return lastUsed > 0 ? Optional.of(Instant.ofEpochMilli(lastUsed)) : Optional.empty();
         }

         @Override
         public CacheEntryExpiration expiration() {
            return CacheEntryExpiration.DEFAULT;
         }

         @Override
         public CacheEntryVersion version() {
            Metadata metadata = entry.getMetadata();
            if (metadata != null) {
               EntryVersion v = metadata.version();
               if (v != null) {
                  return new EmbeddedCacheEntryVersion(v);
               }
            }
            PrivateMetadata internalMetadata = entry.getInternalMetadata();
            if (internalMetadata != null) {
               EntryVersion v = internalMetadata.entryVersion();
               if (v != null) {
                  return new EmbeddedCacheEntryVersion(v);
               }
            }
            return null;
         }
      };
   }
}
