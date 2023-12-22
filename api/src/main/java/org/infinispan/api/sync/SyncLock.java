package org.infinispan.api.sync;

import java.util.concurrent.TimeUnit;

/**
 * @since 14.0
 **/
public interface SyncLock {

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
   SyncContainer container();

   /**
    * Acquires the lock, blocking until it becomes available.
    */
   void lock();

   /**
    * Attempts to acquire the lock without blocking.
    *
    * @return {@code true} if the lock was acquired, {@code false} otherwise
    */
   boolean tryLock();

   /**
    * Attempts to acquire the lock, waiting up to the specified time.
    *
    * @param time the maximum time to wait
    * @param unit the time unit
    * @return {@code true} if the lock was acquired, {@code false} if the wait time elapsed
    */
   boolean tryLock(long time, TimeUnit unit);

   /**
    * Releases the lock.
    */
   void unlock();

   /**
    * Returns whether this lock is currently held by any owner.
    *
    * @return {@code true} if the lock is held
    */
   boolean isLocked();

   /**
    * Returns whether this lock is currently held by the current owner.
    *
    * @return {@code true} if the lock is held by the current owner
    */
   boolean isLockedByMe();
}
