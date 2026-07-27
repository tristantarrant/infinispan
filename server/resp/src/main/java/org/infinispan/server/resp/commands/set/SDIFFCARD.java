package org.infinispan.server.resp.commands.set;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import org.infinispan.commons.marshall.WrappedByteArray;
import org.infinispan.multimap.impl.EmbeddedSetCache;
import org.infinispan.multimap.impl.SetBucket;
import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
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
   public SDIFFCARD() {
      super(-3, 0, 0, 0, AclCategory.READ.mask() | AclCategory.SET.mask() | AclCategory.SLOW.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler,
                                                      ChannelHandlerContext ctx,
                                                      List<byte[]> arguments) {
      long keysNum = SetCardinalityOptions.parseNumKeys(handler, arguments);
      if (keysNum < 0) {
         return handler.myStage();
      }
      SetCardinalityOptions.Parsed opts = SetCardinalityOptions.parseOptions(handler, keysNum, arguments, false);
      if (opts == null) {
         return handler.myStage();
      }

      byte[] firstKey = arguments.get(1);
      var keys = arguments.subList(1, (int) keysNum + 1);
      boolean diffItself = keys.stream().skip(1)
            .anyMatch(item -> Objects.deepEquals(firstKey, item));
      var uniqueKeys = SINTER.getUniqueKeys(handler, keys);
      EmbeddedSetCache<byte[], byte[]> esc = handler.getEmbeddedSetCache();
      var allEntries = esc.getAll(uniqueKeys);

      return handler.stageToReturn(
            allEntries.thenApply(sets -> diffCardinality(firstKey, sets, diffItself, opts.limit())),
            ctx,
            ResponseWriter.INTEGER);
   }

   static long diffCardinality(byte[] key, Map<byte[], SetBucket<byte[]>> buckets, boolean diffItself, long limit) {
      if (diffItself) {
         // the result is empty, but all the keys must still be checked for the right type
         SINTER.checkTypesAndReturnEmpty(buckets.values());
         return 0;
      }
      // Build the union of all the subtrahends for O(1) lookups; touching every
      // bucket also enforces the type check on all of them
      byte[] kInMap = SDIFF.getKeyForMap(key, buckets);
      Set<WrappedByteArray> subtrahends = new HashSet<>();
      for (var entry : buckets.entrySet()) {
         if (entry.getKey() == kInMap) {
            continue;
         }
         for (byte[] el : entry.getValue().toList()) {
            if (el != null) {
               subtrahends.add(new WrappedByteArray(el));
            }
         }
      }
      if (kInMap == null) {
         return 0;
      }
      long count = 0;
      for (byte[] el : buckets.get(kInMap).toSet()) {
         if (el != null && !subtrahends.contains(new WrappedByteArray(el))) {
            count++;
            if (limit > 0 && count >= limit) {
               return limit;
            }
         }
      }
      return count;
   }
}
