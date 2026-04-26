package org.infinispan.server.resp.commands.list;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.multimap.impl.EmbeddedMultimapListCache;
import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.RespUtil;
import org.infinispan.server.resp.commands.Resp3Command;
import org.infinispan.server.resp.serialization.ResponseWriter;

import io.netty.channel.ChannelHandlerContext;

/**
 * LINSERT
 *
 * @see <a href="https://redis.io/commands/linsert/">LINSERT</a>
 * @since 15.0
 */
public class LINSERT extends RespCommand implements Resp3Command {

   private static final byte[] BEFORE = "BEFORE".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] AFTER = "AFTER".getBytes(StandardCharsets.US_ASCII);

   public LINSERT() {
      super(5, 1, 1, 1, AclCategory.WRITE.mask() | AclCategory.LIST.mask() | AclCategory.SLOW.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler,
                                                      ChannelHandlerContext ctx,
                                                      List<byte[]> arguments) {

      byte[] key = arguments.get(0);
      byte[] position = arguments.get(1);
      boolean isBefore = RespUtil.isAsciiBytesEquals(BEFORE, position);
      if (!isBefore && !RespUtil.isAsciiBytesEquals(AFTER, position)) {
         handler.writer().syntaxError();
         return handler.myStage();
      }

      byte[] pivot = arguments.get(2);
      byte[] element = arguments.get(3);

      EmbeddedMultimapListCache<byte[], byte[]> listMultimap = handler.getListMultimap();
      return handler.stageToReturn(listMultimap.insert(key, isBefore, pivot, element), ctx, ResponseWriter.INTEGER);
   }
}
