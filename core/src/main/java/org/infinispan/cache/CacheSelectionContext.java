package org.infinispan.cache;

import javax.security.auth.Subject;

/**
 * @since 15.0
 **/
public class CacheSelectionContext {
   private final String name;
   private final Subject subject;
   private final Object key;

   public CacheSelectionContext(String name, Subject subject) {
      this.name = name;
      this.subject = subject;
      this.key = null;
   }


   public String name() {
      return name;
   }

   public Object key() {
      return key;
   }

   public Subject subject() {
      return subject;
   }
}
