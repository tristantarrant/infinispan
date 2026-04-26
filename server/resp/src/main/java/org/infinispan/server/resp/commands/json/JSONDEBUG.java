package org.infinispan.server.resp.commands.json;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.RespUtil;
import org.infinispan.server.resp.commands.Resp3Command;
import org.infinispan.server.resp.json.EmbeddedJsonCache;
import org.infinispan.server.resp.serialization.ResponseWriter;

import io.netty.channel.ChannelHandlerContext;

/**
 * JSON.DEBUG MEMORY
 *
 * @see <a href="https://redis.io/commands/json.debug-memory/">JSON.DEBUG MEMORY</a>
 *
 * @since 15.2
 */
public class JSONDEBUG extends RespCommand implements Resp3Command {

   private static final byte[] HELP = "HELP".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] MEMORY = "MEMORY".getBytes(StandardCharsets.US_ASCII);

   public JSONDEBUG() {
      super("JSON.DEBUG", -2, 1, 1, 1, AclCategory.JSON.mask() | AclCategory.READ.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler, ChannelHandlerContext ctx,
         List<byte[]> arguments) {
      byte[] subcommand = arguments.get(0);
      if (RespUtil.isAsciiBytesEquals(HELP, subcommand)) {
         List<String> help = List.of(
                 "MEMORY <key> [path] - reports memory usage",
                 "HELP                - this message");
         return handler.stageToReturn(CompletableFuture.completedFuture(help),
                 ctx, ResponseWriter.ARRAY_STRING);
      }

      if (RespUtil.isAsciiBytesEquals(MEMORY, subcommand)) {
         byte[] key = arguments.get(1);
         JSONCommandArgumentReader.CommandArgs commandArgs = JSONCommandArgumentReader.readCommandArgs(arguments, key, 2);
         EmbeddedJsonCache ejc = handler.getJsonCache();
         CompletionStage<List<Long>> debug = ejc.debug(commandArgs.key(), commandArgs.jsonPath());
         if (commandArgs.isLegacy()) {
            CompletionStage<Long> cs = debug.thenApply(result -> result.get(0));
            return  handler.stageToReturn(cs, ctx, ResponseWriter.INTEGER);
         }
         return  handler.stageToReturn(debug, ctx, ResponseWriter.ARRAY_INTEGER);
      }

      throw new RuntimeException("unknown subcommand - try `JSON.DEBUG HELP`");
   }
}
