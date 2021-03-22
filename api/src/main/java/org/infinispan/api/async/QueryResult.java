package org.infinispan.api.async;

import java.util.OptionalLong;
import java.util.concurrent.Flow;

/**
 * @since 13.0
 **/
public interface QueryResult<R> extends AutoCloseable {
   OptionalLong hitCount();

   Flow.Publisher<R> results();

   void close();
}
