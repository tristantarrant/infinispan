package org.infinispan.client.hotrod;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import org.infinispan.api.Experimental;
import org.infinispan.api.async.AsyncSchemas;
import org.infinispan.commons.internal.InternalCacheNames;
import org.infinispan.protostream.schema.Schema;
import org.reactivestreams.FlowAdapters;

import io.reactivex.rxjava3.core.Flowable;

/**
 * @since 16.3
 */
@Experimental
final class HotRodAsyncSchemas implements AsyncSchemas {
   private final RemoteSchemasAdmin schemasAdmin;
   private final RemoteCache<String, String> metadataCache;

   HotRodAsyncSchemas(HotRod hotrod) {
      this.schemasAdmin = hotrod.cacheManager.administration().schemas();
      this.metadataCache = hotrod.cacheManager.getCache(InternalCacheNames.PROTOBUF_METADATA_CACHE_NAME);
   }

   @Override
   public CompletionStage<Void> create(String name, String schema) {
      return schemasAdmin.createAsync(Schema.buildFromStringContent(name, schema)).thenAccept(result -> {
         if (result.getType() == RemoteSchemasAdmin.SchemaOpResultType.NONE) {
            throw new IllegalArgumentException("Schema already exists: " + name);
         }
         if (result.hasError()) {
            throw new IllegalArgumentException("Schema validation error: " + result.getError());
         }
      });
   }

   @Override
   public CompletionStage<Void> update(String name, String schema) {
      return schemasAdmin.updateAsync(Schema.buildFromStringContent(name, schema)).thenAccept(result -> {
         if (result.getType() == RemoteSchemasAdmin.SchemaOpResultType.NONE) {
            throw new IllegalArgumentException("Schema does not exist: " + name);
         }
         if (result.hasError()) {
            throw new IllegalArgumentException("Schema validation error: " + result.getError());
         }
      });
   }

   @Override
   public CompletionStage<Void> createOrUpdate(String name, String schema) {
      return schemasAdmin.createOrUpdateAsync(Schema.buildFromStringContent(name, schema)).thenAccept(result -> {
         if (result.hasError()) {
            throw new IllegalArgumentException("Schema validation error: " + result.getError());
         }
      });
   }

   @Override
   public CompletionStage<Void> remove(String name) {
      return schemasAdmin.removeAsync(name).thenAccept(v -> {});
   }

   @Override
   public CompletionStage<String> get(String name) {
      return metadataCache.getAsync(name);
   }

   @Override
   public Flow.Publisher<String> names() {
      return FlowAdapters.toFlowPublisher(
            Flowable.fromIterable(metadataCache.keySet())
                  .filter(k -> !k.endsWith(".errors"))
      );
   }
}
