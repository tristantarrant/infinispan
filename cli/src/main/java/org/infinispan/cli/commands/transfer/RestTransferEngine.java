package org.infinispan.cli.commands.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.aesh.command.CommandException;
import org.infinispan.cli.commands.Bookmark;
import org.infinispan.cli.impl.ContextAwareCommandInvocation;
import org.infinispan.client.rest.RestCacheClient;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestEntity;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.client.rest.configuration.RestClientConfigurationBuilder;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.dataconversion.internal.Json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonFactory;

/**
 * Transfers cache data between two servers using the REST protocol.
 *
 * @since 16.2
 */
public class RestTransferEngine implements TransferEngine {

   private static final JsonFactory JSON_FACTORY = new JsonFactory();

   private RestClient source;
   private RestClient target;

   @Override
   public void connect(ContextAwareCommandInvocation invocation, String sourceArg, String targetArg,
                       String sourceUrl, String targetUrl) throws CommandException {
      invocation.println("Connecting to source: " + sourceUrl);
      source = createClient(invocation, sourceArg);

      invocation.println("Connecting to target: " + targetUrl);
      target = createClient(invocation, targetArg);
   }

   @Override
   public Set<String> getCacheNames() {
      RestResponse response = source.caches().toCompletableFuture().join();
      if (response.status() >= 300) {
         throw new IllegalArgumentException("Could not list caches: HTTP " + response.status());
      }
      return Json.read(response.body()).asJsonList().stream()
            .map(Json::asString)
            .collect(Collectors.toSet());
   }

   @Override
   public void transferCache(ContextAwareCommandInvocation invocation, String cacheName,
                             int batchSize, long maxEntries) {
      invocation.println("Transferring cache: " + cacheName);

      RestCacheClient sourceCache = source.cache(cacheName);
      RestCacheClient targetCache = target.cache(cacheName);

      ensureCacheExists(sourceCache, targetCache, cacheName);

      int limit = maxEntries > 0 ? (int) maxEntries : -1;
      RestResponse response = sourceCache.entries(limit, true).toCompletableFuture().join();
      if (response.status() >= 300) {
         throw new IllegalArgumentException("Could not read entries from cache '" + cacheName + "': HTTP " + response.status());
      }

      long count = 0;
      long startTime = System.nanoTime();
      List<CompletableFuture<?>> pending = new ArrayList<>(batchSize);

      try (InputStream is = response.bodyAsStream();
           JsonParser parser = JSON_FACTORY.createParser(is)) {

         if (parser.nextToken() != JsonToken.START_ARRAY) {
            throw new IllegalArgumentException("Expected JSON array from entries endpoint for cache '" + cacheName + "'");
         }

         while (parser.nextToken() == JsonToken.START_OBJECT) {
            String key = null;
            String value = null;
            long ttl = -1;
            long maxIdle = -1;

            while (parser.nextToken() != JsonToken.END_OBJECT) {
               String fieldName = parser.currentName();
               parser.nextToken();
               switch (fieldName) {
                  case "key" -> key = parser.getValueAsString();
                  case "value" -> value = parser.getValueAsString();
                  case "timeToLiveSeconds" -> ttl = parser.getLongValue();
                  case "maxIdleTimeSeconds" -> maxIdle = parser.getLongValue();
                  default -> parser.skipChildren();
               }
            }

            if (key == null) {
               continue;
            }
            if (value == null) {
               value = "";
            }

            RestEntity restEntity = RestEntity.create(MediaType.TEXT_PLAIN, value);
            CompletableFuture<?> future;
            if (ttl > 0 || maxIdle > 0) {
               future = targetCache.put(key, restEntity, ttl > 0 ? ttl : -1, maxIdle > 0 ? maxIdle : -1).toCompletableFuture();
            } else {
               future = targetCache.put(key, restEntity).toCompletableFuture();
            }
            pending.add(future);
            count++;

            if (pending.size() >= batchSize) {
               CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new)).join();
               pending.clear();
            }
            if (count % 1_000 == 0) {
               invocation.println("  " + cacheName + ": transferred " + count + " entries"
                     + TransferEngine.eta(count, maxEntries > 0 ? maxEntries : -1, startTime));
            }
         }
      } catch (IOException e) {
         throw new IllegalArgumentException("Error reading entries from cache '" + cacheName + "': " + e.getMessage(), e);
      }

      if (!pending.isEmpty()) {
         CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new)).join();
      }

      long elapsed = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
      invocation.println("  " + cacheName + ": transferred " + count + " entries in "
            + TransferEngine.formatDuration(elapsed) + " (complete)");
   }

   @Override
   public void close() {
      closeQuietly(source);
      closeQuietly(target);
   }

   private static void ensureCacheExists(RestCacheClient sourceCache, RestCacheClient targetCache, String cacheName) {
      RestResponse existsResponse = targetCache.exists().toCompletableFuture().join();
      if (existsResponse.status() == 404) {
         RestResponse configResponse = sourceCache.configuration(MediaType.APPLICATION_JSON_TYPE).toCompletableFuture().join();
         RestResponse createResponse;
         if (configResponse.status() < 300) {
            createResponse = targetCache.createWithConfiguration(
                  RestEntity.create(MediaType.APPLICATION_JSON, (String) configResponse.body()),
                  CacheContainerAdmin.AdminFlag.VOLATILE).toCompletableFuture().join();
         } else {
            createResponse = targetCache.createWithConfiguration(
                  RestEntity.create(MediaType.APPLICATION_JSON, "{}"),
                  CacheContainerAdmin.AdminFlag.VOLATILE).toCompletableFuture().join();
         }
         if (createResponse.status() >= 300) {
            throw new IllegalArgumentException("Could not create cache '" + cacheName + "' on target: HTTP " + createResponse.status());
         }
      }
   }

   private static RestClient createClient(ContextAwareCommandInvocation invocation, String uriOrBookmark) throws CommandException {
      RestClientConfigurationBuilder builder;
      if (uriOrBookmark.contains("://")) {
         builder = new RestClientConfigurationBuilder().uri(uriOrBookmark);
      } else {
         Bookmark.ResolvedBookmark bookmark = Bookmark.resolve(invocation, uriOrBookmark);
         if (bookmark == null) {
            throw new CommandException("Bookmark '" + uriOrBookmark + "' not found and argument is not a valid URI");
         }
         builder = new RestClientConfigurationBuilder().uri(bookmark.url());
         if (bookmark.username() != null) {
            builder.security().authentication().username(bookmark.username());
         }
         if (bookmark.password() != null) {
            builder.security().authentication().password(bookmark.password());
         }
         if (bookmark.truststore() != null) {
            builder.security().ssl().enable().trustStoreFileName(bookmark.truststore());
            if (bookmark.truststorePassword() != null) {
               builder.security().ssl().trustStorePassword(bookmark.truststorePassword().toCharArray());
            }
         }
         if (bookmark.keystore() != null) {
            builder.security().ssl().enable().keyStoreFileName(bookmark.keystore());
            if (bookmark.keystorePassword() != null) {
               builder.security().ssl().keyStorePassword(bookmark.keystorePassword().toCharArray());
            }
         }
      }
      return RestClient.forConfiguration(builder.build());
   }

   private static void closeQuietly(AutoCloseable closeable) {
      if (closeable != null) {
         try {
            closeable.close();
         } catch (Exception ignored) {
         }
      }
   }
}
