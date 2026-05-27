package org.infinispan.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import org.infinispan.api.async.AsyncCache;
import org.infinispan.api.async.AsyncQueryResult;
import org.infinispan.api.model.Book;
import org.junit.jupiter.api.Test;

public abstract class AbstractAsyncCacheQueryTest extends AbstractAPITest {

   private static final String BOOK_FQN = Book.class.getName();

   @Test
   protected void testQueryAll() {
      AsyncCache<String, Book> cache = ext().asyncCache();
      cache.putAll(BOOKS).toCompletableFuture().join();
      AsyncQueryResult<Book> result = cache.<Book>query("from " + BOOK_FQN).find().toCompletableFuture().join();
      List<Book> books = collect(result.results());
      assertEquals(4, books.size());
   }

   @Test
   protected void testQueryByField() {
      AsyncCache<String, Book> cache = ext().asyncCache();
      cache.putAll(BOOKS).toCompletableFuture().join();
      AsyncQueryResult<Book> result = cache.<Book>query("from " + BOOK_FQN + " where title = 'Dune'")
            .find().toCompletableFuture().join();
      List<Book> books = collect(result.results());
      assertEquals(1, books.size());
      assertEquals("Dune", books.get(0).getTitle());
   }

   @Test
   protected void testQueryWithParameter() {
      AsyncCache<String, Book> cache = ext().asyncCache();
      cache.putAll(BOOKS).toCompletableFuture().join();
      AsyncQueryResult<Book> result = cache.<Book>query("from " + BOOK_FQN + " where publicationYear > :year")
            .param("year", 1950).find().toCompletableFuture().join();
      List<Book> books = collect(result.results());
      assertEquals(3, books.size());
   }

   @Test
   protected void testQueryWithSkipAndLimit() {
      AsyncCache<String, Book> cache = ext().asyncCache();
      cache.putAll(BOOKS).toCompletableFuture().join();
      AsyncQueryResult<Book> result = cache.<Book>query("from " + BOOK_FQN + " order by title")
            .skip(1).limit(2).find().toCompletableFuture().join();
      List<Book> books = collect(result.results());
      assertEquals(2, books.size());
   }

   @Test
   protected void testFullTextQuery() {
      AsyncCache<String, Book> cache = ext().asyncCache();
      cache.putAll(BOOKS).toCompletableFuture().join();
      AsyncQueryResult<Book> result = cache.<Book>query("from " + BOOK_FQN + " where description : 'fantasy'")
            .find().toCompletableFuture().join();
      List<Book> books = collect(result.results());
      assertEquals(2, books.size());
   }

   @Test
   protected void testQueryByEmbeddedField() {
      AsyncCache<String, Book> cache = ext().asyncCache();
      cache.putAll(BOOKS).toCompletableFuture().join();
      AsyncQueryResult<Book> result = cache.<Book>query("from " + BOOK_FQN + " where author.surname = :surname")
            .param("surname", "Tolkien").find().toCompletableFuture().join();
      List<Book> books = collect(result.results());
      assertEquals(2, books.size());
   }

   @Test
   protected void testQueryHitCount() {
      AsyncCache<String, Book> cache = ext().asyncCache();
      cache.putAll(BOOKS).toCompletableFuture().join();
      AsyncQueryResult<Book> result = cache.<Book>query("from " + BOOK_FQN).find().toCompletableFuture().join();
      assertTrue(result.hitCount().isPresent());
      assertEquals(4, result.hitCount().getAsLong());
   }

   @Test
   protected void testDeleteByQuery() {
      AsyncCache<String, Book> cache = ext().asyncCache();
      cache.putAll(BOOKS).toCompletableFuture().join();
      long deleted = cache.<Book>query("delete from " + BOOK_FQN + " where publicationYear < :year")
            .param("year", 1950).execute().toCompletableFuture().join();
      assertEquals(1, deleted);
      assertEquals(3, cache.estimateSize().toCompletableFuture().join());
   }

   protected static <T> List<T> collect(Flow.Publisher<T> publisher) {
      CompletableFuture<List<T>> future = new CompletableFuture<>();
      List<T> items = new ArrayList<>();
      publisher.subscribe(new Flow.Subscriber<>() {
         @Override
         public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
         }

         @Override
         public void onNext(T item) {
            items.add(item);
         }

         @Override
         public void onError(Throwable throwable) {
            future.completeExceptionally(throwable);
         }

         @Override
         public void onComplete() {
            future.complete(items);
         }
      });
      return future.join();
   }
}
