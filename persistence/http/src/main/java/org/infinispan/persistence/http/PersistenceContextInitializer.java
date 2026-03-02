package org.infinispan.persistence.http;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(
      includeClasses = {
            HttpMetadata.class,
            HttpMetadata.HeaderEntry.class
      },
      schemaFileName = "persistence.http.proto",
      schemaFilePath = "org/infinispan/persistence/http",
      schemaPackageName = "org.infinispan.persistence.http",
      service = false,
      orderedMarshallers = true
)
interface PersistenceContextInitializer extends SerializationContextInitializer {
}
