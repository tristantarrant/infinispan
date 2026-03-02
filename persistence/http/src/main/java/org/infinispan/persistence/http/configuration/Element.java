package org.infinispan.persistence.http.configuration;

import java.util.HashMap;
import java.util.Map;

public enum Element {
   // must be first
   UNKNOWN(null),

   AUTHENTICATION("authentication"),
   HTTP_STORE("http-store"),
   ;

   private final String name;

   Element(final String name) {
      this.name = name;
   }

   public String getLocalName() {
      return name;
   }

   private static final Map<String, Element> MAP;

   static {
      final Map<String, Element> map = new HashMap<>(values().length);
      for (Element element : values()) {
         final String name = element.getLocalName();
         if (name != null) {
            map.put(name, element);
         }
      }
      MAP = map;
   }

   public static Element forName(final String localName) {
      final Element element = MAP.get(localName);
      return element == null ? UNKNOWN : element;
   }

   @Override
   public String toString() {
      return name;
   }
}
