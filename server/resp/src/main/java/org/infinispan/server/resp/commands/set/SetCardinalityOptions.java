package org.infinispan.server.resp.commands.set;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespUtil;
import org.infinispan.server.resp.commands.ArgumentUtils;

/**
 * Shared argument parsing for the set cardinality commands (SINTERCARD, SUNIONCARD, SDIFFCARD).
 * All of them share the syntax {@code numkeys key [key ...] [LIMIT limit]}, with SUNIONCARD
 * additionally accepting the {@code APPROX} option. Like Redis, the trailing options are
 * parsed in a single pass and can appear in any order, be repeated, and are case-insensitive.
 */
final class SetCardinalityOptions {
   private static final byte[] APPROX = "APPROX".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] LIMIT = "LIMIT".getBytes(StandardCharsets.US_ASCII);

   private SetCardinalityOptions() {
   }

   /**
   * Parsed trailing options: a non-negative {@code limit} (0 means no limit) and,
   * for SUNIONCARD, whether the approximate (HLL) cardinality was requested.
   */
   record Parsed(long limit, boolean approx) {
   }

   /**
   * Parse and validate the {@code numkeys} argument ({@code arguments.get(0)}).
   *
   * @return the number of keys, or -1 if an error response has been written to the handler
   */
   static long parseNumKeys(Resp3Handler handler, List<byte[]> arguments) {
      long keysNum;
      try {
         keysNum = ArgumentUtils.toLong(arguments.get(0));
      } catch (NumberFormatException nfe) {
         keysNum = 0;
      }
      if (keysNum <= 0) {
         handler.writer().customError("numkeys should be greater than 0");
         return -1;
      }
       if (keysNum > arguments.size() - 1) {
          handler.writer().customError("Number of keys can't be greater than number of args");
          return -1;
       }
      return keysNum;
   }

   /**
   * Parse and validate the trailing options after the {@code keysNum} keys.
   *
   * @param approxAllowed whether the {@code APPROX} option is accepted (SUNIONCARD only)
   * @return the parsed options, or null if an error response has been written to the handler
   */
   static Parsed parseOptions(Resp3Handler handler, long keysNum, List<byte[]> arguments, boolean approxAllowed) {
      long limit = 0;
      boolean approx = false;
      for (int i = (int) keysNum + 1; i < arguments.size(); i++) {
         if (RespUtil.isAsciiBytesEquals(LIMIT, arguments.get(i))) {
            i++;
            if (i >= arguments.size()) {
               handler.writer().syntaxError();
               return null;
            }
            try {
               limit = ArgumentUtils.toLong(arguments.get(i));
            } catch (NumberFormatException nfe) {
               handler.writer().customError("LIMIT can't be negative");
               return null;
            }
            if (limit < 0) {
               handler.writer().customError("LIMIT can't be negative");
               return null;
            }
         } else if (approxAllowed && RespUtil.isAsciiBytesEquals(APPROX, arguments.get(i))) {
            approx = true;
         } else {
            handler.writer().syntaxError();
            return null;
         }
      }
      return new Parsed(limit, approx);
   }
}
