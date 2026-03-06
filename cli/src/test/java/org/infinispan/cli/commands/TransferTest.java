package org.infinispan.cli.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.infinispan.server.hotrod.test.HotRodTestingUtil.hotRodCacheConfiguration;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.aesh.command.CommandResult;
import org.aesh.command.Executor;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.command.shell.Shell;
import org.aesh.readline.Prompt;
import org.aesh.terminal.KeyAction;
import org.infinispan.cli.AeshTestShell;
import org.infinispan.cli.impl.ContextAwareCommandInvocation;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.test.HotRodClientTestingUtil;
import org.infinispan.client.rest.RestCacheClient;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestEntity;
import org.infinispan.client.rest.configuration.RestClientConfigurationBuilder;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.rest.RestServer;
import org.infinispan.rest.configuration.RestServerConfigurationBuilder;
import org.infinispan.server.core.DummyServerManagement;
import org.infinispan.server.core.admin.embeddedserver.EmbeddedServerAdminOperationHandler;
import org.infinispan.server.core.test.ServerTestingUtil;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.testing.junit.JUnitThreadTrackerRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class TransferTest {

   @ClassRule
   public static final JUnitThreadTrackerRule tracker = new JUnitThreadTrackerRule();

   private EmbeddedCacheManager sourceCacheManager;
   private EmbeddedCacheManager targetCacheManager;
   private HotRodServer sourceServer;
   private HotRodServer targetServer;
   private RemoteCacheManager sourceRemoteCacheManager;
   private RemoteCacheManager targetRemoteCacheManager;

   // REST servers
   private EmbeddedCacheManager sourceRestCacheManager;
   private EmbeddedCacheManager targetRestCacheManager;
   private RestServer sourceRestServer;
   private RestServer targetRestServer;
   private RestClient sourceRestClient;
   private RestClient targetRestClient;

   @Before
   public void setup() {
      ConfigurationBuilder cacheConfig = hotRodCacheConfiguration();

      sourceCacheManager = TestCacheManagerFactory.createCacheManager(cacheConfig);
      targetCacheManager = TestCacheManagerFactory.createCacheManager(cacheConfig);

      HotRodServerConfigurationBuilder serverBuilder = new HotRodServerConfigurationBuilder();
      serverBuilder.adminOperationsHandler(new EmbeddedServerAdminOperationHandler());

      sourceServer = HotRodClientTestingUtil.startHotRodServer(sourceCacheManager, serverBuilder);

      serverBuilder = new HotRodServerConfigurationBuilder();
      serverBuilder.adminOperationsHandler(new EmbeddedServerAdminOperationHandler());
      targetServer = HotRodClientTestingUtil.startHotRodServer(targetCacheManager, serverBuilder);

      sourceRemoteCacheManager = HotRodClientTestingUtil.getRemoteCacheManager(sourceServer);
      targetRemoteCacheManager = HotRodClientTestingUtil.getRemoteCacheManager(targetServer);

      // REST servers
      sourceRestCacheManager = TestCacheManagerFactory.createCacheManager(cacheConfig);
      targetRestCacheManager = TestCacheManagerFactory.createCacheManager(cacheConfig);

      sourceRestServer = startRestServer(sourceRestCacheManager);
      targetRestServer = startRestServer(targetRestCacheManager);

      sourceRestClient = RestClient.forConfiguration(
            new RestClientConfigurationBuilder()
                  .addServer().host(sourceRestServer.getHost()).port(sourceRestServer.getPort())
                  .build());
      targetRestClient = RestClient.forConfiguration(
            new RestClientConfigurationBuilder()
                  .addServer().host(targetRestServer.getHost()).port(targetRestServer.getPort())
                  .build());
   }

   private static RestServer startRestServer(EmbeddedCacheManager cacheManager) {
      return ServerTestingUtil.startProtocolServer(ServerTestingUtil.findFreePort(), port -> {
         RestServerConfigurationBuilder builder = new RestServerConfigurationBuilder();
         builder.host("127.0.0.1").port(port);
         builder.adminOperationsHandler(new EmbeddedServerAdminOperationHandler());
         RestServer server = new RestServer();
         server.setServerManagement(new DummyServerManagement(cacheManager, new HashMap<>()), true);
         server.start(builder.build(), cacheManager);
         server.postStart();
         return server;
      });
   }

   @After
   public void teardown() {
      HotRodClientTestingUtil.killRemoteCacheManagers(sourceRemoteCacheManager);
      HotRodClientTestingUtil.killRemoteCacheManagers(targetRemoteCacheManager);
      HotRodClientTestingUtil.killServers(sourceServer);
      HotRodClientTestingUtil.killServers(targetServer);
      if (sourceCacheManager != null) sourceCacheManager.stop();
      if (targetCacheManager != null) targetCacheManager.stop();
      closeQuietly(sourceRestClient);
      closeQuietly(targetRestClient);
      ServerTestingUtil.killServer(sourceRestServer);
      ServerTestingUtil.killServer(targetRestServer);
      if (sourceRestCacheManager != null) sourceRestCacheManager.stop();
      if (targetRestCacheManager != null) targetRestCacheManager.stop();
   }

   private static void closeQuietly(AutoCloseable closeable) {
      if (closeable != null) {
         try { closeable.close(); } catch (Exception ignored) {}
      }
   }

   @Test
   public void testTransferEntries() {
      RemoteCache<String, String> sourceCache = sourceRemoteCacheManager.administration()
            .withFlags(org.infinispan.commons.api.CacheContainerAdmin.AdminFlag.VOLATILE)
            .getOrCreateCache("testCache", (String) null);

      sourceCache.put("key1", "value1");
      sourceCache.put("key2", "value2");
      sourceCache.put("key3", "value3");

      Transfer transfer = new Transfer();
      transfer.source = String.format("hotrod://127.0.0.1:%d", sourceServer.getPort());
      transfer.target = String.format("hotrod://127.0.0.1:%d", targetServer.getPort());
      transfer.caches = java.util.List.of("testCache");
      transfer.batchSize = 100;
      transfer.maxEntries = -1;

      CommandResult result = runTransfer(transfer);
      assertThat(result).isEqualTo(CommandResult.SUCCESS);

      RemoteCache<String, String> targetCache = targetRemoteCacheManager.getCache("testCache");
      assertThat(targetCache).isNotNull();
      assertThat(targetCache.size()).isEqualTo(3);
      assertThat(targetCache.get("key1")).isEqualTo("value1");
      assertThat(targetCache.get("key2")).isEqualTo("value2");
      assertThat(targetCache.get("key3")).isEqualTo("value3");
   }

   @Test
   public void testTransferWithMetadata() {
      RemoteCache<String, String> sourceCache = sourceRemoteCacheManager.administration()
            .withFlags(org.infinispan.commons.api.CacheContainerAdmin.AdminFlag.VOLATILE)
            .getOrCreateCache("metaCache", (String) null);

      sourceCache.put("persistent", "no-expiry");
      sourceCache.put("expiring", "with-lifespan", 3600, TimeUnit.SECONDS);
      sourceCache.put("idle", "with-maxidle", -1, TimeUnit.SECONDS, 1800, TimeUnit.SECONDS);
      sourceCache.put("both", "both-set", 7200, TimeUnit.SECONDS, 900, TimeUnit.SECONDS);

      Transfer transfer = new Transfer();
      transfer.source = String.format("hotrod://127.0.0.1:%d", sourceServer.getPort());
      transfer.target = String.format("hotrod://127.0.0.1:%d", targetServer.getPort());
      transfer.caches = java.util.List.of("metaCache");
      transfer.batchSize = 100;
      transfer.maxEntries = -1;

      CommandResult result = runTransfer(transfer);
      assertThat(result).isEqualTo(CommandResult.SUCCESS);

      RemoteCache<String, String> targetCache = targetRemoteCacheManager.getCache("metaCache");
      assertThat(targetCache).isNotNull();
      assertThat(targetCache.size()).isEqualTo(4);

      // Verify values
      assertThat(targetCache.get("persistent")).isEqualTo("no-expiry");
      assertThat(targetCache.get("expiring")).isEqualTo("with-lifespan");
      assertThat(targetCache.get("idle")).isEqualTo("with-maxidle");
      assertThat(targetCache.get("both")).isEqualTo("both-set");

      // Verify metadata was preserved
      MetadataValue<String> persistentMeta = targetCache.getWithMetadata("persistent");
      assertThat(persistentMeta.getLifespan()).isEqualTo(-1);
      assertThat(persistentMeta.getMaxIdle()).isEqualTo(-1);

      MetadataValue<String> expiringMeta = targetCache.getWithMetadata("expiring");
      assertThat(expiringMeta.getLifespan()).isGreaterThan(0);

      MetadataValue<String> idleMeta = targetCache.getWithMetadata("idle");
      assertThat(idleMeta.getMaxIdle()).isGreaterThan(0);

      MetadataValue<String> bothMeta = targetCache.getWithMetadata("both");
      assertThat(bothMeta.getLifespan()).isGreaterThan(0);
      assertThat(bothMeta.getMaxIdle()).isGreaterThan(0);
   }

   @Test
   public void testTransferMultipleCaches() {
      RemoteCache<String, String> cache1 = sourceRemoteCacheManager.administration()
            .withFlags(org.infinispan.commons.api.CacheContainerAdmin.AdminFlag.VOLATILE)
            .getOrCreateCache("cache1", (String) null);
      RemoteCache<String, String> cache2 = sourceRemoteCacheManager.administration()
            .withFlags(org.infinispan.commons.api.CacheContainerAdmin.AdminFlag.VOLATILE)
            .getOrCreateCache("cache2", (String) null);

      cache1.put("a", "1");
      cache2.put("b", "2");

      Transfer transfer = new Transfer();
      transfer.source = String.format("hotrod://127.0.0.1:%d", sourceServer.getPort());
      transfer.target = String.format("hotrod://127.0.0.1:%d", targetServer.getPort());
      transfer.caches = java.util.List.of("cache1", "cache2");
      transfer.batchSize = 100;
      transfer.maxEntries = -1;

      CommandResult result = runTransfer(transfer);
      assertThat(result).isEqualTo(CommandResult.SUCCESS);

      RemoteCache<String, String> targetCache1 = targetRemoteCacheManager.getCache("cache1");
      RemoteCache<String, String> targetCache2 = targetRemoteCacheManager.getCache("cache2");
      assertThat(targetCache1.get("a")).isEqualTo("1");
      assertThat(targetCache2.get("b")).isEqualTo("2");
   }

   @Test
   public void testTransferMaxEntries() {
      RemoteCache<String, String> sourceCache = sourceRemoteCacheManager.administration()
            .withFlags(org.infinispan.commons.api.CacheContainerAdmin.AdminFlag.VOLATILE)
            .getOrCreateCache("limitCache", (String) null);

      for (int i = 0; i < 10; i++) {
         sourceCache.put("key" + i, "value" + i);
      }

      Transfer transfer = new Transfer();
      transfer.source = String.format("hotrod://127.0.0.1:%d", sourceServer.getPort());
      transfer.target = String.format("hotrod://127.0.0.1:%d", targetServer.getPort());
      transfer.caches = java.util.List.of("limitCache");
      transfer.batchSize = 100;
      transfer.maxEntries = 5;

      CommandResult result = runTransfer(transfer);
      assertThat(result).isEqualTo(CommandResult.SUCCESS);

      RemoteCache<String, String> targetCache = targetRemoteCacheManager.getCache("limitCache");
      assertThat(targetCache).isNotNull();
      assertThat(targetCache.size()).isEqualTo(5);
   }

   @Test
   public void testTransferNonExistentCache() {
      Transfer transfer = new Transfer();
      transfer.source = String.format("hotrod://127.0.0.1:%d", sourceServer.getPort());
      transfer.target = String.format("hotrod://127.0.0.1:%d", targetServer.getPort());
      transfer.caches = java.util.List.of("nonExistentCache");
      transfer.batchSize = 100;
      transfer.maxEntries = -1;

      CommandResult result = runTransfer(transfer);
      assertThat(result).isEqualTo(CommandResult.FAILURE);
   }

   @Test
   public void testRestTransferEntries() {
      RestCacheClient sourceCache = sourceRestClient.cache("restTestCache");
      sourceCache.createWithConfiguration(
            RestEntity.create(MediaType.APPLICATION_JSON, "{}"),
            CacheContainerAdmin.AdminFlag.VOLATILE).toCompletableFuture().join();
      sourceCache.put("key1", RestEntity.create(MediaType.TEXT_PLAIN, "value1")).toCompletableFuture().join();
      sourceCache.put("key2", RestEntity.create(MediaType.TEXT_PLAIN, "value2")).toCompletableFuture().join();
      sourceCache.put("key3", RestEntity.create(MediaType.TEXT_PLAIN, "value3")).toCompletableFuture().join();

      Transfer transfer = new Transfer();
      transfer.source = String.format("http://127.0.0.1:%d", sourceRestServer.getPort());
      transfer.target = String.format("http://127.0.0.1:%d", targetRestServer.getPort());
      transfer.caches = java.util.List.of("restTestCache");
      transfer.batchSize = 100;
      transfer.maxEntries = -1;

      CommandResult result = runTransfer(transfer);
      assertThat(result).isEqualTo(CommandResult.SUCCESS);

      RestCacheClient targetCache = targetRestClient.cache("restTestCache");
      assertThat((String) targetCache.get("key1").toCompletableFuture().join().body()).isEqualTo("value1");
      assertThat((String) targetCache.get("key2").toCompletableFuture().join().body()).isEqualTo("value2");
      assertThat((String) targetCache.get("key3").toCompletableFuture().join().body()).isEqualTo("value3");
   }

   @Test
   public void testRestTransferMultipleCaches() {
      RestCacheClient cache1 = sourceRestClient.cache("restCache1");
      RestCacheClient cache2 = sourceRestClient.cache("restCache2");
      cache1.createWithConfiguration(RestEntity.create(MediaType.APPLICATION_JSON, "{}"),
            CacheContainerAdmin.AdminFlag.VOLATILE).toCompletableFuture().join();
      cache2.createWithConfiguration(RestEntity.create(MediaType.APPLICATION_JSON, "{}"),
            CacheContainerAdmin.AdminFlag.VOLATILE).toCompletableFuture().join();
      cache1.put("a", RestEntity.create(MediaType.TEXT_PLAIN, "1")).toCompletableFuture().join();
      cache2.put("b", RestEntity.create(MediaType.TEXT_PLAIN, "2")).toCompletableFuture().join();

      Transfer transfer = new Transfer();
      transfer.source = String.format("http://127.0.0.1:%d", sourceRestServer.getPort());
      transfer.target = String.format("http://127.0.0.1:%d", targetRestServer.getPort());
      transfer.caches = java.util.List.of("restCache1", "restCache2");
      transfer.batchSize = 100;
      transfer.maxEntries = -1;

      CommandResult result = runTransfer(transfer);
      assertThat(result).isEqualTo(CommandResult.SUCCESS);

      assertThat((String) targetRestClient.cache("restCache1").get("a").toCompletableFuture().join().body()).isEqualTo("1");
      assertThat((String) targetRestClient.cache("restCache2").get("b").toCompletableFuture().join().body()).isEqualTo("2");
   }

   @Test
   public void testProtocolMismatch() {
      Transfer transfer = new Transfer();
      transfer.source = String.format("hotrod://127.0.0.1:%d", sourceServer.getPort());
      transfer.target = String.format("http://127.0.0.1:%d", targetRestServer.getPort());
      transfer.caches = java.util.List.of("testCache");
      transfer.batchSize = 100;
      transfer.maxEntries = -1;

      AeshTestShell shell = new AeshTestShell();
      CommandInvocation delegate = new StubCommandInvocation(shell);
      ContextAwareCommandInvocation invocation = new ContextAwareCommandInvocation(delegate, null);
      try {
         CommandResult result = transfer.execute(invocation);
         assertThat(result).isEqualTo(CommandResult.FAILURE);
      } catch (Exception e) {
         // Expected - protocol mismatch should throw
         assertThat(e.getMessage()).contains("protocol");
      }
   }

   private CommandResult runTransfer(Transfer transfer) {
      AeshTestShell shell = new AeshTestShell();
      CommandInvocation delegate = new StubCommandInvocation(shell);
      ContextAwareCommandInvocation invocation = new ContextAwareCommandInvocation(delegate, null);
      try {
         return transfer.execute(invocation);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private record StubCommandInvocation(Shell shell) implements CommandInvocation {

      @Override
         public Shell getShell() {
            return shell;
         }

         @Override
         public void setPrompt(Prompt prompt) {
         }

         @Override
         public Prompt getPrompt() {
            return null;
         }

         @Override
         public String getHelpInfo(String commandName) {
            return "";
         }

         @Override
         public String getHelpInfo() {
            return "";
         }

         @Override
         public void stop() {
         }

         @Override
         public CommandInvocationConfiguration getConfiguration() {
            return null;
         }

         @Override
         public KeyAction input() {
            return null;
         }

         @Override
         public KeyAction input(long timeout, TimeUnit unit) {
            return null;
         }

         @Override
         public String inputLine() {
            return null;
         }

         @Override
         public String inputLine(Prompt prompt) {
            return null;
         }

         @Override
         public void executeCommand(String input) {
         }

         @Override
         public Executor buildExecutor(String line) {
            return null;
         }

         @Override
         public void print(String msg) {
            shell.write(msg, false);
         }

         @Override
         public void println(String msg) {
            shell.writeln(msg, false);
         }

         @Override
         public void print(String msg, boolean paging) {
            shell.write(msg, paging);
         }

         @Override
         public void println(String msg, boolean paging) {
            shell.writeln(msg, paging);
         }
      }
}
