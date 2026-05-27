package org.infinispan.embedded;

import static org.infinispan.configuration.cache.IndexStorage.LOCAL_HEAP;

import org.infinispan.api.AbstractAsyncCacheQueryTest;
import org.infinispan.api.InfinispanAPIExtension;
import org.infinispan.api.model.Author;
import org.infinispan.api.model.Book;
import org.infinispan.api.model.Review;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.junit.jupiter.api.extension.RegisterExtension;

public class EmbeddedLocalAsyncCacheQueryTest extends AbstractAsyncCacheQueryTest {

   @RegisterExtension
   static EmbeddedInfinispanAPIExtension ext = new EmbeddedInfinispanAPIExtension(1, false,
         new ConfigurationBuilder()
               .indexing().enable()
               .storage(LOCAL_HEAP)
               .addIndexedEntity(Book.class)
               .addIndexedEntity(Author.class)
               .addIndexedEntity(Review.class)
               .build());

   @Override
   protected InfinispanAPIExtension ext() {
      return ext;
   }
}
