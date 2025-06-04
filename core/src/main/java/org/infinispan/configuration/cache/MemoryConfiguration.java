package org.infinispan.configuration.cache;

import static org.infinispan.util.logging.Log.CONFIG;

import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.configuration.attributes.ConfigurationElement;
import org.infinispan.commons.util.ByteQuantity;
import org.infinispan.configuration.parsing.Attribute;
import org.infinispan.configuration.parsing.Element;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.eviction.EvictionType;

/**
 * Controls the memory storage configuration for the cache.
 *
 * @author William Burns
 */
public class MemoryConfiguration extends ConfigurationElement<MemoryConfiguration> {

   public static final AttributeDefinition<StorageType> STORAGE = AttributeDefinition.builder(Attribute.STORAGE, StorageType.HEAP).immutable().build();
   public static final AttributeDefinition<String> MAX_SIZE = AttributeDefinition.builder(Attribute.MAX_SIZE, null, String.class).matcher((a1, a2) -> maxSizeToBytes(a1.get()) == maxSizeToBytes(a2.get())).build();
   public static final AttributeDefinition<Long> MAX_COUNT = AttributeDefinition.builder(Attribute.MAX_COUNT, -1L).build();
   public static final AttributeDefinition<EvictionStrategy> WHEN_FULL = AttributeDefinition.builder(Attribute.WHEN_FULL, EvictionStrategy.NONE).immutable().build();

   static AttributeSet attributeDefinitionSet() {
      return new AttributeSet(MemoryConfiguration.class, STORAGE, MAX_SIZE, MAX_COUNT, WHEN_FULL);
   }

   MemoryConfiguration(AttributeSet attributes) {
      super(Element.MEMORY, attributes);
   }

   /**
    * @return true if the storage is off-heap
    */
   public boolean isOffHeap() {
      return attributes.attribute(STORAGE).get() == StorageType.OFF_HEAP;
   }

   /**
    * @return The max size in bytes or -1 if not configured.
    */
   public long maxSizeBytes() {
      return maxSizeToBytes(maxSize());
   }

   public String maxSize() {
      return attributes.attribute(MAX_SIZE).get();
   }

   public void maxSize(String maxSize) {
      if (!isSizeBounded()) throw CONFIG.cannotChangeMaxSize();
      attributes.attribute(MAX_SIZE).set(maxSize);
   }

   /**
    * @return the max number of entries in memory or -1 if not configured.
    */
   public long maxCount() {
      return attributes.attribute(MAX_COUNT).get();
   }

   public void maxCount(long maxCount) {
      if (!isCountBounded()) throw CONFIG.cannotChangeMaxCount();
      attributes.attribute(MAX_COUNT).set(maxCount);
   }

   /**
    * @return The memory {@link StorageType}.
    */
   public StorageType storage() {
      return attributes.attribute(STORAGE).get();
   }

   /**
    * Size of the eviction, -1 if disabled
    * @deprecated Since 11.0, use {@link #maxCount()} or {@link #maxSize()} to obtain
    * either the maximum number of entries or the maximum size of the data container.
    */
   @Deprecated(forRemoval=true, since = "11.0")
   public long size() {
      throw new UnsupportedOperationException();
   }

   /**
    * The configured eviction type
    *
    * @deprecated Since 11.0, use {@link #maxCount()} or {@link #maxSize()} to obtain either the maximum number of
    *       entries or the maximum size of the data container.
    */
   @Deprecated(forRemoval=true, since = "11.0")
   public EvictionType evictionType() {
      throw new UnsupportedOperationException();
   }

   /**
    * @return The configured {@link EvictionStrategy}.
    */
   public EvictionStrategy whenFull() {
      return attributes.attribute(WHEN_FULL).get();
   }

   /**
    * Returns whether remove eviction is in use
    */
   public boolean isEvictionEnabled() {
      return (isSizeBounded() || isCountBounded()) && whenFull().isRemovalBased();
   }

   private boolean isSizeBounded() {
      return maxSize() != null;
   }

   private boolean isCountBounded() {
      return maxCount() > 0;
   }

   static long maxSizeToBytes(String maxSizeStr) {
      return maxSizeStr != null ? ByteQuantity.parse(maxSizeStr) : -1;
   }
}
