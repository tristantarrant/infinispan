package org.infinispan.server.resp.commands.gcra;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * Stores the state for GCRA (Generic Cell Rate Algorithm) rate limiting.
 *
 * <p>
 * The only persisted state is the Theoretical Arrival Time (TaT) in microseconds since epoch.
 * All configuration parameters (max_burst, requests_per_period, period) are passed on each invocation.
 * </p>
 *
 * @since 16.2
 */
@ProtoTypeId(ProtoStreamTypeIds.RESP_GCRA_STATE)
public final class GcraState {

   private final long tat;

   @ProtoFactory
   public GcraState(long tat) {
      this.tat = tat;
   }

   @ProtoField(number = 1, defaultValue = "0")
   public long getTat() {
      return tat;
   }
}
