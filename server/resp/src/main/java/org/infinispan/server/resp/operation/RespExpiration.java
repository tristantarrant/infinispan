package org.infinispan.server.resp.operation;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.infinispan.server.resp.Util;

public enum RespExpiration {
   EX {
      @Override
      protected long convert(long value) {
         return TimeUnit.SECONDS.toMillis(value);
      }
   },
   PX {
      @Override
      protected long convert(long value) {
         return value;
      }
   },
   EXAT {
      @Override
      protected long convert(long value) {
         return (value - Instant.now().getEpochSecond()) * 1000;
      }
   },
   PXAT {
      @Override
      protected long convert(long value) {
         return value - Instant.now().toEpochMilli();
      }
   };

   public static final byte[] EXAT_BYTES = "EXAT".getBytes(StandardCharsets.US_ASCII);
   public static final byte[] PXAT_BYTES = "PXAT".getBytes(StandardCharsets.US_ASCII);

   protected abstract long convert(long value);

   public static RespExpiration valueOf(byte[] type) {
      if (type.length == 2) {
         if (!Util.caseInsensitiveAsciiCheck('X', type[1]))
            throw new IllegalArgumentException("Invalid expiration type");

         switch (type[0]) {
            case 'E':
            case 'e':
               return EX;
            case 'P':
            case 'p':
               return PX;
         }
      }

      if (type.length == 4) {
         if (Util.isAsciiBytesEquals(EXAT_BYTES, type)) return EXAT;
         if (Util.isAsciiBytesEquals(PXAT_BYTES, type)) return PXAT;
      }

      throw new IllegalArgumentException("Invalid expiration type");
   }
}
