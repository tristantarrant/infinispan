package org.infinispan.api;

import org.infinispan.commons.jdkspecific.CallerId;

public abstract class AbstractAPITest {
   protected abstract InfinispanAPIExtension ext();

   public static String k() {
      return k(0);
   }

   public static String k(int index) {
      return String.format("k%d-%s", index, CallerId.getCallerMethodName(2));
   }

   public static String v() {
      return v(0);
   }

   public static String v(int index) {
      return String.format("v%d-%s", index, CallerId.getCallerMethodName(2));
   }
}
