package org.infinispan.server.resp.commands.geo;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.infinispan.server.resp.RespUtil;
import org.infinispan.server.resp.commands.ArgumentUtils;

/**
 * Options for GEOSEARCH and related commands.
 *
 * @since 16.2
 */
public class GeoSearchOptions {

   private static final byte[] FROMMEMBER = "FROMMEMBER".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] FROMLONLAT = "FROMLONLAT".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] BYRADIUS = "BYRADIUS".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] BYBOX = "BYBOX".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] ASC = "ASC".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] DESC = "DESC".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] COUNT = "COUNT".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] ANY = "ANY".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] WITHCOORD = "WITHCOORD".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] WITHDIST = "WITHDIST".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] WITHHASH = "WITHHASH".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] STOREDIST_KW = "STOREDIST".getBytes(StandardCharsets.US_ASCII);

   // Search center - either FROMMEMBER or FROMLONLAT
   private byte[] fromMember;
   private Double longitude;
   private Double latitude;

   // Search shape - either BYRADIUS or BYBOX
   private Double radius;
   private Double width;
   private Double height;
   private GeoUnit unit;

   // Result options
   private boolean withCoord;
   private boolean withDist;
   private boolean withHash;
   private Long count;
   private boolean any;
   private boolean asc = true; // default ASC

   public byte[] getFromMember() {
      return fromMember;
   }

   public Double getLongitude() {
      return longitude;
   }

   public Double getLatitude() {
      return latitude;
   }

   public boolean isByRadius() {
      return radius != null;
   }

   public boolean isByBox() {
      return width != null && height != null;
   }

   public Double getRadius() {
      return radius;
   }

   public Double getWidth() {
      return width;
   }

   public Double getHeight() {
      return height;
   }

   public GeoUnit getUnit() {
      return unit;
   }

   public boolean isWithCoord() {
      return withCoord;
   }

   public boolean isWithDist() {
      return withDist;
   }

   public boolean isWithHash() {
      return withHash;
   }

   public Long getCount() {
      return count;
   }

   public boolean isAny() {
      return any;
   }

   public boolean isAsc() {
      return asc;
   }

   public boolean hasCenter() {
      return fromMember != null || (longitude != null && latitude != null);
   }

   public boolean hasShape() {
      return isByRadius() || isByBox();
   }

   /**
    * Parse GEOSEARCH options from arguments.
    *
    * @param arguments  the command arguments
    * @param startIndex index to start parsing from
    * @return GeoSearchOptions instance
    */
   public static GeoSearchOptions parse(List<byte[]> arguments, int startIndex) {
      GeoSearchOptions options = new GeoSearchOptions();
      int pos = startIndex;

      while (pos < arguments.size()) {
         byte[] arg = arguments.get(pos);
         if (RespUtil.isAsciiBytesEquals(FROMMEMBER, arg)) {
            if (pos + 1 >= arguments.size()) {
               throw new IllegalArgumentException("syntax error");
            }
            options.fromMember = arguments.get(++pos);
         } else if (RespUtil.isAsciiBytesEquals(FROMLONLAT, arg)) {
            if (pos + 2 >= arguments.size()) {
               throw new IllegalArgumentException("syntax error");
            }
            try {
               options.longitude = ArgumentUtils.toDouble(arguments.get(++pos));
               options.latitude = ArgumentUtils.toDouble(arguments.get(++pos));
            } catch (NumberFormatException e) {
               throw new IllegalArgumentException("value is not a valid float");
            }
            if (!GeoHashUtil.isValidLongitude(options.longitude) ||
                  !GeoHashUtil.isValidLatitude(options.latitude)) {
               throw new IllegalArgumentException(String.format(
                     "invalid longitude,latitude pair %.6f,%.6f",
                     options.longitude, options.latitude));
            }
         } else if (RespUtil.isAsciiBytesEquals(BYRADIUS, arg)) {
            if (pos + 2 >= arguments.size()) {
               throw new IllegalArgumentException("syntax error");
            }
            try {
               options.radius = ArgumentUtils.toDouble(arguments.get(++pos));
            } catch (NumberFormatException e) {
               throw new IllegalArgumentException("value is not a valid float");
            }
            options.unit = GeoUnit.parse(arguments.get(++pos));
            if (options.unit == null) {
               throw new IllegalArgumentException("unsupported unit provided. please use M, KM, FT, MI");
            }
         } else if (RespUtil.isAsciiBytesEquals(BYBOX, arg)) {
            if (pos + 3 >= arguments.size()) {
               throw new IllegalArgumentException("syntax error");
            }
            try {
               options.width = ArgumentUtils.toDouble(arguments.get(++pos));
               options.height = ArgumentUtils.toDouble(arguments.get(++pos));
            } catch (NumberFormatException e) {
               throw new IllegalArgumentException("value is not a valid float");
            }
            options.unit = GeoUnit.parse(arguments.get(++pos));
            if (options.unit == null) {
               throw new IllegalArgumentException("unsupported unit provided. please use M, KM, FT, MI");
            }
         } else if (RespUtil.isAsciiBytesEquals(ASC, arg)) {
            options.asc = true;
         } else if (RespUtil.isAsciiBytesEquals(DESC, arg)) {
            options.asc = false;
         } else if (RespUtil.isAsciiBytesEquals(COUNT, arg)) {
            if (pos + 1 >= arguments.size()) {
               throw new IllegalArgumentException("syntax error");
            }
            try {
               options.count = ArgumentUtils.toLong(arguments.get(++pos));
            } catch (NumberFormatException e) {
               throw new IllegalArgumentException("value is not an integer or out of range");
            }
            if (pos + 1 < arguments.size() &&
                  RespUtil.isAsciiBytesEquals(ANY, arguments.get(pos + 1))) {
               options.any = true;
               pos++;
            }
         } else if (RespUtil.isAsciiBytesEquals(WITHCOORD, arg)) {
            options.withCoord = true;
         } else if (RespUtil.isAsciiBytesEquals(WITHDIST, arg)) {
            options.withDist = true;
         } else if (RespUtil.isAsciiBytesEquals(WITHHASH, arg)) {
            options.withHash = true;
         } else if (RespUtil.isAsciiBytesEquals(STOREDIST_KW, arg)) {
            // Handled by GEOSEARCHSTORE, skip here
         } else {
            throw new IllegalArgumentException("syntax error");
         }
         pos++;
      }

      return options;
   }
}
