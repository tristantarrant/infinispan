package org.infinispan.api.common.events;

/**
 * @since 13.0
 **/
public interface ListenerHandle<T> {
   T get();

   void remove();
}
