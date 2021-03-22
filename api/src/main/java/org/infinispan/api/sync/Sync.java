package org.infinispan.api.sync;

/**
 * @since 13.0
 **/
public interface Sync {

   Caches caches();

   StrongCounters strongCounters();

   WeakCounters weakCounters();

   Locks locks();
}
