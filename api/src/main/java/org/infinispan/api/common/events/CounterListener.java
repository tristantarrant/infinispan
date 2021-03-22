package org.infinispan.api.common.events;

/**
 * A listener interface to listen to counter changes.
 * <p>
 * The events received will have the previous/current value and its previous/current state.
 *
 * @author Pedro Ruivo
 * @since 9.0
 */
public interface CounterListener {

   void onUpdate(CounterEvent entry);
}
