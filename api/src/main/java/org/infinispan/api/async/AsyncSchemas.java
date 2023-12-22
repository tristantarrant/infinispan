package org.infinispan.api.async;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import org.jspecify.annotations.Nullable;

/**
 * Manage protobuf schemas asynchronously.
 *
 * @since 15.1
 */
public interface AsyncSchemas {

   /**
    * Registers a new schema. The returned stage completes exceptionally if a schema with the same name already exists.
    *
    * @param name   the schema name (e.g. {@code "person.proto"})
    * @param schema the protobuf schema content
    */
   CompletionStage<Void> create(String name, String schema);

   /**
    * Updates an existing schema. The returned stage completes exceptionally if no schema with the given name exists.
    *
    * @param name   the schema name
    * @param schema the updated protobuf schema content
    */
   CompletionStage<Void> update(String name, String schema);

   /**
    * Creates or updates a schema.
    *
    * @param name   the schema name
    * @param schema the protobuf schema content
    */
   CompletionStage<Void> createOrUpdate(String name, String schema);

   /**
    * Removes a schema.
    *
    * @param name the schema name
    */
   CompletionStage<Void> remove(String name);

   /**
    * Retrieves a schema by name.
    *
    * @param name the schema name
    * @return a stage completing with the schema content, or {@code null} if not found
    */
   CompletionStage<@Nullable String> get(String name);

   /**
    * Returns the names of all registered schemas.
    *
    * @return a publisher of schema names
    */
   Flow.Publisher<String> names();
}
