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
 * FUNCTION LIST
 *
 * @see <a href="https://redis.io/docs/latest/commands/function-list/">FUNCTION LIST</a>
 * @since 16.0
 */
public class LIST extends RespCommand implements Resp3Command {

   protected LIST() {
      super(3, 0, 0, 0, AclCategory.SCRIPTING.mask() | AclCategory.SLOW.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler, ChannelHandlerContext ctx, List<byte[]> arguments) {
      try {
         String pattern = "*";
         boolean withCode = false;
         for (int i = 1; i < arguments.size(); i++) {
            String arg = ascii(arguments.get(i));
            switch (arg) {
               case "LIBRARYNAME":
                  if (i == arguments.size() -1) {
                     throw new IllegalArgumentException("library name argument was not given");
                  }
                  pattern = ascii(arguments.get(++i));
                  break;
               case "WITHCODE":
                  withCode = true;
                  break;
               default:
                  throw new IllegalArgumentException("Unknown argument " + arg);
            }
         }
         return perform0(handler, ctx, pattern, withCode);
      } catch (Exception e) {
         handler.writer().customError(e.getMessage());
         return handler.myStage();
      }
   }

   private static CompletionStage<RespRequestHandler> perform0(Resp3Handler handler, ChannelHandlerContext ctx, String pattern, boolean withCode) {
      FunctionTaskEngine engine = handler.respServer().functionEngine();
      return handler.getBlockingManager()
            .supplyBlocking(() -> engine.functionList(pattern, withCode), "function list")
            .thenApplyAsync(functions -> {
               handler.writer().serialize(functions);
               return handler;
            }, ctx.channel().eventLoop());
   }
}
