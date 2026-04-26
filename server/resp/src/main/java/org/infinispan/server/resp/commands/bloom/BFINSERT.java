package org.infinispan.server.resp.commands.bloom;

import static org.infinispan.server.resp.commands.ArgumentUtils.toDouble;
import static org.infinispan.server.resp.commands.ArgumentUtils.toInt;
import static org.infinispan.server.resp.commands.ArgumentUtils.toLong;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.functional.FunctionalMap;
import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.RespUtil;
import org.infinispan.server.resp.commands.Resp3Command;

import io.netty.channel.ChannelHandlerContext;

/**
 * BF.INSERT key [CAPACITY capacity] [ERROR error] [EXPANSION expansion] [NOCREATE] [NONSCALING] ITEMS item [item ...]
 * <p>
 * Creates a new Bloom filter if the key does not exist using the specified parameters, then adds items.
 *
 * @see <a href="https://redis.io/commands/bf.insert/">BF.INSERT</a>
 * @since 16.2
 */
public class BFINSERT extends RespCommand implements Resp3Command {

   private static final byte[] CAPACITY = "CAPACITY".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] ERROR = "ERROR".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] EXPANSION = "EXPANSION".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] NOCREATE = "NOCREATE".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] NONSCALING = "NONSCALING".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] ITEMS = "ITEMS".getBytes(StandardCharsets.US_ASCII);

   public BFINSERT() {
      super("BF.INSERT", -4, 1, 1, 1,
            // No @slow: matches COMMAND INFO output, despite docs claiming @slow
            AclCategory.BLOOM.mask() | AclCategory.WRITE.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler, ChannelHandlerContext ctx,
                                                      List<byte[]> arguments) {
      byte[] key = arguments.get(0);
      long capacity = BloomFilter.DEFAULT_CAPACITY;
      double errorRate = BloomFilter.DEFAULT_ERROR_RATE;
      int expansion = BloomFilter.DEFAULT_EXPANSION;
      boolean noCreate = false;
      boolean nonScaling = false;
      List<byte[]> items = null;

      int i = 1;
      while (i < arguments.size()) {
         byte[] arg = arguments.get(i);
         if (RespUtil.isAsciiBytesEquals(CAPACITY, arg)) {
            if (i + 1 >= arguments.size()) {
               handler.writer().wrongArgumentNumber(this);
               return handler.myStage();
            }
            capacity = toLong(arguments.get(++i));
            if (capacity <= 0) {
               handler.writer().customError("Bad capacity");
               return handler.myStage();
            }
         } else if (RespUtil.isAsciiBytesEquals(ERROR, arg)) {
            if (i + 1 >= arguments.size()) {
               handler.writer().syntaxError();
               return handler.myStage();
            }
            errorRate = toDouble(arguments.get(++i));
            if (errorRate <= 0 || errorRate >= 1) {
               handler.writer().customError("Bad error rate");
               return handler.myStage();
            }
         } else if (RespUtil.isAsciiBytesEquals(EXPANSION, arg)) {
            if (i + 1 >= arguments.size()) {
               handler.writer().wrongArgumentNumber(this);
               return handler.myStage();
            }
            expansion = toInt(arguments.get(++i));
            if (expansion <= 0) {
               handler.writer().customError("Bad expansion");
               return handler.myStage();
            }
         } else if (RespUtil.isAsciiBytesEquals(NOCREATE, arg)) {
            noCreate = true;
         } else if (RespUtil.isAsciiBytesEquals(NONSCALING, arg)) {
            nonScaling = true;
         } else if (RespUtil.isAsciiBytesEquals(ITEMS, arg)) {
            if (i + 1 >= arguments.size()) {
               handler.writer().wrongArgumentNumber(this);
               return handler.myStage();
            }
            items = arguments.subList(i + 1, arguments.size());
            i = arguments.size();
         } else {
            handler.writer().syntaxError();
            return handler.myStage();
         }
         i++;
      }

      if (items == null || items.isEmpty()) {
         handler.writer().wrongArgumentNumber(this);
         return handler.myStage();
      }

      // NOCREATE cannot be combined with CAPACITY or ERROR
      if (noCreate && (capacity != BloomFilter.DEFAULT_CAPACITY || errorRate != BloomFilter.DEFAULT_ERROR_RATE)) {
         handler.writer().customError("ERR NOCREATE cannot be used together with CAPACITY or ERROR");
         return handler.myStage();
      }

      FunctionalMap.ReadWriteMap<byte[], Object> cache =
            FunctionalMap.create(handler.typedCache(null)).toReadWriteMap();

      BloomFilterInsertFunction function = new BloomFilterInsertFunction(items, capacity, errorRate, expansion, noCreate, nonScaling);
      CompletionStage<List<Boolean>> result = cache.eval(key, function);

      return handler.stageToReturn(result, ctx, (r, w) ->
            w.array(r, (b, writer) -> writer.booleans(b)));
   }
}
