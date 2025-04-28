package org.infinispan.server.resp.commands.scripting.function;

import static org.infinispan.server.resp.RespUtil.ascii;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.RespUtil;
import org.infinispan.server.resp.commands.Resp3Command;
import org.infinispan.server.resp.scripting.FunctionTaskEngine;

import io.netty.channel.ChannelHandlerContext;

/**
 * FUNCTION LOAD
 *
 * @see <a href="https://redis.io/docs/latest/commands/function-load/">FUNCTION LOAD</a>
 * @since 15.1
 */
public class LOAD extends RespCommand implements Resp3Command {
   private static final byte[] REPLACE_BYTES = "REPLACE".getBytes(StandardCharsets.US_ASCII);

   protected LOAD() {
      super(3, 0, 0, 0, AclCategory.WRITE.mask() | AclCategory.SCRIPTING.mask() | AclCategory.SLOW.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler, ChannelHandlerContext ctx, List<byte[]> arguments) {
      final boolean replace;
      final String script;
      switch (arguments.size()) {
         case 2:
            replace = false;
            script = ascii(arguments.get(1));
            break;
         case 3:
            if (!RespUtil.isAsciiBytesEquals(REPLACE_BYTES,arguments.get(1))) {
               throw new IllegalArgumentException("Unknown option given: " + ascii(arguments.get(1)));
            }
            replace = true;
            script = ascii(arguments.get(2));
            break;
         default:
            throw new IllegalArgumentException("");
      }

      try {
         FunctionTaskEngine engine = handler.respServer().functionEngine();
         return handler.getBlockingManager()
               .supplyBlocking(() -> engine.functionLoad(script, replace).name(), "function load")
               .handleAsync((name, t) -> {
                  if (t != null) {
                     handler.writer().error(t);
                  } else {
                     handler.writer().string(name);
                  }
                  return handler;
               }, ctx.channel().eventLoop());
      } catch (Exception e) {
         handler.writer().customError(e.getMessage());
         return handler.myStage();
      }
   }
}
