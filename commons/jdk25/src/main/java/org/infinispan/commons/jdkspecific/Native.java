package org.infinispan.commons.jdkspecific;

import io.smallrye.ffm.Link;

public class Native {

   @Link
   public static native long malloc(long size);

   @Link
   public static native void free(long address);
}
