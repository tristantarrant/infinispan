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
 * FUNCTION RESTORE
 *
 * @see <a href="https://redis.io/docs/latest/commands/function-restore/">FUNCTION RESTORE</a>
 * @since 15.1
 */
public class RESTORE extends RespCommand implements Resp3Command {
   protected RESTORE() {
      super(3, 0, 0, 0, AclCategory.WRITE.mask() | AclCategory.SCRIPTING.mask() | AclCategory.SLOW.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler, ChannelHandlerContext ctx, List<byte[]> arguments) {
      return handler.myStage();
   }
}
