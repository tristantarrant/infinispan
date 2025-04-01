package org.infinispan.test.marshall;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;

public class IdViaConfigObj {
   String name;

   public IdViaConfigObj setName(String name) {
      this.name = name;
      return this;
   }

   public static class Externalizer extends AbstractExternalizer<IdViaConfigObj> {
      @Override
      public void writeObject(ObjectOutput output, IdViaConfigObj object) throws IOException {
         output.writeUTF(object.name);
      }

      @Override
      public IdViaConfigObj readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         return new IdViaConfigObj().setName(input.readUTF());
      }

      @Override
      public Set<Class<? extends IdViaConfigObj>> getTypeClasses() {
         return Util.asSet(IdViaConfigObj.class);
      }
   }
}
