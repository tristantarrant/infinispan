package org.infinispan.server.resp.commands.set;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.multimap.impl.EmbeddedSetCache;
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
 * SUNIONCARD
 *
 * @see <a href="https://redis.io/commands/sunioncard/">SUNIONCARD</a>
 * @since 17.0
 */
public class SUNIONCARD extends RespCommand implements Resp3Command {
   private static final byte[] APPROX = "APPROX".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] LIMIT = "LIMIT".getBytes(StandardCharsets.US_ASCII);

   public SUNIONCARD() {
      super(-3, 0, 0, 0, AclCategory.READ.mask() | AclCategory.SET.mask() | AclCategory.SLOW.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler,
                                                      ChannelHandlerContext ctx,
                                                      List<byte[]> arguments) {
      int keysNum;
      try {
         keysNum = ArgumentUtils.toInt(arguments.get(0));
      } catch (NumberFormatException ignore) {
         keysNum = 0;
      }

      if (keysNum <= 0) {
         handler.writer().customError("numkeys should be greater than 0");
         return handler.myStage();
      }

      if (arguments.size() < keysNum + 1) {
         handler.writer().customError("Number of keys can't be greater than number of args");
         return handler.myStage();
      }

      Options opts = parseOptions(keysNum, arguments, handler);
      if (opts == null) {
         return handler.myStage();
      }

      var keys = arguments.subList(1, keysNum + 1);
      var uniqueKeys = SINTER.getUniqueKeys(handler, keys);
      EmbeddedSetCache<byte[], byte[]> esc = handler.getEmbeddedSetCache();
      var allEntries = esc.getAll(uniqueKeys);

      return handler.stageToReturn(
            allEntries.thenApply(sets -> opts.approx
                  ? SUNION.unionCardinalityApprox(sets.values(), opts.limit)
                  : SUNION.unionCardinality(sets.values(), opts.limit)),
            ctx,
            ResponseWriter.INTEGER);
   }

   private Options parseOptions(int keysNum, List<byte[]> arguments, Resp3Handler handler) {
      int offset = keysNum + 1;
      boolean approx = false;
      int limit = 0;

      if (offset < arguments.size() && RespUtil.isAsciiBytesEquals(APPROX, arguments.get(offset))) {
         approx = true;
         offset++;
      }

      if (offset < arguments.size()) {
         if (!RespUtil.isAsciiBytesEquals(LIMIT, arguments.get(offset))) {
            handler.writer().syntaxError();
            return null;
         }
         offset++;
         if (offset >= arguments.size()) {
            handler.writer().syntaxError();
            return null;
         }
         try {
            limit = ArgumentUtils.toInt(arguments.get(offset));
         } catch (NumberFormatException ex) {
            handler.writer().customError("LIMIT can't be negative");
            return null;
         }
         if (limit < 0) {
            handler.writer().customError("LIMIT can't be negative");
            return null;
         }
         offset++;
      }

      if (offset < arguments.size()) {
         handler.writer().syntaxError();
         return null;
      }

      return new Options(approx, limit);
   }

   private record Options(boolean approx, int limit) {}
}
