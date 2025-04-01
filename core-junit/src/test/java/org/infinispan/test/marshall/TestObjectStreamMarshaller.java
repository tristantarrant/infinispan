package org.infinispan.test.marshall;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.marshall.BufferSizePredictor;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.marshall.persistence.PersistenceMarshaller;
import org.infinispan.protostream.SerializationContextInitializer;

@Scope(Scopes.GLOBAL)
public class TestObjectStreamMarshaller implements PersistenceMarshaller {

   public TestObjectStreamMarshaller() {
   }

   @Override
   public void register(SerializationContextInitializer initializer) {

   }

   @Override
   public Marshaller getUserMarshaller() {
      return null;
   }

   @Override
   public byte[] objectToByteBuffer(Object obj, int estimatedSize) throws IOException, InterruptedException {
      return new byte[0];
   }

   @Override
   public byte[] objectToByteBuffer(Object obj) throws IOException, InterruptedException {
      return new byte[0];
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf) throws IOException, ClassNotFoundException {
      return null;
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
      return null;
   }

   @Override
   public ByteBuffer objectToBuffer(Object o) throws IOException, InterruptedException {
      return null;
   }

   @Override
   public void writeObject(Object o, OutputStream out) throws IOException {

   }

   @Override
   public Object readObject(InputStream in) throws ClassNotFoundException, IOException {
      return null;
   }

   @Override
   public boolean isMarshallable(Object o) {
      return false;
   }

   @Override
   public int sizeEstimate(Object o) {
      return 0;
   }

   @Override
   public BufferSizePredictor getBufferSizePredictor(Object o) {
      return null;
   }

   @Override
   public MediaType mediaType() {
      return null;
   }
}
