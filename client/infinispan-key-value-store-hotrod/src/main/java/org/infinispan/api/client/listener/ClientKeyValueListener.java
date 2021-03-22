package org.infinispan.api.client.listener;

import java.util.Arrays;
import java.util.List;

import org.infinispan.api.Experimental;
import org.infinispan.api.mutiny.EntryStatus;
import org.infinispan.api.common.events.KeyValueListener;

@Experimental
public final class ClientKeyValueListener implements KeyValueListener {
   private final boolean listenUpdated;
   private final boolean listenCreated;
   private final boolean listenDeleted;

   private ClientKeyValueListener(EntryStatus[] types) {
      List<EntryStatus> eventTypes = Arrays.asList(types);
      listenCreated = eventTypes.contains(EntryStatus.CREATED);
      listenUpdated = eventTypes.contains(EntryStatus.UPDATED);
      listenDeleted = eventTypes.contains(EntryStatus.DELETED);
   }

   public static KeyValueListener create() {
      return new ClientKeyValueListener();
   }

   public static KeyValueListener create(EntryStatus... status) {
      return new ClientKeyValueListener(status);
   }

   private ClientKeyValueListener() {
      listenCreated = true;
      listenUpdated = true;
      listenDeleted = true;
   }

   public boolean isListenCreated() {
      return listenCreated;
   }

   public boolean isListenUpdated() {
      return listenUpdated;
   }

   public boolean isListenDeleted() {
      return listenDeleted;
   }
}
