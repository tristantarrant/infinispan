package org.infinispan.api.async;

/**
 * @since 13.0
 **/
public interface Async {

   Caches caches();

   MultiMap multiMaps();

   StrongCounters strongCounters();

   WeakCounters weakCounters();

   Locks locks();
}
