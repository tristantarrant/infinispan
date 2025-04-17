package org.infinispan.server.resp.commands.scripting.function;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.commands.Resp3Command;

import io.netty.channel.ChannelHandlerContext;

/**
 * FUNCTION FLUSH
 *
 * @see <a href="https://redis.io/docs/latest/commands/function-flush/">FUNCTION FLUSH</a>
 * @since 16.0
 */
public class FLUSH extends RespCommand implements Resp3Command {
   private static final byte[] SYNC_BYTES = "SYNC".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] ASYNC_BYTES = "ASYNC".getBytes(StandardCharsets.US_ASCII);

   protected FLUSH() {
      super(3, 0, 0, 0, AclCategory.SCRIPTING.mask() | AclCategory.SLOW.mask() | AclCategory.WRITE.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler, ChannelHandlerContext ctx, List<byte[]> arguments) {
      return handler.myStage();
   }
}
