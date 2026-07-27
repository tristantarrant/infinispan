package org.infinispan.server.resp.commands.set;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.multimap.impl.EmbeddedSetCache;
import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
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
   public SUNIONCARD() {
      super(-3, 0, 0, 0, AclCategory.READ.mask() | AclCategory.SET.mask() | AclCategory.SLOW.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler,
                                                      ChannelHandlerContext ctx,
                                                      List<byte[]> arguments) {
      long keysNum = SetCardinalityOptions.parseNumKeys(handler, arguments);
      if (keysNum < 0) {
         return handler.myStage();
      }
      SetCardinalityOptions.Parsed opts = SetCardinalityOptions.parseOptions(handler, keysNum, arguments, true);
      if (opts == null) {
         return handler.myStage();
      }

      var keys = arguments.subList(1, (int) keysNum + 1);
      var uniqueKeys = SINTER.getUniqueKeys(handler, keys);
      EmbeddedSetCache<byte[], byte[]> esc = handler.getEmbeddedSetCache();
      var allEntries = esc.getAll(uniqueKeys);

      return handler.stageToReturn(
            allEntries.thenApply(sets -> opts.approx()
                  ? SUNION.unionCardinalityApprox(sets.values(), opts.limit())
                  : SUNION.unionCardinality(sets.values(), opts.limit())),
            ctx,
            ResponseWriter.INTEGER);
   }
}
