package org.infinispan.server.resp.commands.scripting.function;

import static org.infinispan.server.resp.RespUtil.ascii;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.commands.ArgumentUtils;
import org.infinispan.server.resp.commands.Resp3Command;

import io.netty.channel.ChannelHandlerContext;

/**
 * FCALL
 *
 * @see <a href="https://redis.io/docs/latest/commands/fcall/">FCALL</a>
 * @since 16.0
 */
public class FCALL extends RespCommand implements Resp3Command {

   public FCALL() {
      super(-3, 1, 1, 1, AclCategory.SCRIPTING.mask() | AclCategory.SLOW.mask());
   }

   @Override
   public final CompletionStage<RespRequestHandler> perform(Resp3Handler handler,
                                                            ChannelHandlerContext ctx,
                                                            List<byte[]> arguments) {
      String function = ascii(arguments.get(0));
      int numKeys = (int) ArgumentUtils.toLong(arguments.get(1));
      String[] keys = new String[numKeys];
      for (int i = 0; i < numKeys; i++) {
         keys[i] = ascii(arguments.get(i + 2));
      }
      String[] argv = new String[arguments.size() - numKeys - 2];
      for (int i = numKeys; i < arguments.size() - 2; i++) {
         argv[i - numKeys] = ascii(arguments.get(i + 2));
      }
      return performFcall(handler, ctx, function, keys, argv);
   }

   protected CompletionStage<RespRequestHandler> performFcall(Resp3Handler handler, ChannelHandlerContext ctx, String function, String[] keys, String[] argv) {
      try {
         return handler.stageToReturn(handler.respServer().functionEngine().fcall(handler, ctx, function, keys, argv, false).thenApply(__ -> handler), ctx);
      } catch (Exception e) {
         handler.writer().customError(e.getMessage());
         return handler.myStage();
      }
   }
}
