package org.infinispan.api.sync;

import java.util.Iterator;
import java.util.OptionalLong;

/**
 * @since 13.0
 **/
public interface QueryResult<R> extends AutoCloseable {
   OptionalLong hitCount();

   Iterator<R> results();

   void close();
}
