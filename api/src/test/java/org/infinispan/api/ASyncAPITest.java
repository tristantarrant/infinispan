package org.infinispan.api;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;

import org.infinispan.api.async.Cache;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 13.0
 **/
public class ASyncAPITest {
   public void testAPI() throws ExecutionException, InterruptedException {
      try (Infinispan infinispan = Infinispan.create("file:///path/to/infinispan.xml")) {
         CompletionStage<Cache<String, String>> mycache = infinispan.async().caches().get("mycache");
         mycache.thenCompose(cache -> cache.put("key", "value")).toCompletableFuture().get();
         mycache.thenAccept(cache -> cache.keys().subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {}

            @Override
            public void onNext(String item) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
         }));
      }
   }
}
