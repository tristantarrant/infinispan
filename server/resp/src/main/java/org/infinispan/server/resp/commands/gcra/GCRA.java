package org.infinispan.server.resp.commands.gcra;

import static org.infinispan.server.resp.commands.ArgumentUtils.toDouble;
import static org.infinispan.server.resp.commands.ArgumentUtils.toLong;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.infinispan.functional.FunctionalMap;
import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.commands.Resp3Command;

import io.netty.channel.ChannelHandlerContext;

/**
 * GCRA key max_burst requests_per_period period [NUM_REQUESTS count]
 *
 * <p>
 * Implements the Generic Cell Rate Algorithm (GCRA) for rate limiting.
 * Returns an array of 5 elements: [limited, max_req_num, num_avail_req, reply_after, full_burst_after].
 * </p>
 *
 * @see <a href="https://redis.io/commands/gcra/">GCRA</a>
 * @since 16.2
 */
public class GCRA extends RespCommand implements Resp3Command {

   public GCRA() {
      super("GCRA", -5, 1, 1, 1,
            AclCategory.WRITE.mask() | AclCategory.FAST.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler, ChannelHandlerContext ctx,
                                                      List<byte[]> arguments) {
      byte[] key = arguments.get(0);

      long maxBurst;
      long requestsPerPeriod;
      double periodSeconds;
      long numRequests = 1;

      try {
         maxBurst = toLong(arguments.get(1));
         if (maxBurst < 0) {
            handler.writer().customError("max_burst must be >= 0");
            return handler.myStage();
         }

         requestsPerPeriod = toLong(arguments.get(2));
         if (requestsPerPeriod < 1) {
            handler.writer().customError("requests_per_period must be >= 1");
            return handler.myStage();
         }

         periodSeconds = toDouble(arguments.get(3));
         if (periodSeconds < 1.0) {
            handler.writer().customError("period must be >= 1.0");
            return handler.myStage();
         }
      } catch (NumberFormatException e) {
         handler.writer().valueNotInteger();
         return handler.myStage();
      }

      // Parse optional NUM_REQUESTS
      int i = 4;
      while (i < arguments.size()) {
         String arg = new String(arguments.get(i), StandardCharsets.US_ASCII).toUpperCase();
         if ("NUM_REQUESTS".equals(arg)) {
            if (i + 1 >= arguments.size()) {
               handler.writer().syntaxError();
               return handler.myStage();
            }
            try {
               numRequests = toLong(arguments.get(++i));
               if (numRequests < 0) {
                  handler.writer().customError("num_requests must be >= 0");
                  return handler.myStage();
               }
            } catch (NumberFormatException e) {
               handler.writer().valueNotInteger();
               return handler.myStage();
            }
         } else {
            handler.writer().syntaxError();
            return handler.myStage();
         }
         i++;
      }

      // Capture current time
      var now = handler.respServer().getTimeService().instant();
      long nowMicros = TimeUnit.SECONDS.toMicros(now.getEpochSecond())
            + TimeUnit.NANOSECONDS.toMicros(now.getNano());

      // Convert period to microseconds
      long periodMicros = (long) (periodSeconds * 1_000_000);

      FunctionalMap.ReadWriteMap<byte[], Object> cache =
            FunctionalMap.create(handler.typedCache(null)).toReadWriteMap();

      GcraFunction function = new GcraFunction(maxBurst, requestsPerPeriod,
            periodMicros, numRequests, nowMicros);
      CompletionStage<List<Object>> result = cache.eval(key, function);

      return handler.stageToReturn(result, ctx, (r, writer) -> {
         writer.arrayStart(5);
         writer.integers((Long) r.get(0));
         writer.integers((Long) r.get(1));
         writer.integers((Long) r.get(2));
         writer.doubles((Double) r.get(3));
         writer.doubles((Double) r.get(4));
         writer.arrayEnd();
      });
   }
}
