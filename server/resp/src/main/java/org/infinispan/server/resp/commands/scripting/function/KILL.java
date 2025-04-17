package org.infinispan.server.resp.commands.scripting.function;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.commands.Resp3Command;

import io.netty.channel.ChannelHandlerContext;

/**
 * FUNCTION LOAD
 *
 * @see <a href="https://redis.io/docs/latest/commands/function-load/">FUNCTION LOAD</a>
 * @since 16.0
 */
public class KILL extends RespCommand implements Resp3Command {
   protected KILL() {
      super(3, 0, 0, 0, AclCategory.SCRIPTING.mask() | AclCategory.SLOW.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler, ChannelHandlerContext ctx, List<byte[]> arguments) {
      return handler.myStage();
   }
}
