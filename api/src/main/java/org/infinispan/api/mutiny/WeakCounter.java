package org.infinispan.api.mutiny;

import io.smallrye.mutiny.Uni;

/**
 *
 * @since 13.0
 **/
public interface WeakCounter {

   Uni<Long> value();

   default Uni<Void> increment() {
      return add(1);
   }

   default Uni<Void> decrement() {
      return add(-1);
   }

   Uni<Void> add(long delta);

   /**
    * Returns the name of this counter
    *
    * @return the name of this counter
    */
   String name();

   /**
    * Returns an sync version of this counter
    *
    * @return
    */
   org.infinispan.api.sync.WeakCounter sync();

   /**
    * Returns an async version of this counter
    *
    * @return
    */
   org.infinispan.api.async.WeakCounter async();
}
