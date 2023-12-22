package org.infinispan.embedded;

import java.util.stream.Collectors;

import org.infinispan.Cache;
import org.infinispan.api.sync.SyncSchemas;
import org.infinispan.commons.internal.InternalCacheNames;

/**
 * @since 16.3
 */
class EmbeddedSyncSchemas implements SyncSchemas {
   private final Cache<String, String> metadataCache;

   EmbeddedSyncSchemas(Embedded embedded) {
      this.metadataCache = embedded.cacheManager.getCache(InternalCacheNames.PROTOBUF_METADATA_CACHE_NAME);
   }

   @Override
   public void create(String name, String schema) {
      String existing = metadataCache.putIfAbsent(name, schema);
      if (existing != null) {
         throw new IllegalArgumentException("Schema already exists: " + name);
      }
   }

   @Override
   public void update(String name, String schema) {
      String existing = metadataCache.replace(name, schema);
      if (existing == null) {
         throw new IllegalArgumentException("Schema does not exist: " + name);
      }
   }

   @Override
   public void createOrUpdate(String name, String schema) {
      metadataCache.put(name, schema);
   }

   @Override
   public void remove(String name) {
      metadataCache.remove(name);
   }

   @Override
   public String get(String name) {
      return metadataCache.get(name);
   }

   @Override
   public Iterable<String> names() {
      return metadataCache.keySet().stream()
            .filter(k -> !k.endsWith(".errors"))
            .collect(Collectors.toList());
   }
}
