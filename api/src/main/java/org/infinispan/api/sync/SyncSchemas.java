package org.infinispan.api.sync;

import org.jspecify.annotations.Nullable;

/**
 * Manage protobuf schemas.
 *
 * @since 15.1
 */
public interface SyncSchemas {

   /**
    * Registers a new schema. Throws if a schema with the same name already exists.
    *
    * @param name   the schema name (e.g. {@code "person.proto"})
    * @param schema the protobuf schema content
    */
   void create(String name, String schema);

   /**
    * Updates an existing schema. Throws if no schema with the given name exists.
    *
    * @param name   the schema name
    * @param schema the updated protobuf schema content
    */
   void update(String name, String schema);

   /**
    * Creates or updates a schema.
    *
    * @param name   the schema name
    * @param schema the protobuf schema content
    */
   void createOrUpdate(String name, String schema);

   /**
    * Removes a schema.
    *
    * @param name the schema name
    */
   void remove(String name);

   /**
    * Retrieves a schema by name.
    *
    * @param name the schema name
    * @return the schema content, or {@code null} if not found
    */
   @Nullable String get(String name);

   /**
    * Returns the names of all registered schemas.
    *
    * @return an iterable of schema names
    */
   Iterable<String> names();
}
