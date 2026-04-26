package org.infinispan.server.resp.commands.geo;

import java.nio.charset.StandardCharsets;

import org.infinispan.server.resp.RespUtil;

/**
 * Distance units for GEO commands.
 *
 * @since 16.2
 */
public enum GeoUnit {
   M(1.0),
   KM(1000.0),
   MI(1609.344),
   FT(0.3048);

   private static final byte[] M_BYTES = "M".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] KM_BYTES = "KM".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] MI_BYTES = "MI".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] FT_BYTES = "FT".getBytes(StandardCharsets.US_ASCII);

   private final double meters;

   GeoUnit(double meters) {
      this.meters = meters;
   }

   public double toMeters(double value) {
      return value * meters;
   }

   public double fromMeters(double value) {
      return value / meters;
   }

   public static GeoUnit parse(byte[] arg) {
      if (arg == null || arg.length == 0) {
         return null;
      }
      if (RespUtil.isAsciiBytesEquals(M_BYTES, arg)) return M;
      if (RespUtil.isAsciiBytesEquals(KM_BYTES, arg)) return KM;
      if (RespUtil.isAsciiBytesEquals(MI_BYTES, arg)) return MI;
      if (RespUtil.isAsciiBytesEquals(FT_BYTES, arg)) return FT;
      return null;
   }
}
