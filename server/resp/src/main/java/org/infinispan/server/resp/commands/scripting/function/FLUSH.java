package org.infinispan.server.resp.commands.scripting.function;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.commands.Resp3Command;
import org.infinispan.server.resp.scripting.FunctionTaskEngine;

import io.netty.channel.ChannelHandlerContext;

/**
 * FUNCTION FLUSH
 *
 * @see <a href="https://redis.io/docs/latest/commands/function-flush/">FUNCTION FLUSH</a>
 * @since 16.0
 */
public class FLUSH extends RespCommand implements Resp3Command {

   protected FLUSH() {
      super(3, 0, 0, 0, AclCategory.SCRIPTING.mask() | AclCategory.SLOW.mask() | AclCategory.WRITE.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler, ChannelHandlerContext ctx, List<byte[]> arguments) {
      FunctionTaskEngine engine = handler.respServer().functionEngine();
      return handler.getBlockingManager()
            .runBlocking(engine::functionFlush, "function flush")
            .handleAsync((dump, throwable) -> {
               if (throwable == null) {
                  handler.writer().ok();
               } else {
                  handler.writer().customError(throwable.getMessage());
               }
               return handler;
            }, ctx.channel().eventLoop());
   }
}
