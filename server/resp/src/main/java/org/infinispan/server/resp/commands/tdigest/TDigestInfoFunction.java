package org.infinispan.server.resp.commands.tdigest;

import java.util.LinkedHashMap;
import java.util.Map;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.functional.EntryView;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.server.resp.commands.ProbabilisticErrors;
import org.infinispan.util.function.SerializableFunction;

/**
 * Function to get information about a T-Digest using FunctionalMap.
 * Used by TDIGEST.INFO command.
 *
 * @since 16.2
 */
@ProtoTypeId(ProtoStreamTypeIds.RESP_TDIGEST_INFO_FUNCTION)
public final class TDigestInfoFunction
      implements SerializableFunction<EntryView.ReadEntryView<byte[], Object>, TDigestInfoFunction.TDigestInfo> {

   public static final TDigestInfoFunction INSTANCE = new TDigestInfoFunction();

   TDigestInfoFunction() {
   }

   @Override
   public TDigestInfo apply(EntryView.ReadEntryView<byte[], Object> view) {
      TDigest tdigest = (TDigest) view.peek().orElse(null);
      if (tdigest == null) {
         throw new IllegalStateException(ProbabilisticErrors.TDIGEST_KEY_NOT_FOUND);
      }
      return new TDigestInfo(
            tdigest.getCompression(),
            tdigest.getTotalWeight(),
            tdigest.getCentroids().size(),
            tdigest.getTotalWeight() * Long.BYTES + tdigest.getCentroids().size() * (Double.BYTES + Long.BYTES)
      );
   }

   @ProtoTypeId(ProtoStreamTypeIds.RESP_TDIGEST_INFO)
   public static final class TDigestInfo {
      private final int compression;
      private final long observations;
      private final int mergedNodes;
      private final long memoryUsage;

      @ProtoFactory
      public TDigestInfo(int compression, long observations, int mergedNodes, long memoryUsage) {
         this.compression = compression;
         this.observations = observations;
         this.mergedNodes = mergedNodes;
         this.memoryUsage = memoryUsage;
      }

      @ProtoField(number = 1, defaultValue = "0")
      public int getCompression() {
         return compression;
      }

      @ProtoField(number = 2, defaultValue = "0")
      public long getObservations() {
         return observations;
      }

      @ProtoField(number = 3, defaultValue = "0")
      public int getMergedNodes() {
         return mergedNodes;
      }

      @ProtoField(number = 4, defaultValue = "0")
      public long getMemoryUsage() {
         return memoryUsage;
      }

      public Map<String, Long> toMap() {
         Map<String, Long> map = new LinkedHashMap<>();
         map.put("Compression", (long) compression);
         map.put("Capacity", (long) (compression * 2));
         map.put("Merged nodes", (long) mergedNodes);
         map.put("Unmerged nodes", 0L);
         map.put("Merged weight", observations);
         map.put("Unmerged weight", 0L);
         map.put("Observations", observations);
         map.put("Total compressions", 0L);
         map.put("Memory usage", memoryUsage);
         return map;
      }
   }
}
