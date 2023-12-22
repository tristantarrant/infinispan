package org.infinispan.api.async;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * @since 14.0
 */
public interface AsyncLock {

   /**
    * Returns the name of this lock.
    *
    * @return the lock name
    */
   String name();

   /**
    * Returns the container of this lock.
    *
    * @return the container
    */
   AsyncContainer container();

   /**
    * Acquires the lock asynchronously, completing when it becomes available.
    *
    * @return a {@link CompletionStage} that completes when the lock is acquired
    */
   CompletionStage<Void> lock();

   /**
    * Attempts to acquire the lock without blocking.
    *
    * @return a {@link CompletionStage} completing with {@code true} if the lock was acquired, {@code false} otherwise
    */
   CompletionStage<Boolean> tryLock();

   /**
    * Attempts to acquire the lock, waiting up to the specified time.
    *
    * @param time the maximum time to wait
    * @param unit the time unit
    * @return a {@link CompletionStage} completing with {@code true} if the lock was acquired, {@code false} if the wait
    * time elapsed
    */
   CompletionStage<Boolean> tryLock(long time, TimeUnit unit);

   /**
    * Releases the lock asynchronously.
    *
    * @return a {@link CompletionStage} that completes when the lock is released
    */
   CompletionStage<Void> unlock();

   /**
    * Returns whether this lock is currently held by any owner.
    *
    * @return a {@link CompletionStage} completing with {@code true} if the lock is held
    */
   CompletionStage<Boolean> isLocked();

   /**
    * Returns whether this lock is currently held by the current owner.
    *
    * @return a {@link CompletionStage} completing with {@code true} if the lock is held by the current owner
    */
   CompletionStage<Boolean> isLockedByMe();
}
