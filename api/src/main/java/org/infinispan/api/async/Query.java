package org.infinispan.api.async;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

/**
 * Parameterized Query builder
 * @param <R> the result type for the query
 */
public interface Query<R> {
   /**
    * Sets the named parameter to the specified value
    * @param name
    * @param value
    * @return
    */
   Query param(String name, Object value);

   /**
    * Skips the first specified number of results
    * @param skip
    * @return
    */
   Query skip(long skip);

   /**
    * Limits the number of results
    * @param limit
    * @return
    */
   Query limit(int limit);

   /**
    * Executes the query
    */
   Flow.Publisher<R> find();

   /**
    * Removes all entries which match the query.
    *
    * @return
    */
   CompletionStage<Void> remove();

   /**
    * Updates entries using a {@link Consumer}.
    * If the cache is embedded, the consumer will be executed locally on the owner of the entry.
    * If the cache is remote, entries will be retrieved, manipulated locally and put back.
    */
   CompletionStage<Void> update(Consumer<Map.Entry> entryConsumer);

   /**
    * Updates entries using a named task. The task must be an EntryConsumerTask.
    */
   CompletionStage<Void> update(String taskName, Object... args);
}
