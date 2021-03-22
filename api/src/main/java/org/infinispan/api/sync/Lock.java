package org.infinispan.api.sync;

import java.util.concurrent.TimeUnit;

/**
 *
 * @since 13.0
 **/
public interface Lock {

   String name();

   void lock();

   boolean tryLock();

   boolean tryLock(long time, TimeUnit unit);

   void unlock();

   boolean isLocked();

   boolean isLockedByMe();
}
