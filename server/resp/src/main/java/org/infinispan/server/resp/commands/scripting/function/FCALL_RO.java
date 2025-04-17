package org.infinispan.server.resp.commands.scripting.function;

import java.util.concurrent.CompletionStage;

import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespRequestHandler;

import io.netty.channel.ChannelHandlerContext;

/**
 * FCALL_RO
 *
 * @see <a href="https://redis.io/docs/latest/commands/fcall_ro/">FCALL_RO</a>
 * @since 16.0
 */
public class FCALL_RO extends FCALL {

   protected CompletionStage<RespRequestHandler> performFcall(Resp3Handler handler, ChannelHandlerContext ctx, String function, String[] keys, String[] argv) {
      try {
         return handler
               .stageToReturn(handler.respServer().evalEngine().fcall(handler, ctx, function, keys, argv, true)
                     .thenApply(__ -> handler), ctx);
      } catch (Exception e) {
         handler.writer().customError(e.getMessage());
         return handler.myStage();
      }
   }
}
