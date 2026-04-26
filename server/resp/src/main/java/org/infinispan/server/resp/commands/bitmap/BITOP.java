package org.infinispan.server.resp.commands.bitmap;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.RespUtil;
import org.infinispan.server.resp.commands.Resp3Command;

import io.netty.channel.ChannelHandlerContext;

/**
 * BITOP
 *
 * @see <a href="https://redis.io/commands/bitop/">BITOP</a>
 * @since 16.2
 */
public class BITOP extends RespCommand implements Resp3Command {
   private static final byte[] AND = "AND".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] OR = "OR".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] XOR = "XOR".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] NOT = "NOT".getBytes(StandardCharsets.US_ASCII);

   private static final int OP_AND = 0;
   private static final int OP_OR = 1;
   private static final int OP_XOR = 2;

   public BITOP() {
      super(-4, 2, -1, 1, AclCategory.WRITE.mask() | AclCategory.BITMAP.mask() | AclCategory.SLOW.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler,
                                                      ChannelHandlerContext ctx,
                                                      List<byte[]> arguments) {
      byte[] operationArg = arguments.get(0);
      byte[] destKey = arguments.get(1);
      List<byte[]> srcKeys = arguments.subList(2, arguments.size());

      final int op;
      if (RespUtil.isAsciiBytesEquals(AND, operationArg)) {
         op = OP_AND;
      } else if (RespUtil.isAsciiBytesEquals(OR, operationArg)) {
         op = OP_OR;
      } else if (RespUtil.isAsciiBytesEquals(XOR, operationArg)) {
         op = OP_XOR;
      } else if (RespUtil.isAsciiBytesEquals(NOT, operationArg)) {
         op = -1;
      } else {
         throw new IllegalArgumentException("ERR syntax error");
      }

      List<CompletableFuture<byte[]>> futures = new ArrayList<>();
      for (byte[] srcKey : srcKeys) {
         futures.add(handler.cache().getAsync(srcKey));
      }

      return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenCompose(v -> {
               List<byte[]> values = new ArrayList<>();
               for (CompletableFuture<byte[]> future : futures) {
                  values.add(future.join());
               }

               byte[] result;
               if (op == -1) {
                  if (values.size() != 1) {
                     throw new IllegalArgumentException("BITOP NOT must be called with a single source key.");
                  }
                  result = not(values.get(0));
               } else {
                  result = bitop(op, values);
               }
               return handler.cache().putAsync(destKey, result).thenApply(r -> (long) result.length);
            }).thenCompose(res -> handler.stageToReturn(CompletableFuture.completedFuture(res), ctx, (r, ch) -> ch.integers(r)));
   }

   private byte[] not(byte[] value) {
      if (value == null) {
         return new byte[0];
      }
      byte[] result = new byte[value.length];
      for (int i = 0; i < value.length; i++) {
         result[i] = (byte) ~value[i];
      }
      return result;
   }

   private byte[] bitop(int op, List<byte[]> values) {
      int maxLength = 0;
      for (byte[] value : values) {
         if (value != null && value.length > maxLength) {
            maxLength = value.length;
         }
      }
      if (maxLength == 0) return new byte[0];

      byte[] result = new byte[maxLength];
      for (int i = 0; i < maxLength; i++) {
         byte b = 0;
         boolean first = true;
         for (byte[] value : values) {
            byte v = (value != null && i < value.length) ? value[i] : 0;
            if (first) {
               b = v;
               first = false;
               continue;
            }
            switch (op) {
               case OP_AND:
                  b &= v;
                  break;
               case OP_OR:
                  b |= v;
                  break;
               case OP_XOR:
                  b ^= v;
                  break;
            }
         }
         result[i] = b;
      }
      return result;
   }
}
