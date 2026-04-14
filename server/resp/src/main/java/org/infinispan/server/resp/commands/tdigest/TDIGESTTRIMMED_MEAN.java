package org.infinispan.server.resp.commands.tdigest;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.functional.FunctionalMap;
import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.commands.ProbabilisticErrors;
import org.infinispan.server.resp.commands.Resp3Command;

import io.netty.channel.ChannelHandlerContext;

/**
 * TDIGEST.TRIMMED_MEAN key lowFraction highFraction
 * <p>
 * Returns the trimmed mean between the specified fractions.
 *
 * @see <a href="https://redis.io/commands/tdigest.trimmed_mean/">TDIGEST.TRIMMED_MEAN</a>
 * @since 16.2
 */
public class TDIGESTTRIMMED_MEAN extends RespCommand implements Resp3Command {

   public TDIGESTTRIMMED_MEAN() {
      super("TDIGEST.TRIMMED_MEAN", 4, 1, 1, 1,
            AclCategory.TDIGEST.mask() | AclCategory.READ.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler, ChannelHandlerContext ctx,
                                                      List<byte[]> arguments) {
      byte[] key = arguments.get(0);
      double lowFraction;
      double highFraction;

      try {
         lowFraction = Double.parseDouble(new String(arguments.get(1)));
      } catch (NumberFormatException e) {
         handler.writer().customError(ProbabilisticErrors.TDIGEST_ERROR_PARSING_LOW_CUT);
         return handler.myStage();
      }

      try {
         highFraction = Double.parseDouble(new String(arguments.get(2)));
      } catch (NumberFormatException e) {
         handler.writer().customError(ProbabilisticErrors.TDIGEST_ERROR_PARSING_HIGH_CUT);
         return handler.myStage();
      }

      if (lowFraction < 0 || lowFraction > 1 || highFraction < 0 || highFraction > 1) {
         handler.writer().customError(ProbabilisticErrors.TDIGEST_CUT_PERCENTILE_RANGE);
         return handler.myStage();
      }

      if (lowFraction >= highFraction) {
         handler.writer().customError(ProbabilisticErrors.TDIGEST_LOW_CUT_LOWER);
         return handler.myStage();
      }

      FunctionalMap.ReadOnlyMap<byte[], Object> cache =
            FunctionalMap.create(handler.typedCache(null)).toReadOnlyMap();

      TDigestTrimmedMeanFunction function = new TDigestTrimmedMeanFunction(lowFraction, highFraction);
      CompletionStage<Double> result = cache.eval(key, function);

      return handler.stageToReturn(result, ctx, (value, w) -> w.doubles(value));
   }
}
