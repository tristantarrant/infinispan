package org.infinispan.client.hotrod;

import java.util.stream.Collectors;

import org.infinispan.api.Experimental;
import org.infinispan.api.sync.SyncSchemas;
import org.infinispan.commons.internal.InternalCacheNames;
import org.infinispan.protostream.schema.Schema;

/**
 * @since 16.3
 */
@Experimental
final class HotRodSyncSchemas implements SyncSchemas {
   private final RemoteSchemasAdmin schemasAdmin;
   private final RemoteCache<String, String> metadataCache;

   HotRodSyncSchemas(HotRod hotrod) {
      this.schemasAdmin = hotrod.cacheManager.administration().schemas();
      this.metadataCache = hotrod.cacheManager.getCache(InternalCacheNames.PROTOBUF_METADATA_CACHE_NAME);
   }

   @Override
   public void create(String name, String schema) {
      RemoteSchemasAdmin.SchemaOpResult result = schemasAdmin.create(Schema.buildFromStringContent(name, schema));
      if (result.getType() == RemoteSchemasAdmin.SchemaOpResultType.NONE) {
         throw new IllegalArgumentException("Schema already exists: " + name);
      }
      if (result.hasError()) {
         throw new IllegalArgumentException("Schema validation error: " + result.getError());
      }
   }

   @Override
   public void update(String name, String schema) {
      RemoteSchemasAdmin.SchemaOpResult result = schemasAdmin.update(Schema.buildFromStringContent(name, schema));
      if (result.getType() == RemoteSchemasAdmin.SchemaOpResultType.NONE) {
         throw new IllegalArgumentException("Schema does not exist: " + name);
      }
      if (result.hasError()) {
         throw new IllegalArgumentException("Schema validation error: " + result.getError());
      }
   }

   @Override
   public void createOrUpdate(String name, String schema) {
      RemoteSchemasAdmin.SchemaOpResult result = schemasAdmin.createOrUpdate(Schema.buildFromStringContent(name, schema));
      if (result.hasError()) {
         throw new IllegalArgumentException("Schema validation error: " + result.getError());
      }
   }

   @Override
   public void remove(String name) {
      schemasAdmin.remove(name);
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
