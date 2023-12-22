package org.infinispan.embedded.impl;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.infinispan.AdvancedCache;
import org.infinispan.api.common.events.cache.CacheEntryEvent;
import org.infinispan.api.common.events.cache.CacheListenerOptions;
import org.infinispan.api.sync.events.cache.SyncCacheListener;
import org.infinispan.cache.impl.InternalCache;
import org.infinispan.commons.util.Util;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.CacheEntryListenerInvocation;
import org.infinispan.notifications.cachelistener.CacheNotifierImpl;
import org.infinispan.notifications.cachelistener.FunctionalCacheEntryListenerInvocation;
import org.infinispan.notifications.cachelistener.FunctionalListenerInvocation;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryExpired;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.Event;
import org.infinispan.notifications.cachelistener.event.impl.EventImpl;

/**
 * Embedded implementation of {@link SyncCacheListener} that bridges the new builder-based
 * listener API to the internal {@link CacheNotifierImpl} infrastructure.
 *
 * @since 16.3
 */
public class EmbeddedSyncCacheListener<K, V> extends SyncCacheListener<K, V> {
   private final AdvancedCache<K, V> cache;

   public EmbeddedSyncCacheListener(AdvancedCache<K, V> cache) {
      this.cache = cache;
   }

   @Override
   public Closeable install() {
      if (options.isIncludeCurrentState()) {
         throw new UnsupportedOperationException("includeCurrentState is not yet supported");
      }

      @SuppressWarnings("unchecked")
      CacheNotifierImpl<K, V> notifier = (CacheNotifierImpl<K, V>)
            ((InternalCache<K, V>) cache).getComponentRegistry().getCacheNotifier().running();

      Object target = new Object();
      UUID identifier = Util.threadLocalRandomUUID();
      boolean onlyPrimary = options.isPrimaryOnly();
      boolean clustered = options.isClustered();
      Listener.Observation observation = mapObservation(options.getObservation());

      List<Class<? extends Annotation>> registeredAnnotations = new ArrayList<>(4);

      registerIfPresent(notifier, target, identifier, onlyPrimary, clustered, observation,
            onCreate, CacheEntryCreated.class, registeredAnnotations);
      registerIfPresent(notifier, target, identifier, onlyPrimary, clustered, observation,
            onUpdate, CacheEntryModified.class, registeredAnnotations);
      registerIfPresent(notifier, target, identifier, onlyPrimary, clustered, observation,
            onRemove, CacheEntryRemoved.class, registeredAnnotations);
      registerIfPresent(notifier, target, identifier, onlyPrimary, clustered, observation,
            onExpired, CacheEntryExpired.class, registeredAnnotations);

      return () -> {
         for (Class<? extends Annotation> annotation : registeredAnnotations) {
            notifier.getListenerCollectionForAnnotation(annotation)
                  .removeIf(inv -> target.equals(inv.getTarget()));
         }
      };
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   private void registerIfPresent(CacheNotifierImpl<K, V> notifier, Object target, UUID identifier,
                                  boolean onlyPrimary, boolean clustered, Listener.Observation observation,
                                  Consumer callback,
                                  Class<? extends Annotation> annotationClass,
                                  List<Class<? extends Annotation>> registeredAnnotations) {
      if (callback == null) {
         return;
      }
      Consumer<Event<K, V>> wrappedConsumer = event -> {
         if (event instanceof EventImpl) {
            CacheEntryEvent apiEvent = new EmbeddedCacheEntryEvent<>((EventImpl<K, V>) event);
            if (converter != null) {
               apiEvent = new ConvertedCacheEntryEvent<>(apiEvent, (Function) converter);
            }
            if (filter != null && !filter.test(apiEvent)) {
               return;
            }
            callback.accept(apiEvent);
         }
      };
      FunctionalListenerInvocation<K, V> listenerInvocation =
            FunctionalListenerInvocation.sync(target, wrappedConsumer);
      CacheEntryListenerInvocation<K, V> invocation = new FunctionalCacheEntryListenerInvocation<>(
            listenerInvocation, annotationClass, onlyPrimary, clustered, identifier, true, observation);
      notifier.getListenerCollectionForAnnotation(annotationClass).add(invocation);
      registeredAnnotations.add(annotationClass);
   }

   private static Listener.Observation mapObservation(CacheListenerOptions.Observation observation) {
      if (observation == null) {
         return Listener.Observation.POST;
      }
      return switch (observation) {
         case PRE -> Listener.Observation.PRE;
         case POST -> Listener.Observation.POST;
         case BOTH -> Listener.Observation.BOTH;
      };
   }
}
