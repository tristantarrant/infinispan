package org.infinispan.server.resp.commands.tdigest;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.functional.EntryView;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.server.resp.commands.ProbabilisticErrors;
import org.infinispan.util.function.SerializableFunction;

/**
 * Function to create a T-Digest using FunctionalMap.
 * Used by TDIGEST.CREATE command.
 *
 * @since 16.2
 */
@ProtoTypeId(ProtoStreamTypeIds.RESP_TDIGEST_CREATE_FUNCTION)
public final class TDigestCreateFunction
      implements SerializableFunction<EntryView.ReadWriteEntryView<byte[], Object>, Boolean> {

   private final int compression;

   @ProtoFactory
   public TDigestCreateFunction(int compression) {
      this.compression = compression;
   }

   @ProtoField(number = 1, defaultValue = "100")
   public int getCompression() {
      return compression;
   }

   @Override
   public Boolean apply(EntryView.ReadWriteEntryView<byte[], Object> view) {
      if (view.peek().isPresent()) {
         throw new IllegalStateException(ProbabilisticErrors.TDIGEST_KEY_EXISTS);
      }

      TDigest tdigest = new TDigest(compression);
      view.set(tdigest);
      return true;
   }
}
