package org.infinispan.server.resp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.infinispan.test.TestingUtil.k;

import java.util.List;

import org.testng.annotations.Test;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.ArrayOutput;
import io.lettuce.core.protocol.CommandArgs;

@Test(groups = "functional", testName = "server.resp.GcraCommandTest")
public class GcraCommandTest extends SingleNodeRespBaseTest {

   @Test
   public void testBasicAllow() {
      RedisCommands<String, String> redis = redisConnection.sync();

      // First request with max_burst=1, 10 requests per 10 seconds
      List<Object> result = gcra(redis, k(), 1, 10, 10.0);

      // Should be allowed
      assertThat(toLong(result, 0)).isEqualTo(0); // not limited
      assertThat(toLong(result, 1)).isEqualTo(2); // max_req_num = max_burst + 1
      assertThat(toLong(result, 2)).isEqualTo(1); // 1 available after consuming 1
      assertThat(toDouble(result, 3)).isEqualTo(-1.0); // not limited
   }

   @Test
   public void testExhaustCapacity() {
      RedisCommands<String, String> redis = redisConnection.sync();
      String key = k();

      // max_burst=1, 1 request per 10 seconds = 2 total capacity
      // First request — allowed
      List<Object> r1 = gcra(redis, key, 1, 1, 10.0);
      assertThat(toLong(r1, 0)).isEqualTo(0);

      // Second request — allowed (using burst)
      List<Object> r2 = gcra(redis, key, 1, 1, 10.0);
      assertThat(toLong(r2, 0)).isEqualTo(0);

      // Third request — should be denied
      List<Object> r3 = gcra(redis, key, 1, 1, 10.0);
      assertThat(toLong(r3, 0)).isEqualTo(1); // limited
      assertThat(toDouble(r3, 3)).isGreaterThan(0); // reply_after > 0
   }

   @Test
   public void testBurstCapacity() {
      RedisCommands<String, String> redis = redisConnection.sync();
      String key = k();

      // max_burst=4, 1 request per 10 seconds = 5 total capacity
      for (int i = 0; i < 5; i++) {
         List<Object> r = gcra(redis, key, 4, 1, 10.0);
         assertThat(toLong(r, 0)).as("request %d should be allowed", i).isEqualTo(0);
         assertThat(toLong(r, 1)).isEqualTo(5); // max_req_num = 4 + 1
      }

      // 6th request — should be denied
      List<Object> r = gcra(redis, key, 4, 1, 10.0);
      assertThat(toLong(r, 0)).isEqualTo(1);
   }

   @Test
   public void testNumRequests() {
      RedisCommands<String, String> redis = redisConnection.sync();
      String key = k();

      // max_burst=9, 10 requests per 10 seconds = 10 total capacity
      // Consume 5 at once
      List<Object> r1 = gcraWithNumRequests(redis, key, 9, 10, 10.0, 5);
      assertThat(toLong(r1, 0)).isEqualTo(0);
      assertThat(toLong(r1, 2)).isEqualTo(5); // 5 available

      // Consume 5 more
      List<Object> r2 = gcraWithNumRequests(redis, key, 9, 10, 10.0, 5);
      assertThat(toLong(r2, 0)).isEqualTo(0);
      assertThat(toLong(r2, 2)).isEqualTo(0); // 0 available

      // Try to consume 1 more — should be denied
      List<Object> r3 = gcra(redis, key, 9, 10, 10.0);
      assertThat(toLong(r3, 0)).isEqualTo(1);
   }

   @Test
   public void testPeek() {
      RedisCommands<String, String> redis = redisConnection.sync();
      String key = k();

      // First, consume 1
      gcra(redis, key, 2, 1, 10.0);

      // Peek (NUM_REQUESTS 0) — should not change state
      List<Object> peek1 = gcraWithNumRequests(redis, key, 2, 1, 10.0, 0);
      assertThat(toLong(peek1, 0)).isEqualTo(0); // not limited
      long avail1 = toLong(peek1, 2);

      // Peek again — same availability
      List<Object> peek2 = gcraWithNumRequests(redis, key, 2, 1, 10.0, 0);
      assertThat(toLong(peek2, 2)).isEqualTo(avail1);
   }

   @Test
   public void testZeroBurst() {
      RedisCommands<String, String> redis = redisConnection.sync();
      String key = k();

      // max_burst=0, 1 request per 10 seconds = 1 total capacity
      List<Object> r1 = gcra(redis, key, 0, 1, 10.0);
      assertThat(toLong(r1, 0)).isEqualTo(0);
      assertThat(toLong(r1, 1)).isEqualTo(1); // max_req_num = 0 + 1

      // Second request — should be denied
      List<Object> r2 = gcra(redis, key, 0, 1, 10.0);
      assertThat(toLong(r2, 0)).isEqualTo(1);
   }

   @Test
   public void testResponseFormat() {
      RedisCommands<String, String> redis = redisConnection.sync();
      String key = k();

      // First request on fresh key
      List<Object> r = gcra(redis, key, 3, 2, 10.0);

      // Verify all 5 elements
      assertThat(r).hasSize(5);
      assertThat(toLong(r, 0)).isIn(0L, 1L); // limited
      assertThat(toLong(r, 1)).isEqualTo(4L); // max_burst + 1
      assertThat(toLong(r, 2)).isGreaterThanOrEqualTo(0); // num_avail_req
      // reply_after and full_burst_after are doubles
      assertThat(toDouble(r, 3)).isNotNull();
      assertThat(toDouble(r, 4)).isNotNull();
   }

   @Test
   public void testErrorInvalidMaxBurst() {
      RedisCommands<String, String> redis = redisConnection.sync();

      assertThatThrownBy(() -> gcra(redis, k(), -1, 1, 10.0))
            .isInstanceOf(RedisCommandExecutionException.class)
            .hasMessageContaining("max_burst");
   }

   @Test
   public void testErrorInvalidRequestsPerPeriod() {
      RedisCommands<String, String> redis = redisConnection.sync();

      assertThatThrownBy(() -> gcra(redis, k(), 1, 0, 10.0))
            .isInstanceOf(RedisCommandExecutionException.class)
            .hasMessageContaining("requests_per_period");
   }

   @Test
   public void testErrorInvalidPeriod() {
      RedisCommands<String, String> redis = redisConnection.sync();

      assertThatThrownBy(() -> gcra(redis, k(), 1, 1, 0.5))
            .isInstanceOf(RedisCommandExecutionException.class)
            .hasMessageContaining("period");
   }

   @Test
   public void testErrorInvalidNumRequests() {
      RedisCommands<String, String> redis = redisConnection.sync();

      assertThatThrownBy(() -> gcraWithNumRequests(redis, k(), 1, 1, 10.0, -1))
            .isInstanceOf(RedisCommandExecutionException.class)
            .hasMessageContaining("num_requests");
   }

   @Test
   public void testErrorUnknownOption() {
      RedisCommands<String, String> redis = redisConnection.sync();

      assertThatThrownBy(() -> redis.dispatch(
            command("GCRA"),
            new ArrayOutput<>(StringCodec.UTF8),
            new CommandArgs<>(StringCodec.UTF8).addKey(k())
                  .add("1").add("1").add("10").add("UNKNOWN").add("5")))
            .isInstanceOf(RedisCommandExecutionException.class)
            .hasMessageContaining("syntax");
   }

   @Test
   public void testNewKeyFirstRequestFullAvailability() {
      RedisCommands<String, String> redis = redisConnection.sync();
      String key = k();

      // On a fresh key, first peek should show full availability
      List<Object> r = gcraWithNumRequests(redis, key, 5, 10, 10.0, 0);
      assertThat(toLong(r, 0)).isEqualTo(0);
      assertThat(toLong(r, 1)).isEqualTo(6); // max_burst + 1
      assertThat(toLong(r, 2)).isEqualTo(6); // all available
      assertThat(toDouble(r, 3)).isEqualTo(-1.0);
      assertThat(toDouble(r, 4)).isEqualTo(-1.0); // fully replenished
   }

   private List<Object> gcra(RedisCommands<String, String> redis, String key,
                              long maxBurst, long requestsPerPeriod, double period) {
      return redis.dispatch(
            command("GCRA"),
            new ArrayOutput<>(StringCodec.UTF8),
            new CommandArgs<>(StringCodec.UTF8).addKey(key)
                  .add(String.valueOf(maxBurst))
                  .add(String.valueOf(requestsPerPeriod))
                  .add(String.valueOf(period)));
   }

   private List<Object> gcraWithNumRequests(RedisCommands<String, String> redis, String key,
                                             long maxBurst, long requestsPerPeriod, double period,
                                             long numRequests) {
      return redis.dispatch(
            command("GCRA"),
            new ArrayOutput<>(StringCodec.UTF8),
            new CommandArgs<>(StringCodec.UTF8).addKey(key)
                  .add(String.valueOf(maxBurst))
                  .add(String.valueOf(requestsPerPeriod))
                  .add(String.valueOf(period))
                  .add("NUM_REQUESTS")
                  .add(String.valueOf(numRequests)));
   }

   @SuppressWarnings("unchecked")
   private long toLong(List<Object> result, int index) {
      Object val = result.get(index);
      if (val instanceof Long l) {
         return l;
      }
      return Long.parseLong(val.toString());
   }

   @SuppressWarnings("unchecked")
   private double toDouble(List<Object> result, int index) {
      Object val = result.get(index);
      if (val instanceof Double d) {
         return d;
      }
      return Double.parseDouble(val.toString());
   }

   private SimpleCommand command(String name) {
      return new SimpleCommand(name);
   }

   private static class SimpleCommand implements io.lettuce.core.protocol.ProtocolKeyword {
      private final String name;

      SimpleCommand(String name) {
         this.name = name;
      }

      @Override
      public byte[] getBytes() {
         return name.getBytes();
      }

      @Override
      public String name() {
         return name;
      }
   }
}
