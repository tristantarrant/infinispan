package org.infinispan.test.marshall;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;

public class IdViaBothObj {
   int age;

   public IdViaBothObj setAge(int age) {
      this.age = age;
      return this;
   }

   public static class Externalizer extends AbstractExternalizer<IdViaBothObj> {
      @Override
      public void writeObject(ObjectOutput output, IdViaBothObj object) throws IOException {
         output.writeInt(object.age);
      }

      @Override
      public IdViaBothObj readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         return new IdViaBothObj().setAge(input.readInt());
      }

      @Override
      public Integer getId() {
         return 9012;
      }

      @Override
      public Set<Class<? extends IdViaBothObj>> getTypeClasses() {
         return Util.asSet(IdViaBothObj.class);
      }
   }
}
