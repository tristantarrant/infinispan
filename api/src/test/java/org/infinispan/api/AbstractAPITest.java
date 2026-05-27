package org.infinispan.api;

import java.util.List;
import java.util.Map;

import org.infinispan.api.model.Author;
import org.infinispan.api.model.Book;
import org.infinispan.api.model.Review;
import org.infinispan.commons.jdkspecific.CallerId;

public abstract class AbstractAPITest {

   protected static final Map<String, Book> BOOKS = Map.of(
         "lotr", new Book("The Lord of the Rings", "An epic high fantasy novel", 1954,
               new Author("John Ronald Reuel", "Tolkien"),
               List.of(new Review("Alice", "A masterpiece of fantasy literature", 5),
                     new Review("Bob", "Too long but rewarding", 4))),
         "hobbit", new Book("The Hobbit", "A fantasy novel and prelude to The Lord of the Rings", 1937,
               new Author("John Ronald Reuel", "Tolkien"),
               List.of(new Review("Charlie", "A delightful adventure", 5))),
         "foundation", new Book("Foundation", "A science fiction novel about the fall of a galactic empire", 1951,
               new Author("Isaac", "Asimov")),
         "dune", new Book("Dune", "A science fiction novel set in the distant future", 1965,
               new Author("Frank", "Herbert"),
               List.of(new Review("Diana", "An incredible science fiction epic", 5),
                     new Review("Eve", "Complex but brilliant world building", 4)))
   );
   protected abstract InfinispanAPIExtension ext();

   public static String k() {
      return k(0);
   }

   public static String k(int index) {
      return String.format("k%d-%s", index, CallerId.getCallerMethodName(2));
   }

   public static String v() {
      return v(0);
   }

   public static String v(int index) {
      return String.format("v%d-%s", index, CallerId.getCallerMethodName(2));
   }
}
