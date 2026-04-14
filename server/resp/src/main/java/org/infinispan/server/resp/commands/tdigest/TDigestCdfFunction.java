package org.infinispan.server.resp.commands.tdigest;

import java.util.ArrayList;
import java.util.List;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.functional.EntryView;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.server.resp.commands.ProbabilisticErrors;
import org.infinispan.util.function.SerializableFunction;

/**
 * Function to get CDF values from a T-Digest using FunctionalMap.
 * Used by TDIGEST.CDF command.
 *
 * @since 16.2
 */
@ProtoTypeId(ProtoStreamTypeIds.RESP_TDIGEST_CDF_FUNCTION)
public final class TDigestCdfFunction
      implements SerializableFunction<EntryView.ReadEntryView<byte[], Object>, List<Double>> {

   private final List<Double> values;

   @ProtoFactory
   public TDigestCdfFunction(List<Double> values) {
      this.values = values;
   }

   @ProtoField(number = 1)
   public List<Double> getValues() {
      return values;
   }

   @Override
   public List<Double> apply(EntryView.ReadEntryView<byte[], Object> view) {
      TDigest tdigest = (TDigest) view.peek().orElse(null);
      if (tdigest == null) {
         throw new IllegalStateException(ProbabilisticErrors.TDIGEST_KEY_NOT_FOUND);
      }

      List<Double> results = new ArrayList<>();
      for (Double value : values) {
         results.add(tdigest.cdf(value));
      }
      return results;
   }
}
