package org.infinispan.api.mutiny;

/**
 * @since 13.0
 **/
public interface Mutiny {
   Caches caches();

   StrongCounters strongCounters();

   WeakCounters weakCounters();

   Locks locks();
}
