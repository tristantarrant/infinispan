package org.infinispan.cli.commands;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.infinispan.cli.commands.transfer.HotRodTransferEngine;
import org.infinispan.cli.commands.transfer.RestTransferEngine;
import org.infinispan.cli.commands.transfer.TransferEngine;
import org.infinispan.cli.completers.BookmarkCompleter;
import org.infinispan.cli.impl.ContextAwareCommandInvocation;
import org.infinispan.cli.logging.Messages;
import org.kohsuke.MetaInfServices;

/**
 * @since 16.0
 */
@MetaInfServices(Command.class)
@CommandDefinition(name = "transfer", description = "Transfers data between two servers")
public class Transfer extends CliCommand {

   @Option(shortName = 's', required = true, description = "The source URI or bookmark name (e.g. hotrod://host:11222, http://host:11222)", completer = BookmarkCompleter.class)
   String source;

   @Option(shortName = 't', required = true, description = "The target URI or bookmark name (e.g. hotrod://host:11222, http://host:11222)", completer = BookmarkCompleter.class)
   String target;

   @OptionList(shortName = 'c', description = "List of cache names to transfer. If not specified, all caches are transferred.")
   List<String> caches;

   @Option(shortName = 'b', defaultValue = "1000", description = "The number of entries to read per batch. Defaults to 1000.")
   int batchSize;

   @Option(shortName = 'm', defaultValue = "-1", name = "max-entries", description = "Maximum number of entries to transfer per cache. Defaults to all entries.")
   long maxEntries;

   @Option(shortName = 'h', hasValue = false, overrideRequired = true)
   protected boolean help;

   @Override
   public boolean isHelp() {
      return help;
   }

   @Override
   public CommandResult exec(ContextAwareCommandInvocation invocation) throws CommandException {
      String sourceUrl = resolveUrl(invocation, this.source);
      String targetUrl = resolveUrl(invocation, this.target);

      String sourceScheme = URI.create(sourceUrl).getScheme();
      String targetScheme = URI.create(targetUrl).getScheme();

      if (!protocolFamily(sourceScheme).equals(protocolFamily(targetScheme))) {
         throw Messages.MSG.protocolMismatch(sourceScheme, targetScheme);
      }

      try (TransferEngine engine = protocolFamily(sourceScheme).equals("hotrod")
            ? new HotRodTransferEngine()
            : new RestTransferEngine()) {
         engine.connect(invocation, this.source, this.target, sourceUrl, targetUrl);

         Set<String> cacheNames = resolveCacheNames(engine.getCacheNames());
         if (cacheNames == null) {
            cacheNames = engine.getCacheNames();
            invocation.println("Discovered " + cacheNames.size() + " caches on source");
         }

         boolean hasErrors = false;
         for (String cacheName : cacheNames) {
            try {
               engine.transferCache(invocation, cacheName, batchSize, maxEntries);
            } catch (Exception e) {
               invocation.errorln("Failed to transfer cache '" + cacheName + "': " + e.getMessage());
               hasErrors = true;
            }
         }

         return hasErrors ? CommandResult.FAILURE : CommandResult.SUCCESS;
      } catch (Exception e) {
         throw new CommandException(e);
      }
   }

   static String protocolFamily(String scheme) {
      return switch (scheme) {
         case "hotrod", "hotrods" -> "hotrod";
         case "http", "https" -> "http";
         default -> scheme;
      };
   }

   private String resolveUrl(ContextAwareCommandInvocation invocation, String uriOrBookmark) throws CommandException {
      if (uriOrBookmark.contains("://")) {
         return uriOrBookmark;
      }
      Bookmark.ResolvedBookmark bookmark = Bookmark.resolve(invocation, uriOrBookmark);
      if (bookmark == null) {
         throw new CommandException("Bookmark '" + uriOrBookmark + "' not found and argument is not a valid URI");
      }
      return bookmark.url();
   }

   private Set<String> resolveCacheNames(Set<String> discovered) {
      if (caches != null && !caches.isEmpty()) {
         return Set.copyOf(caches);
      }
      return null;
   }
}
