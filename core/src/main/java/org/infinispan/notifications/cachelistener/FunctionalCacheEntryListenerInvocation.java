package org.infinispan.notifications.cachelistener;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import org.infinispan.encoding.DataConversion;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.infinispan.notifications.cachelistener.event.Event;
import org.infinispan.notifications.cachelistener.filter.CacheEventConverter;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.infinispan.notifications.impl.ListenerInvocation;

/**
 * A {@link CacheEntryListenerInvocation} backed by a lambda-based {@link ListenerInvocation}, without
 * filter/converter/DataConversion support.
 *
 * @since 15.1
 */
public final class FunctionalCacheEntryListenerInvocation<K, V> implements CacheEntryListenerInvocation<K, V> {
   private final ListenerInvocation<Event<K, V>> invocation;
   private final Class<? extends Annotation> annotation;
   private final boolean onlyPrimary;
   private final boolean clustered;
   private final UUID identifier;
   private final boolean sync;
   private final Listener.Observation observation;

   public FunctionalCacheEntryListenerInvocation(ListenerInvocation<Event<K, V>> invocation,
                                                  Class<? extends Annotation> annotation,
                                                  boolean onlyPrimary, boolean clustered,
                                                  UUID identifier, boolean sync,
                                                  Listener.Observation observation) {
      this.invocation = invocation;
      this.annotation = annotation;
      this.onlyPrimary = onlyPrimary;
      this.clustered = clustered;
      this.identifier = identifier;
      this.sync = sync;
      this.observation = observation;
   }

   @Override
   public CompletionStage<Void> invoke(Event<K, V> event) {
      if (observation.shouldInvoke(event.isPre())) {
         return invocation.invoke(event);
      }
      return null;
   }

   @Override
   public CompletionStage<Void> invoke(EventWrapper<K, V, CacheEntryEvent<K, V>> event, boolean isLocalNodePrimaryOwner) {
      if (onlyPrimary && !isLocalNodePrimaryOwner) return null;
      if (!observation.shouldInvoke(event.getEvent().isPre())) return null;
      return invocation.invoke(event.getEvent());
   }

   @Override
   public CompletionStage<Void> invokeNoChecks(EventWrapper<K, V, CacheEntryEvent<K, V>> wrappedEvent,
                                                boolean skipQueue, boolean skipConverter, boolean needsTransform) {
      return invocation.invoke(wrappedEvent.getEvent());
   }

   @Override
   public Object getTarget() {
      return invocation.getTarget();
   }

   @Override
   public boolean isClustered() {
      return clustered;
   }

   @Override
   public boolean isSync() {
      return sync;
   }

   @Override
   public UUID getIdentifier() {
      return identifier;
   }

   @Override
   public Listener.Observation getObservation() {
      return observation;
   }

   @Override
   public Class<? extends Annotation> getAnnotation() {
      return annotation;
   }

   @Override
   public CacheEventFilter<? super K, ? super V> getFilter() {
      return null;
   }

   @Override
   public <C> CacheEventConverter<? super K, ? super V, C> getConverter() {
      return null;
   }

   @Override
   public Set<Class<? extends Annotation>> getFilterAnnotations() {
      return Collections.emptySet();
   }

   @Override
   public DataConversion getKeyDataConversion() {
      return null;
   }

   @Override
   public DataConversion getValueDataConversion() {
      return null;
   }

   @Override
   public boolean useStorageFormat() {
      return false;
   }
}
