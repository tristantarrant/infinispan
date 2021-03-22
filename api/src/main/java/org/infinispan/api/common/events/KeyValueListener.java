package org.infinispan.api.common.events;

/**
 * @since 13.0
 */
public interface KeyValueListener<K, V> {

   default boolean primaryOnly() {
      return false;
   }

   default boolean clustered() {
      return false;
   }

   default boolean includeCurrentState() {
      return false;
   }

   default Observation observation() {
      return Observation.BOTH;
   }

   enum Observation {
      PRE() {
         @Override
         public boolean shouldInvoke(boolean pre) {
            return pre;
         }
      },
      POST() {
         @Override
         public boolean shouldInvoke(boolean pre) {
            return !pre;
         }
      },
      BOTH() {
         @Override
         public boolean shouldInvoke(boolean pre) {
            return true;
         }
      };

      public abstract boolean shouldInvoke(boolean pre);
   }
}
