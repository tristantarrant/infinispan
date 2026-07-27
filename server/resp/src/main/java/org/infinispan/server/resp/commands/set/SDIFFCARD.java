package org.infinispan.server.resp.commands.set;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

import org.infinispan.multimap.impl.EmbeddedSetCache;
import org.infinispan.multimap.impl.SetBucket;
import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.RespUtil;
import org.infinispan.server.resp.commands.ArgumentUtils;
import org.infinispan.server.resp.commands.Resp3Command;
import org.infinispan.server.resp.serialization.ResponseWriter;

import io.netty.channel.ChannelHandlerContext;

/**
 * SDIFFCARD
 *
 * @see <a href="https://redis.io/commands/sdiffcard/">SDIFFCARD</a>
 * @since 17.0
 */
public class SDIFFCARD extends RespCommand implements Resp3Command {
   private static final byte[] LIMIT = "LIMIT".getBytes(StandardCharsets.US_ASCII);

   public SDIFFCARD() {
      super(-3, 0, 0, 0, AclCategory.READ.mask() | AclCategory.SET.mask() | AclCategory.SLOW.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler,
                                                      ChannelHandlerContext ctx,
                                                      List<byte[]> arguments) {
      int keysNum;
      try {
         keysNum = ArgumentUtils.toInt(arguments.get(0));
      } catch (NumberFormatException ignore) {
         keysNum = 0;
      }

      if (keysNum <= 0) {
         handler.writer().customError("numkeys should be greater than 0");
         return handler.myStage();
      }

      if (arguments.size() < keysNum + 1) {
         handler.writer().customError("Number of keys can't be greater than number of args");
         return handler.myStage();
      }

      int limit = parseOptions(keysNum, arguments, handler);
      if (limit < 0) {
         return handler.myStage();
      }

      byte[] firstKey = arguments.get(1);
      var keys = arguments.subList(1, keysNum + 1);
      boolean diffItself = keys.stream().skip(1)
            .anyMatch(item -> Objects.deepEquals(firstKey, item));
      var uniqueKeys = SINTER.getUniqueKeys(handler, keys);
      EmbeddedSetCache<byte[], byte[]> esc = handler.getEmbeddedSetCache();
      var allEntries = esc.getAll(uniqueKeys);

      final int finalLimit = limit;
      return handler.stageToReturn(
            allEntries.thenApply(sets -> diffCardinality(firstKey, sets, diffItself, finalLimit)),
            ctx,
            ResponseWriter.INTEGER);
   }

   static long diffCardinality(byte[] key, Map<byte[], SetBucket<byte[]>> buckets, boolean diffItself, int limit) {
      if (diffItself) {
         return 0;
      }
      byte[] kInMap = SDIFF.getKeyForMap(key, buckets);
      if (kInMap == null) {
         return 0;
      }
      SetBucket<byte[]> minuend = buckets.get(kInMap);
      buckets.remove(kInMap);

      long count = 0;
      for (byte[] el : minuend.toSet()) {
         boolean found = false;
         for (var bucket : buckets.values()) {
            for (byte[] item : bucket.toList()) {
               if (Objects.deepEquals(el, item)) {
                  found = true;
                  break;
               }
            }
            if (found) {
               break;
            }
         }
         if (!found) {
            count++;
            if (limit > 0 && count >= limit) {
               return limit;
            }
         }
      }
      return count;
   }

   private int parseOptions(int keysNum, List<byte[]> arguments, Resp3Handler handler) {
      int offset = keysNum + 1;
      int limit = 0;

      if (offset < arguments.size()) {
         if (!RespUtil.isAsciiBytesEquals(LIMIT, arguments.get(offset))) {
            handler.writer().syntaxError();
            return -1;
         }
         offset++;
         if (offset >= arguments.size()) {
            handler.writer().syntaxError();
            return -1;
         }
         try {
            limit = ArgumentUtils.toInt(arguments.get(offset));
         } catch (NumberFormatException ex) {
            handler.writer().customError("LIMIT can't be negative");
            return -1;
         }
         if (limit < 0) {
            handler.writer().customError("LIMIT can't be negative");
            return -1;
         }
         offset++;
      }

      if (offset < arguments.size()) {
         handler.writer().syntaxError();
         return -1;
      }

      return limit;
   }
}
