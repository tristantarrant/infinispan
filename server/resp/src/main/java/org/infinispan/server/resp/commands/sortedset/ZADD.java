package org.infinispan.server.resp.commands.sortedset;

import static org.infinispan.multimap.impl.SortedSetAddArgs.ADD_AND_UPDATE_ONLY_INCOMPATIBLE_ERROR;

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
 * ZADD
 *
 * @see <a href="https://redis.io/commands/zadd/">ZADD</a>
 * @since 15.0
 */
public class ZADD extends RespCommand implements Resp3Command {

   private static final byte[] XX = "XX".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] NX = "NX".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] LT = "LT".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] GT = "GT".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] CH = "CH".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] INCR = "INCR".getBytes(StandardCharsets.US_ASCII);

   public ZADD() {
      super(-4, 1, 1, 1, AclCategory.WRITE.mask() | AclCategory.SORTEDSET.mask() | AclCategory.FAST.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler,
                                                      ChannelHandlerContext ctx,
                                                      List<byte[]> arguments) {

      //zadd key [NX|XX] [GT|LT] [CH] [INCR] score member [score member ...]
      byte[] name = arguments.get(0);
      SortedSetAddArgs.Builder addManyArgs = SortedSetAddArgs.create();
      EmbeddedMultimapSortedSetCache<byte[], byte[]> sortedSetCache = handler.getSortedSeMultimap();

      int pos = 1;
      while (pos < arguments.size()) {
         byte[] arg = arguments.get(pos);
         if (RespUtil.isAsciiBytesEquals(NX, arg)) {
            addManyArgs.addOnly();
            pos++;
         } else if (RespUtil.isAsciiBytesEquals(XX, arg)) {
            addManyArgs.updateOnly();
            pos++;
         } else if (RespUtil.isAsciiBytesEquals(GT, arg)) {
            addManyArgs.updateGreaterScoresOnly();
            pos++;
         } else if (RespUtil.isAsciiBytesEquals(LT, arg)) {
            addManyArgs.updateLessScoresOnly();
            pos++;
         } else if (RespUtil.isAsciiBytesEquals(CH, arg)) {
            addManyArgs.returnChangedCount();
            pos++;
         } else if (RespUtil.isAsciiBytesEquals(INCR, arg)) {
            addManyArgs.incr();
            pos++;
         } else {
            break;
         }
      }

      // Validate arguments
      SortedSetAddArgs sortedSetAddArgs;
      try {
         sortedSetAddArgs = addManyArgs.build();
      } catch (IllegalArgumentException ex) {
         if (ex.getMessage().equals(ADD_AND_UPDATE_ONLY_INCOMPATIBLE_ERROR)) {
            handler.writer().customError("XX and NX options at the same time are not compatible");
         } else {
            handler.writer().customError("GT, LT, and/or NX options at the same time are not compatible");
         }
         return handler.myStage();
      }

      // Validate scores and values in pairs. We need at least 1 pair
      if (((arguments.size() - pos) == 0) || (arguments.size() - pos) % 2 != 0) {
         // Scores and Values come in pairs
         handler.writer().syntaxError();
         return handler.myStage();
      }

      int count = (arguments.size() - pos) / 2;
      if (sortedSetAddArgs.incr && count > 1) {
         handler.writer().customError("INCR option supports a single increment-element pair");
         return handler.myStage();
      }

      List<ScoredValue<byte[]>> scoredValues = new ArrayList<>(count);
      while (pos < arguments.size()) {
         double score;
         try {
            score = ArgumentUtils.toDouble(arguments.get(pos++));
         } catch (NumberFormatException e) {
            // validate number format
            handler.writer().valueNotAValidFloat();
            return handler.myStage();
         }
         byte[] value = arguments.get(pos++);
         scoredValues.add(ScoredValue.of(score, value));
      }

      if (sortedSetAddArgs.incr) {
         return handler.stageToReturn(sortedSetCache.incrementScore(name, scoredValues.get(0).score(), scoredValues.get(0).getValue(), sortedSetAddArgs),
               ctx, ResponseWriter.DOUBLE);
      }

      return handler.stageToReturn(sortedSetCache.addMany(name, scoredValues, sortedSetAddArgs), ctx, ResponseWriter.INTEGER);
   }

}
