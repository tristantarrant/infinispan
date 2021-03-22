package org.infinispan.api.async;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import org.infinispan.api.configuration.LockConfiguration;

/**
 * @since 13.0
 **/
public interface Locks {
   CompletionStage<Lock> create(String name, LockConfiguration configuration);

   CompletionStage<Lock> lock(String name);

   CompletionStage<Void> remove(String name);

   Flow.Publisher<String> names();
}
