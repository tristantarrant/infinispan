package org.infinispan.server.resp.commands.gcra;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.functional.EntryView;
import org.infinispan.functional.MetaParam;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.util.function.SerializableFunction;

/**
 * Implements the GCRA (Generic Cell Rate Algorithm) atomically within a FunctionalMap entry.
 *
 * <p>
 * The algorithm uses a single state value — the Theoretical Arrival Time (TaT) — to determine
 * whether a request should be allowed or rate-limited. When allowed, the TaT is advanced; when
 * limited, the TaT remains unchanged.
 * </p>
 *
 * <p>
 * Returns a {@code List<Object>} with 5 elements:
 * <ol>
 *   <li>{@code Long limited} — 0 (allowed) or 1 (rate limited)</li>
 *   <li>{@code Long maxReqNum} — max_burst + 1 (total capacity)</li>
 *   <li>{@code Long numAvailReq} — available request tokens</li>
 *   <li>{@code Double replyAfter} — seconds until retry (-1.0 if not limited)</li>
 *   <li>{@code Double fullBurstAfter} — seconds until full capacity (-1.0 if already full)</li>
 * </ol>
 * </p>
 *
 * @since 16.2
 */
@ProtoTypeId(ProtoStreamTypeIds.RESP_GCRA_FUNCTION)
public final class GcraFunction
      implements SerializableFunction<EntryView.ReadWriteEntryView<byte[], Object>, List<Object>> {

   private final long maxBurst;
   private final long requestsPerPeriod;
   private final long periodMicros;
   private final long numRequests;
   private final long nowMicros;

   @ProtoFactory
   public GcraFunction(long maxBurst, long requestsPerPeriod, long periodMicros,
                        long numRequests, long nowMicros) {
      this.maxBurst = maxBurst;
      this.requestsPerPeriod = requestsPerPeriod;
      this.periodMicros = periodMicros;
      this.numRequests = numRequests;
      this.nowMicros = nowMicros;
   }

   @ProtoField(number = 1, defaultValue = "0")
   public long getMaxBurst() {
      return maxBurst;
   }

   @ProtoField(number = 2, defaultValue = "1")
   public long getRequestsPerPeriod() {
      return requestsPerPeriod;
   }

   @ProtoField(number = 3, defaultValue = "0")
   public long getPeriodMicros() {
      return periodMicros;
   }

   @ProtoField(number = 4, defaultValue = "1")
   public long getNumRequests() {
      return numRequests;
   }

   @ProtoField(number = 5, defaultValue = "0")
   public long getNowMicros() {
      return nowMicros;
   }

   @Override
   public List<Object> apply(EntryView.ReadWriteEntryView<byte[], Object> view) {
      long emissionInterval = periodMicros / requestsPerPeriod;
      // max_burst is the ADDITIONAL burst beyond the base rate, so total capacity = max_burst + 1.
      // delay_tolerance must account for the full capacity window.
      long delayTolerance = (maxBurst + 1) * emissionInterval;

      GcraState state = (GcraState) view.peek().orElse(null);
      long tat = (state != null) ? state.getTat() : nowMicros;

      long newTat = Math.max(tat, nowMicros) + emissionInterval * numRequests;
      long allowAt = newTat - delayTolerance;

      long limited;
      double replyAfter;
      long effectiveTat;

      if (allowAt > nowMicros) {
         // Rate limited — don't update TaT
         limited = 1;
         replyAfter = (allowAt - nowMicros) / 1_000_000.0;
         effectiveTat = tat;

         // Refresh TTL on existing entry to prevent premature eviction
         if (state != null) {
            long ttlMicros = tat - nowMicros;
            if (ttlMicros > 0) {
               long ttlMillis = TimeUnit.MICROSECONDS.toMillis(ttlMicros) + 1;
               view.set(state, new MetaParam.MetaLifespan(ttlMillis));
            }
         }
      } else {
         // Allowed
         limited = 0;
         replyAfter = -1.0;
         effectiveTat = newTat;

         if (numRequests > 0) {
            GcraState newState = new GcraState(newTat);
            long ttlMicros = newTat - nowMicros;
            if (ttlMicros > 0) {
               long ttlMillis = TimeUnit.MICROSECONDS.toMillis(ttlMicros) + 1;
               view.set(newState, new MetaParam.MetaLifespan(ttlMillis));
            } else {
               view.set(newState);
            }
         }
         // When numRequests == 0 (peek), skip view.set() — no state change
      }

      // Calculate available requests
      long maxReqNum = maxBurst + 1;
      long numAvailReq;
      if (emissionInterval == 0) {
         numAvailReq = maxReqNum;
      } else {
         long diff = nowMicros - (effectiveTat - delayTolerance);
         if (diff < 0) {
            numAvailReq = 0;
         } else {
            numAvailReq = Math.min(diff / emissionInterval, maxReqNum);
         }
      }

      // Calculate full burst after
      double fullBurstAfter;
      if (effectiveTat <= nowMicros) {
         fullBurstAfter = -1.0;
      } else {
         fullBurstAfter = (effectiveTat - nowMicros) / 1_000_000.0;
      }

      return Arrays.asList(limited, maxReqNum, numAvailReq, replyAfter, fullBurstAfter);
   }
}
