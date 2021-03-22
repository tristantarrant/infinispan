package org.infinispan.api.sync;

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
   Iterable<QueryResult<R>> find();

   /**
    * Removes all entries which match the query
    */
   void remove();

   /**
    * Processes entries matching the query
    */
   Iterable<QueryResult<R>> process();
}
