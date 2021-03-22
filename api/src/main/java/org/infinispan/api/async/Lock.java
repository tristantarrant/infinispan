package org.infinispan.api.async;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public interface Lock {
   String name();

   CompletionStage<Void> lock();

   CompletionStage<Boolean> tryLock();

   CompletionStage<Boolean> tryLock(long time, TimeUnit unit);

   CompletionStage<Void> unlock();

   CompletionStage<Boolean> isLocked();

   CompletionStage<Boolean> isLockedByMe();
}
