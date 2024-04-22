package org.infinispan.cli.wizard;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Values {
   private final Map<String, Object> values = new LinkedHashMap<>();

   public boolean hasValue(String name) {
      return values.containsKey(name);
   }

   public int asInt(String name) {
      return (Integer) values.get(name);
   }

   public boolean asBoolean(String name) {
      return (Boolean) values.get(name);
   }

   public void setValue(String name, Object value) {
      values.put(name, value);
   }

   @Override
   public String toString() {
      return "Values{" +
            "values=" + values +
            '}';
   }

   public String asString(String name) {
      return (String) values.get(name);
   }

   public char[] asCharArray(String name) {
      return (char[]) values.get(name);
   }

   public Iterable<Values> asIterable(String prefix) {
      return () -> new Iterator<>() {
         private int i = 0;

         @Override
         public boolean hasNext() {
            String ithPrefix = prefix + '.' + i + '.';
            return values.keySet().stream().anyMatch(k -> k.startsWith(ithPrefix));
         }

         @Override
         public Values next() {
            Values v = new Values();
            String ithPrefix = prefix + '.' + i + '.';
            values.entrySet().stream().filter(e -> e.getKey().startsWith(ithPrefix)).forEach(e -> v.setValue(e.getKey().substring(ithPrefix.length() + 1), e.getValue()));
            i++;
            return v;
         }
      };
   }
}
