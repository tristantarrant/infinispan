package org.infinispan.api;

import java.util.Map;

import org.infinispan.api.common.events.KeyValueCreatedListener;
import org.infinispan.api.common.events.KeyValueEvent;
import org.infinispan.api.common.events.KeyValueRemovedListener;
import org.infinispan.api.common.events.KeyValueUpdatedListener;
import org.infinispan.api.sync.Cache;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 13.0
 **/
public class SyncCacheAPITest {
   public void testCacheAPI() {
      try (Infinispan infinispan = Infinispan.create("file:///path/to/infinispan.xml")) {
         Cache<String, String> cache = infinispan.sync().caches().get("cache");

         // Simple ops
         cache.put("key", "value");
         cache.putIfAbsent("anotherKey", "anotherValue");
         String value = cache.get("key");
         cache.remove("key");

         // Compute
         cache.compute("key", (k, v) -> "value");
         cache.computeIfAbsent("key", (k) -> "value");

         // Bulk ops
         cache.putAll(Map.of("key1", "value1", "key2", "value2"));

         // Iteration over keys and entries
         cache.keys().forEachRemaining(k -> System.out.printf("key=%s%n", k));
         cache.entries().forEachRemaining(e -> System.out.printf("key=%s, value=%s%n", e.key(), e.value()));

         // Query
         Iterable<Object> results = cache.find("%alu%");

         // Parameterized query
         results = cache.query("...").skip(10).limit(100).param("a", "b").find();

         // Event handling
         cache.listen((KeyValueCreatedListener<String, String>) event -> {
            // Handle create event
         });

         cache.listen(new AListener());
      }
   }

   public static class AListener implements KeyValueUpdatedListener, KeyValueRemovedListener {

      @Override
      public void onRemove(KeyValueEvent event) {

      }

      @Override
      public void onUpdate(KeyValueEvent event) {

      }
   }
}
