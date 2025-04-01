package org.infinispan.test.marshall;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;
import java.util.Set;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;

public class IdViaAnnotationObj {
   Date date;

   public IdViaAnnotationObj setDate(Date date) {
      this.date = date;
      return this;
   }

   public static class Externalizer extends AbstractExternalizer<IdViaAnnotationObj> {
      @Override
      public void writeObject(ObjectOutput output, IdViaAnnotationObj object) throws IOException {
         output.writeObject(object.date);
      }

      @Override
      public IdViaAnnotationObj readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         return new IdViaAnnotationObj().setDate((Date) input.readObject());
      }

      @Override
      public Integer getId() {
         return 5678;
      }

      @Override
      public Set<Class<? extends IdViaAnnotationObj>> getTypeClasses() {
         return Util.asSet(IdViaAnnotationObj.class);
      }
   }
}
