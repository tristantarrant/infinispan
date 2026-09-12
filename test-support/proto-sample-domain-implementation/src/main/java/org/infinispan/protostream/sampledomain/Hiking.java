package org.infinispan.protostream.sampledomain;

import org.infinispan.api.annotations.indexing.GeoField;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.api.annotations.indexing.Keyword;
import org.infinispan.api.annotations.indexing.model.LatLng;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.annotations.ProtoSyntax;

@Proto
@Indexed
public record Hiking(@Keyword(projectable = true) String name,
                     @GeoField(projectable = true, sortable = true) LatLng start,
                     @GeoField(projectable = true, sortable = true) LatLng end) {

   @ProtoSchema(
         dependsOn = LatLng.LatLngSchema.class,
         includeClasses = Hiking.class,
         schemaFileName = "hiking.proto",
         schemaPackageName = "geo",
         syntax = ProtoSyntax.PROTO3,
         service = false
   )
   public interface HikingSchema extends GeneratedSchema {
      Hiking.HikingSchema INSTANCE = new HikingSchemaImpl();
   }
}
