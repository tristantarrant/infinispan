package org.infinispan.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.infinispan.api.model.Book;
import org.infinispan.api.sync.SyncCache;
import org.infinispan.api.sync.SyncQueryResult;
import org.junit.jupiter.api.Test;

public abstract class AbstractSyncCacheQueryTest extends AbstractAPITest {

   private static final String BOOK_FQN = Book.class.getName();

   @Test
   protected void testQueryAll() {
      SyncCache<String, Book> cache = ext().syncCache();
      cache.putAll(BOOKS);
      try (SyncQueryResult<Book> result = cache.<Book>query("from " + BOOK_FQN).find()) {
         List<Book> books = toList(result);
         assertEquals(4, books.size());
      }
   }

   @Test
   protected void testQueryByField() {
      SyncCache<String, Book> cache = ext().syncCache();
      cache.putAll(BOOKS);
      try (SyncQueryResult<Book> result = cache.<Book>query("from " + BOOK_FQN + " where title = 'Dune'").find()) {
         List<Book> books = toList(result);
         assertEquals(1, books.size());
         assertEquals("Dune", books.get(0).getTitle());
      }
   }

   @Test
   protected void testQueryWithParameter() {
      SyncCache<String, Book> cache = ext().syncCache();
      cache.putAll(BOOKS);
      try (SyncQueryResult<Book> result = cache.<Book>query("from " + BOOK_FQN + " where publicationYear > :year")
            .param("year", 1950).find()) {
         List<Book> books = toList(result);
         assertEquals(3, books.size());
      }
   }

   @Test
   protected void testQueryWithSkipAndLimit() {
      SyncCache<String, Book> cache = ext().syncCache();
      cache.putAll(BOOKS);
      try (SyncQueryResult<Book> result = cache.<Book>query("from " + BOOK_FQN + " order by title")
            .skip(1).limit(2).find()) {
         List<Book> books = toList(result);
         assertEquals(2, books.size());
      }
   }

   @Test
   protected void testFullTextQuery() {
      SyncCache<String, Book> cache = ext().syncCache();
      cache.putAll(BOOKS);
      try (SyncQueryResult<Book> result = cache.<Book>query("from " + BOOK_FQN + " where description : 'fantasy'").find()) {
         List<Book> books = toList(result);
         assertEquals(2, books.size());
      }
   }

   @Test
   protected void testQueryByEmbeddedField() {
      SyncCache<String, Book> cache = ext().syncCache();
      cache.putAll(BOOKS);
      try (SyncQueryResult<Book> result = cache.<Book>query("from " + BOOK_FQN + " where author.surname = :surname")
            .param("surname", "Tolkien").find()) {
         List<Book> books = toList(result);
         assertEquals(2, books.size());
      }
   }

   @Test
   protected void testQueryHitCount() {
      SyncCache<String, Book> cache = ext().syncCache();
      cache.putAll(BOOKS);
      try (SyncQueryResult<Book> result = cache.<Book>query("from " + BOOK_FQN).find()) {
         assertTrue(result.hitCount().isPresent());
         assertEquals(4, result.hitCount().getAsLong());
      }
   }

   @Test
   protected void testDeleteByQuery() {
      SyncCache<String, Book> cache = ext().syncCache();
      cache.putAll(BOOKS);
      int deleted = cache.<Book>query("delete from " + BOOK_FQN + " where publicationYear < :year")
            .param("year", 1950).execute();
      assertEquals(1, deleted);
      assertEquals(3, cache.estimateSize());
   }

   private static <T> List<T> toList(SyncQueryResult<T> result) {
      List<T> list = new ArrayList<>();
      result.results().forEach(list::add);
      return list;
   }
}
