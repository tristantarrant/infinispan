package org.infinispan.api.mutiny;

import org.infinispan.api.Infinispan;
import org.infinispan.api.common.events.container.ContainerEvent;
import org.infinispan.api.common.events.container.ContainerListenerEventType;
import org.infinispan.api.sync.events.cache.SyncCacheEntryListener;

import io.smallrye.mutiny.Multi;

/**
 * @since 14.0
 **/
public interface MutinyContainer extends Infinispan {
   MutinyCaches caches();

   MutinyStrongCounters strongCounters();

   MutinyWeakCounters weakCounters();

   MutinyLocks locks();

   /**
    * Listens to the {@link SyncCacheEntryListener}
    *
    * @param types
    * @return
    */
   Multi<ContainerEvent> listen(ContainerListenerEventType... types);
}
