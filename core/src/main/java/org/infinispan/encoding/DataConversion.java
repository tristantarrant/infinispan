package org.infinispan.encoding;

import java.util.Objects;

import org.infinispan.commons.dataconversion.IdentityEncoder;
import org.infinispan.commons.dataconversion.IdentityWrapper;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.dataconversion.Transcoder;
import org.infinispan.commons.dataconversion.Wrapper;
import org.infinispan.commons.dataconversion.WrapperIds;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.encoding.impl.StorageConfigurationManager;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.marshall.core.EncoderRegistry;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoReserved;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * Handle conversions for Keys or values.
 *
 * @since 9.2
 */
@ProtoTypeId(ProtoStreamTypeIds.DATA_CONVERSION)
@ProtoReserved(numbers = 2, names = "encoderId")
@Scope(Scopes.NONE)
public final class DataConversion {

   /**
    * @deprecated Since 11.0. To be removed in 14.0. For internal use only.
    */
   @Deprecated(forRemoval=true, since = "11.0")
   public static final DataConversion IDENTITY_KEY = new DataConversion(IdentityWrapper.INSTANCE, true);
   /**
    * @deprecated Since 11.0. To be removed in 14.0. For internal use only.
    */
   @Deprecated(forRemoval=true, since = "11.0")
   public static final DataConversion IDENTITY_VALUE = new DataConversion(IdentityWrapper.INSTANCE, false);

   // On the origin node the conversion is initialized with the encoder/wrapper classes, on remote nodes with the ids
   private final transient Class<? extends Wrapper> wrapperClass;
   private final byte wrapperId;
   private final MediaType requestMediaType;
   private final boolean isKey;

   private transient MediaType storageMediaType;
   private transient Wrapper customWrapper;
   private transient Transcoder transcoder;
   private transient EncoderRegistry encoderRegistry;
   private transient StorageConfigurationManager storageConfigurationManager;

   private DataConversion(Class<? extends Wrapper> wrapperClass,
                          MediaType requestMediaType, boolean isKey) {
      this.wrapperClass = wrapperClass;
      this.requestMediaType = requestMediaType;
      this.isKey = isKey;
      this.wrapperId = WrapperIds.NO_WRAPPER;
   }

   /**
    * Used for de-serialization
    */
   private DataConversion(Byte wrapperId, MediaType requestMediaType, boolean isKey) {
      this.wrapperId = wrapperId;
      this.requestMediaType = requestMediaType;
      this.isKey = isKey;
      this.wrapperClass = null;
   }

   private DataConversion(Wrapper wrapper, boolean isKey) {
      this.customWrapper = wrapper;
      this.wrapperClass = wrapper.getClass();
      this.isKey = isKey;
      this.storageMediaType = MediaType.APPLICATION_OBJECT;
      this.requestMediaType = MediaType.APPLICATION_OBJECT;
      this.wrapperId = WrapperIds.NO_WRAPPER;
   }

   @ProtoFactory
   static DataConversion protoFactory(boolean isKey, byte wrapperId, MediaType mediaType) {
      return new DataConversion(wrapperId, mediaType, isKey);
   }

   @ProtoField(1)
   boolean getIsKey() {
      return isKey;
   }

   @ProtoField(3)
   byte getWrapperId() {
      return customWrapper != null ? customWrapper.id() : WrapperIds.NO_WRAPPER;
   }

   @ProtoField(4)
   MediaType getMediaType() {
      return requestMediaType;
   }

   public DataConversion withRequestMediaType(MediaType requestMediaType) {
      if (Objects.equals(this.requestMediaType, requestMediaType)) return this;
      return new DataConversion(this.wrapperClass, requestMediaType, this.isKey);
   }

   @Inject
   void injectDependencies(StorageConfigurationManager storageConfigurationManager, EncoderRegistry encoderRegistry) {
      if (this == IDENTITY_KEY || this == IDENTITY_VALUE) {
         return;
      }
      this.storageMediaType = storageConfigurationManager.getStorageMediaType(isKey);
      this.encoderRegistry = encoderRegistry;
      this.storageConfigurationManager = storageConfigurationManager;
      this.customWrapper = encoderRegistry.getWrapper(wrapperClass, wrapperId);
      this.lookupTranscoder();
   }

   private void lookupTranscoder() {
      boolean needsTranscoding = storageMediaType != null && requestMediaType != null && !requestMediaType.matchesAll() && !requestMediaType.equals(storageMediaType);
      if (needsTranscoding) {
            transcoder = encoderRegistry.getTranscoder(requestMediaType, storageMediaType);
      }
   }

   public Object fromStorage(Object stored) {
      if (stored == null) return null;
      Object fromStorage = getWrapper().unwrap(stored);
      return transcoder == null ? fromStorage : transcoder.transcode(fromStorage, storageMediaType, requestMediaType);
   }

   public Object toStorage(Object toStore) {
      if (toStore == null) return null;
      toStore = transcoder == null ? toStore : transcoder.transcode(toStore, requestMediaType, storageMediaType);
      return getWrapper().wrap(toStore);
   }

   /**
    * Convert the stored object in a format suitable to be indexed.
    */
   public Object extractIndexable(Object stored, boolean javaEmbeddedEntities) {
      if (stored == null) return null;

      // Keys are indexed as stored, without the wrapper
      Wrapper wrapper = getWrapper();
      if (isKey) return wrapper.unwrap(stored);

      if (wrapper.isFilterable() && !javaEmbeddedEntities) {
         // If the value wrapper is indexable, return the already wrapped value or wrap it otherwise
         return stored.getClass() == wrapperClass ? stored : wrapper.wrap(stored);
      }

      // Otherwise convert to the request format
      Object unencoded = wrapper.unwrap(stored);
      return transcoder == null ? unencoded : transcoder.transcode(unencoded, storageMediaType, requestMediaType);
   }

   public MediaType getRequestMediaType() {
      return requestMediaType;
   }

   public MediaType getStorageMediaType() {
      return storageMediaType;
   }

   /**
    * @deprecated Since 11.0. To be removed in 14.0, with no replacement.
    */
   @Deprecated(forRemoval=true, since = "11.0")
   public Wrapper getWrapper() {
      if (customWrapper != null)
         return customWrapper;

      return storageConfigurationManager.getWrapper(isKey);
   }
   /**
    * @return A new instance with an {@link IdentityEncoder} and request type {@link MediaType#APPLICATION_OBJECT}.
    * @since 11.0
    */
   public static DataConversion newKeyDataConversion() {
      return new DataConversion(WrapperIds.NO_WRAPPER, MediaType.APPLICATION_OBJECT, true);
   }

   /**
    * @return A new instance with an {@link IdentityEncoder} and request type {@link MediaType#APPLICATION_OBJECT}.
    * @since 11.0
    */
   public static DataConversion newValueDataConversion() {
      return new DataConversion(WrapperIds.NO_WRAPPER, MediaType.APPLICATION_OBJECT, false);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      DataConversion that = (DataConversion) o;
      return isKey == that.isKey &&
            Objects.equals(customWrapper, that.customWrapper) &&
            Objects.equals(transcoder, that.transcoder) &&
            Objects.equals(requestMediaType, that.requestMediaType);
   }

   @Override
   public String toString() {
      return "DataConversion@" + System.identityHashCode(this) + "{" +
            "wrapperClass=" + wrapperClass +
            ", requestMediaType=" + requestMediaType +
            ", storageMediaType=" + storageMediaType +
            ", wrapperId=" + wrapperId +
            ", wrapper=" + customWrapper +
            ", isKey=" + isKey +
            ", transcoder=" + transcoder +
            '}';
   }

   @Override
   public int hashCode() {
      return Objects.hash(wrapperClass, requestMediaType, isKey);
   }
}
