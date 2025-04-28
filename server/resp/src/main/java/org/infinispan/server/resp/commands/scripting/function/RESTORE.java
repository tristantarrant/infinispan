package org.infinispan.server.resp.commands.scripting.function;

import static org.infinispan.server.resp.RespUtil.ascii;

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
 * FUNCTION RESTORE
 *
 * @see <a href="https://redis.io/docs/latest/commands/function-restore/">FUNCTION RESTORE</a>
 * @since 16.0
 */
public class RESTORE extends RespCommand implements Resp3Command {
   protected RESTORE() {
      super(3, 0, 0, 0, AclCategory.WRITE.mask() | AclCategory.SCRIPTING.mask() | AclCategory.SLOW.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler, ChannelHandlerContext ctx, List<byte[]> arguments) {
      if (arguments.isEmpty()) {
         handler.writer().customError("wrong number of arguments for 'function|restore' command");
      }
      String dump = ascii(arguments.get(0));
      FunctionTaskEngine engine = handler.respServer().functionEngine();
      return handler.getBlockingManager()
            .runBlocking(() -> engine.functionRestore(dump), "function restore")
            .handleAsync((__, throwable) -> {
               if (throwable == null) {
                  handler.writer().ok();
               } else {
                  handler.writer().error(throwable);
               }
               return handler;
            }, ctx.channel().eventLoop());
   }
}
