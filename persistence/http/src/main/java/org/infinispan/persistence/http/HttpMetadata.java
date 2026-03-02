package org.infinispan.persistence.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.container.versioning.EntryVersion;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

@ProtoTypeId(ProtoStreamTypeIds.HTTP_METADATA)
public class HttpMetadata implements Metadata {

   private final long lifespan;
   private final Map<String, String> headers;

   public HttpMetadata(long lifespan, Map<String, String> headers) {
      this.lifespan = lifespan;
      this.headers = headers != null ? Collections.unmodifiableMap(new LinkedHashMap<>(headers)) : Collections.emptyMap();
   }

   @ProtoFactory
   HttpMetadata(long lifespan, List<HeaderEntry> headerEntries) {
      this.lifespan = lifespan;
      if (headerEntries != null && !headerEntries.isEmpty()) {
         Map<String, String> map = new LinkedHashMap<>(headerEntries.size());
         for (HeaderEntry entry : headerEntries) {
            map.put(entry.key, entry.value);
         }
         this.headers = Collections.unmodifiableMap(map);
      } else {
         this.headers = Collections.emptyMap();
      }
   }

   @ProtoField(number = 1, defaultValue = "-1")
   @Override
   public long lifespan() {
      return lifespan;
   }

   @ProtoField(number = 2, collectionImplementation = ArrayList.class)
   List<HeaderEntry> getHeaderEntries() {
      if (headers.isEmpty()) {
         return Collections.emptyList();
      }
      List<HeaderEntry> entries = new ArrayList<>(headers.size());
      for (Map.Entry<String, String> e : headers.entrySet()) {
         entries.add(new HeaderEntry(e.getKey(), e.getValue()));
      }
      return entries;
   }

   public Map<String, String> headers() {
      return headers;
   }

   @Override
   public long maxIdle() {
      return -1;
   }

   @Override
   public EntryVersion version() {
      return null;
   }

   @Override
   public Builder builder() {
      return new HttpMetadataBuilder()
            .headers(headers)
            .lifespan(lifespan);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      HttpMetadata that = (HttpMetadata) o;
      return lifespan == that.lifespan && Objects.equals(headers, that.headers);
   }

   @Override
   public int hashCode() {
      return Objects.hash(lifespan, headers);
   }

   @Override
   public String toString() {
      return "HttpMetadata{lifespan=" + lifespan + ", headers=" + headers + '}';
   }

   public static class HttpMetadataBuilder implements Metadata.Builder {

      private long lifespan = -1;
      private Map<String, String> headers;

      @Override
      public Metadata.Builder lifespan(long time, TimeUnit unit) {
         this.lifespan = unit.toMillis(time);
         return this;
      }

      @Override
      public Metadata.Builder lifespan(long time) {
         this.lifespan = time;
         return this;
      }

      @Override
      public Metadata.Builder maxIdle(long time, TimeUnit unit) {
         return this;
      }

      @Override
      public Metadata.Builder maxIdle(long time) {
         return this;
      }

      @Override
      public Metadata.Builder version(EntryVersion version) {
         return this;
      }

      public HttpMetadataBuilder headers(Map<String, String> headers) {
         this.headers = headers;
         return this;
      }

      @Override
      public Metadata.Builder merge(Metadata metadata) {
         if (metadata instanceof HttpMetadata httpMetadata) {
            if (lifespan < 0) {
               lifespan = httpMetadata.lifespan();
            }
            if (headers == null) {
               headers = httpMetadata.headers();
            }
         } else {
            if (lifespan < 0) {
               lifespan = metadata.lifespan();
            }
         }
         return this;
      }

      @Override
      public Metadata build() {
         return new HttpMetadata(lifespan, headers);
      }
   }

   @ProtoTypeId(ProtoStreamTypeIds.HTTP_METADATA_HEADER_ENTRY)
   public static class HeaderEntry {

      @ProtoField(number = 1)
      final String key;

      @ProtoField(number = 2)
      final String value;

      @ProtoFactory
      public HeaderEntry(String key, String value) {
         this.key = key;
         this.value = value;
      }

      public String key() {
         return key;
      }

      public String value() {
         return value;
      }
   }
}
