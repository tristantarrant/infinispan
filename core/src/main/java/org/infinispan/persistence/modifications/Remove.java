package org.infinispan.persistence.modifications;

import java.util.Objects;

/**
 * Represents a {@link org.infinispan.persistence.async.AsyncNonBlockingStore#delete(int, Object)} modification
 *
 * @author Manik Surtani
 * @since 4.0
 */
public class Remove implements Modification {

   final Object key;

   public Remove(Object key) {
      this.key = key;
   }

   @Override
   public Type getType() {
      return Type.REMOVE;
   }

   public Object getKey() {
      return key;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Remove remove = (Remove) o;

      return Objects.equals(key, remove.key);
   }

   @Override
   public int hashCode() {
      return key != null ? key.hashCode() : 0;
   }

   @Override
   public String toString() {
      return "Remove{" +
            "key=" + key +
            '}';
   }
}
