package org.infinispan.api.common.events;

import java.util.concurrent.CompletionStage;

/**
 * @since 13.0
 **/
public interface AsyncListenerHandle<T> {
   T get();

   CompletionStage<Void> remove();
}
