package org.infinispan.embedded;

import java.util.OptionalLong;
import java.util.concurrent.Flow;

import org.infinispan.api.async.AsyncQueryResult;
import org.infinispan.commons.api.query.QueryResult;
import org.reactivestreams.FlowAdapters;

import io.reactivex.rxjava3.core.Flowable;

/**
 * @param <R>
 * @since 16.3
 */
public class EmbeddedAsyncQueryResult<R> implements AsyncQueryResult<R> {
   private final QueryResult<R> result;

   EmbeddedAsyncQueryResult(QueryResult<R> result) {
      this.result = result;
   }

   @Override
   public OptionalLong hitCount() {
      return OptionalLong.of(result.count().value());
   }

   @Override
   public Flow.Publisher<R> results() {
      return FlowAdapters.toFlowPublisher(Flowable.fromIterable(result.list()));
   }

   @Override
   public void close() {
   }
}
