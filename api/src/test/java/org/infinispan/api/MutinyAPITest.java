package org.infinispan.api;

import org.infinispan.api.common.KeyValueEntry;
import org.infinispan.api.common.events.KeyValueRemovedListener;
import org.infinispan.api.mutiny.Cache;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Uni;

/**
 * @since 13.0
 **/
public class MutinyAPITest {
   public void testAPI() {
      try (Infinispan infinispan = Infinispan.create("file:///path/to/infinispan.xml")) {
         Uni<Cache<String, String>> uni = infinispan.mutiny().caches().cache("mycache");
         uni.onItem().invoke(c -> c.put("k", "v")).subscribe();
         uni.onItem().transformToMulti(c -> c.query("...").limit(100).find());
         uni.onItem().transformToMulti(c -> c.listen((KeyValueRemovedListener) event -> { })).subscribe(new Subscriber<KeyValueEntry<String, String>>() {
            @Override
            public void onSubscribe(Subscription subscription) {
               subscription.cancel();
            }

            @Override
            public void onNext(KeyValueEntry<String, String> stringStringKeyValueEntry) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
         });

      }
   }
}
