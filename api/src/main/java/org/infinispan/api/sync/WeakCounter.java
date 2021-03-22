package org.infinispan.api.sync;

/**
 *
 * @since 13.0
 **/
public interface WeakCounter {

   long value();

   /**
    * Increments the counter.
    */
   default void increment() {
      add(1L);
   }


   /**
    * Decrements the counter.
    */
   default void decrement() {
      add(-1L);
   }

   /**
    * Adds the given value to the new value.
    *
    * @param delta the value to add.
    */
   void add(long delta);

   /**
    * Resets the counter to its initial value.
    */
   void reset();

   /**
    * Returns the name of this counter
    *
    * @return the name of this counter
    */
   String name();

   /**
    * Returns an async version of this counter
    *
    * @return
    */
   org.infinispan.api.async.WeakCounter async();

   /**
    * Returns a reactive version of this counter
    *
    * @return
    */
   org.infinispan.api.mutiny.WeakCounter reactive();
}
