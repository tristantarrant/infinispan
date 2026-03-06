package org.infinispan.cli.commands.transfer;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.aesh.command.CommandException;
import org.infinispan.cli.impl.ContextAwareCommandInvocation;

/**
 * Abstraction for protocol-specific cache transfer logic.
 *
 * @since 16.2
 */
public interface TransferEngine extends AutoCloseable {

   /**
    * Connects to the source and target servers.
    */
   void connect(ContextAwareCommandInvocation invocation, String sourceArg, String targetArg,
                String sourceUrl, String targetUrl) throws CommandException;

   /**
    * Returns the set of cache names available on the source.
    */
   Set<String> getCacheNames();

   /**
    * Transfers a single cache from source to target.
    */
   void transferCache(ContextAwareCommandInvocation invocation, String cacheName,
                      int batchSize, long maxEntries);

   @Override
   void close();

   static String eta(long transferred, long total, long startTimeNanos) {
      if (total <= 0 || transferred <= 0) {
         return "";
      }
      long elapsedNanos = System.nanoTime() - startTimeNanos;
      long remainingEntries = total - transferred;
      long etaNanos = (long) ((double) elapsedNanos / transferred * remainingEntries);
      long etaSeconds = TimeUnit.NANOSECONDS.toSeconds(etaNanos);
      return " (ETA: " + formatDuration(etaSeconds) + ")";
   }

   static String formatDuration(long totalSeconds) {
      if (totalSeconds < 60) {
         return totalSeconds + "s";
      }
      long minutes = totalSeconds / 60;
      long seconds = totalSeconds % 60;
      if (minutes < 60) {
         return minutes + "m " + seconds + "s";
      }
      long hours = minutes / 60;
      minutes = minutes % 60;
      return hours + "h " + minutes + "m " + seconds + "s";
   }
}
