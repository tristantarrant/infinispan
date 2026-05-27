package org.infinispan.embedded;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import org.infinispan.AdvancedCache;
import org.infinispan.api.async.AsyncSchemas;
import org.infinispan.commons.internal.InternalCacheNames;
import org.reactivestreams.FlowAdapters;

import io.reactivex.rxjava3.core.Flowable;

/**
 * @since 16.3
 */
class EmbeddedAsyncSchemas implements AsyncSchemas {
   private final AdvancedCache<String, String> metadataCache;

   EmbeddedAsyncSchemas(Embedded embedded) {
      this.metadataCache = (AdvancedCache<String, String>) embedded.cacheManager
            .<String, String>getCache(InternalCacheNames.PROTOBUF_METADATA_CACHE_NAME).getAdvancedCache();
   }

   @Override
   public CompletionStage<Void> create(String name, String schema) {
      return metadataCache.putIfAbsentAsync(name, schema).thenAccept(existing -> {
         if (existing != null) {
            throw new IllegalArgumentException("Schema already exists: " + name);
         }
      });
   }

   @Override
   public CompletionStage<Void> update(String name, String schema) {
      return metadataCache.replaceAsync(name, schema).thenAccept(existing -> {
         if (existing == null) {
            throw new IllegalArgumentException("Schema does not exist: " + name);
         }
      });
   }

   @Override
   public CompletionStage<Void> createOrUpdate(String name, String schema) {
      return metadataCache.putAsync(name, schema).thenAccept(v -> {
      });
   }

   @Override
   public CompletionStage<Void> remove(String name) {
      return metadataCache.removeAsync(name).thenAccept(v -> {
      });
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
