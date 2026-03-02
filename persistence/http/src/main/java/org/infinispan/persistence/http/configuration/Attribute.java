package org.infinispan.persistence.http.configuration;

import java.util.HashMap;
import java.util.Map;

public enum Attribute {
   // must be first
   UNKNOWN(null),

   BASE_URL("base-url"),
   USERNAME("username"),
   PASSWORD("password"),
   BEARER_TOKEN("bearer-token"),
   ;

   private final String name;

   Attribute(final String name) {
      this.name = name;
   }

   public String getLocalName() {
      return name;
   }

   private static final Map<String, Attribute> attributes;

   static {
      Map<String, Attribute> map = new HashMap<>();
      for (Attribute attribute : values()) {
         final String name = attribute.getLocalName();
         if (name != null) {
            map.put(name, attribute);
         }
      }
      attributes = map;
   }

   public static Attribute forName(final String localName) {
      final Attribute attribute = attributes.get(localName);
      return attribute == null ? UNKNOWN : attribute;
   }

   @Override
   public String toString() {
      return name;
   }
}
