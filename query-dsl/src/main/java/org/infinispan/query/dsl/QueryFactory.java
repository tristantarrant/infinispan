package org.infinispan.query.dsl;

/**
 * <p><b>NOTE:</b> Most methods in this class are deprecated, except {@link #create(java.lang.String)}. Please do not
 * use any of the deprecated methods or else you will experience difficulties in porting your code to the new query API
 * that will be introduced by Infinispan 12.
 *
 * @author anistor@redhat.com
 * @since 6.0
 */
@Deprecated(since = "16.0", forRemoval = true)
public interface QueryFactory {

   /**
    * Creates a Query based on an Ickle query string.
    *
    * @return a query
    * @deprecated use {@link org.infinispan.commons.api.BasicCache#query(String)} instead
    */
   <T> Query<T> create(String queryString);

}
