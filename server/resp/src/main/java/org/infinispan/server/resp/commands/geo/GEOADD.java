package org.infinispan.server.resp.commands.geo;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.multimap.impl.EmbeddedMultimapSortedSetCache;
import org.infinispan.multimap.impl.ScoredValue;
import org.infinispan.multimap.impl.SortedSetAddArgs;
import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.RespUtil;
import org.infinispan.server.resp.commands.ArgumentUtils;
import org.infinispan.server.resp.commands.Resp3Command;
import org.infinispan.server.resp.serialization.ResponseWriter;

import io.netty.channel.ChannelHandlerContext;

/**
 * GEOADD
 * <p>
 * Adds the specified geospatial items (longitude, latitude, name) to the specified key.
 * Data is stored as a sorted set with the score being the 52-bit geohash.
 *
 * @see <a href="https://redis.io/commands/geoadd/">GEOADD</a>
 * @since 16.2
 */
public class GEOADD extends RespCommand implements Resp3Command {

   private static final byte[] NX = "NX".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] XX = "XX".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] CH = "CH".getBytes(StandardCharsets.US_ASCII);

   public GEOADD() {
      super(-5, 1, 1, 1, AclCategory.WRITE.mask() | AclCategory.GEO.mask() | AclCategory.SLOW.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler,
                                                      ChannelHandlerContext ctx,
                                                      List<byte[]> arguments) {

      byte[] key = arguments.get(0);
      EmbeddedMultimapSortedSetCache<byte[], byte[]> sortedSetCache = handler.getSortedSeMultimap();
      SortedSetAddArgs.Builder addArgs = SortedSetAddArgs.create();

      int pos = 1;
      // Parse optional arguments (NX, XX, CH)
      while (pos < arguments.size()) {
         byte[] arg = arguments.get(pos);
         if (RespUtil.isAsciiBytesEquals(NX, arg)) {
            addArgs.addOnly();
            pos++;
         } else if (RespUtil.isAsciiBytesEquals(XX, arg)) {
            addArgs.updateOnly();
            pos++;
         } else if (RespUtil.isAsciiBytesEquals(CH, arg)) {
            addArgs.returnChangedCount();
            pos++;
         } else {
            break;
         }
      }

      SortedSetAddArgs sortedSetAddArgs;
      try {
         sortedSetAddArgs = addArgs.build();
      } catch (IllegalArgumentException ex) {
         handler.writer().customError("XX and NX options at the same time are not compatible");
         return handler.myStage();
      }

      // Remaining arguments must be (longitude, latitude, member) triples
      int remaining = arguments.size() - pos;
      if (remaining == 0 || remaining % 3 != 0) {
         handler.writer().syntaxError();
         return handler.myStage();
      }

      List<ScoredValue<byte[]>> scoredValues = new ArrayList<>(remaining / 3);
      while (pos < arguments.size()) {
         double longitude;
         double latitude;
         try {
            longitude = ArgumentUtils.toDouble(arguments.get(pos));
            latitude = ArgumentUtils.toDouble(arguments.get(pos + 1));
         } catch (NumberFormatException e) {
            handler.writer().valueNotAValidFloat();
            return handler.myStage();
         }

         if (!GeoHashUtil.isValidLongitude(longitude) || !GeoHashUtil.isValidLatitude(latitude)) {
            handler.writer().customError(String.format(
                  "invalid longitude,latitude pair %.6f,%.6f", longitude, latitude));
            return handler.myStage();
         }

         byte[] member = arguments.get(pos + 2);
         long geohash = GeoHashUtil.encode(longitude, latitude);
         double score = GeoHashUtil.geohashToScore(geohash);
         scoredValues.add(ScoredValue.of(score, member));

         pos += 3;
      }

      return handler.stageToReturn(
            sortedSetCache.addMany(key, scoredValues, sortedSetAddArgs),
            ctx,
            ResponseWriter.INTEGER);
   }
}
