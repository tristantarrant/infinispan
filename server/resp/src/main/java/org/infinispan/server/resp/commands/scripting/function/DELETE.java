package org.infinispan.server.resp.commands.scripting.function;

import static org.infinispan.server.resp.RespUtil.ascii;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.commands.Resp3Command;
import org.infinispan.server.resp.scripting.EvalTaskEngine;

import io.netty.channel.ChannelHandlerContext;

/**
 * FUNCTION DELETE
 *
 * @see <a href="https://redis.io/docs/latest/commands/function-delete/">FUNCTION DELETE</a>
 * @since 16.0
 */
public class DELETE extends RespCommand implements Resp3Command {
   protected DELETE() {
      super(3, 0, 0, 0, AclCategory.WRITE.mask() | AclCategory.SLOW.mask() | AclCategory.SCRIPTING.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler, ChannelHandlerContext ctx, List<byte[]> arguments) {
      String lib = ascii(arguments.get(0));
      EvalTaskEngine engine = handler.respServer().evalEngine();
      return handler.getBlockingManager()
            .supplyBlocking(() -> engine.functionDelete(lib), "function delete")
            .thenApplyAsync(ignore -> {
               handler.writer().ok();
               return handler;
            }, ctx.channel().eventLoop());
   }
}
