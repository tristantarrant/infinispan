package org.infinispan.test;

import static org.infinispan.commons.test.TestResourceTracker.testFinished;
import static org.infinispan.commons.test.TestResourceTracker.testStarted;
import static org.infinispan.test.JGroupsConfigBuilder.getJGroupsConfig;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.infinispan.Cache;
import org.infinispan.commons.jdkspecific.CallerId;
import org.infinispan.commons.jmx.PlatformMBeanServerLookup;
import org.infinispan.commons.test.TestResourceTracker;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.infinispan.security.Security;
import org.infinispan.security.actions.SecurityActions;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class EmbeddedTestDriver implements AutoCloseable, AfterAllCallback, AfterEachCallback, BeforeAllCallback, BeforeEachCallback {
   private final Log log;
   private final List<EmbeddedCacheManager> cacheManagers;
   private final ConfigurationBuilderHolder holder;
   private final boolean start;
   private final int size;
   private final TransportFlags flags;
   private String methodName;
   private Set<String> methodCaches;

   public static Builder local() {
      ConfigurationBuilderHolder holder = new ConfigurationBuilderHolder();
      holder.getGlobalConfigurationBuilder().nonClusteredDefault();
      return new Builder(holder);
   }

   public static Builder clustered() {
      ConfigurationBuilderHolder holder = new ConfigurationBuilderHolder();
      holder.getGlobalConfigurationBuilder().clusteredDefault();
      return new Builder(holder);
   }

   public static Builder fromFile(String configuration) {
      return fromFile(Paths.get(configuration));
   }

   public static Builder fromFile(Path configuration) {
      try {
         return new Builder(new ParserRegistry().parse(configuration));
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public static Builder fromResource(String configuration) {
      try {
         Class<?> caller = CallerId.getCallerClass(2);
         URL resource = caller.getClassLoader().getResource(configuration);
         return new Builder(new ParserRegistry().parse(resource));
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public static Builder fromString(String configuration) {
      return new Builder(new ParserRegistry().parse(configuration));
   }

   public static Builder fromHolder(ConfigurationBuilderHolder holder) {
      return new Builder(holder);
   }

   private EmbeddedTestDriver(Builder builder) {
      log = LogFactory.getLog(CallerId.getCallerClass(3));
      holder = builder.holder;
      size = builder.size;
      start = builder.start;
      flags = builder.flags;
      cacheManagers = new ArrayList<>(size);
   }

   private ConfigurationBuilderHolder amend(ConfigurationBuilderHolder holder) {
      String testName = TestResourceTracker.getCurrentTestName();
      GlobalConfigurationBuilder gcb = holder.getGlobalConfigurationBuilder();
      GlobalConfiguration gc = gcb.build();
      // Ensure JMX is ok
      assertFalse(gc.jmx().enabled() && gc.jmx().mbeanServerLookup() instanceof PlatformMBeanServerLookup,
            "Tests must configure a MBeanServerLookup other than the default PlatformMBeanServerLookup or not enable JMX");
      // Amend the cluster node name. Set it even for local managers in so that worker threads are named correctly
      TransportConfigurationBuilder transport = gcb.transport();
      if (gc.transport().nodeName() == null) {
         String nextNodeName = TestResourceTracker.getNextNodeName();
         transport.nodeName(nextNodeName);
      }
      if (!flags.isPreserveConfig() && gc.transport().transport() != null) {
         if (flags.isRelayRequired()) {
            // Respect siteName transport flag
            transport.clusterName(flags.siteName() + "-" + testName);
         } else if (gc.transport().attributes().attribute(TransportConfiguration.CLUSTER_NAME).isModified()) {
            // Respect custom cluster name (e.g. from TestCluster)
            transport.clusterName(gc.transport().clusterName() + "-" + testName);
         } else {
            transport.clusterName(testName);
         }
         // Remove any configuration file that might have been set.
         transport.removeProperty(JGroupsTransport.CONFIGURATION_FILE);
         transport.removeProperty(JGroupsTransport.CHANNEL_CONFIGURATOR);
         transport.addProperty(JGroupsTransport.CONFIGURATION_STRING, getJGroupsConfig(testName, flags));
      }
      return holder;
   }

   public EmbeddedCacheManager cacheManager() {
      return cacheManager(0);
   }

   public EmbeddedCacheManager cacheManager(int index) {
      return cacheManagers.get(index);
   }

   public <K, V> Cache<K, V> cache(ConfigurationBuilder builder, Object... qualifiers) {
      String cacheName = cacheName(qualifiers);
      Cache<K, V> cache = cacheManager().createCache(cacheName, builder.build());
      methodCaches.add(cacheName); // only add if creation is successful
      return cache;
   }

   public <K, V> Cache<K, V> cache(String configuration, Object... qualifiers) {
      return cache(new ParserRegistry().parse(configuration).getCurrentConfigurationBuilder(), qualifiers);
   }

   public Log log() {
      return log;
   }

   @Override
   public void close() {
      // Stop the managers in reverse order to prevent each of them from becoming coordinator in turn
      for (int i = cacheManagers.size() - 1; i >= 0; i--) {
         EmbeddedCacheManager cm = cacheManagers.get(i);
         try {
            if (cm != null) {
               SecurityActions.stopManager(cm);
            }
         } catch (Throwable e) {
            log.warnf(e, "Problems killing cache manager %s", cm);
         }
      }
   }

   private void start() {
      for (int i = 0; i < size; i++) {
         DefaultCacheManager cacheManager = new DefaultCacheManager(amend(holder), start);
         TestResourceTracker.addResource(new CacheManagerCleaner(cacheManager));
         cacheManagers.add(cacheManager);
      }
   }

   @Override
   public void beforeAll(ExtensionContext context) {
      testStarted(context.getDisplayName());
      start();
   }

   @Override
   public void afterAll(ExtensionContext context) {
      close();
      testFinished(context.getDisplayName());
   }

   @Override
   public void afterEach(ExtensionContext context) {
      // Remove all caches created during the method execution

   }

   @Override
   public void beforeEach(ExtensionContext context) {
      methodName = context.getTestMethod().map(Method::getName).orElse("unknown");
      methodCaches = new HashSet<>();
   }

   public String cacheName(Object... qualifiers) {
      StringBuilder sb = new StringBuilder("C").append(methodName);
      if (qualifiers != null) {
         for (Object q : qualifiers) {
            if (q != null)
               sb.append(q);
         }
      }
      String cacheName = sb.toString();
      try {
         MessageDigest sha1 = MessageDigest.getInstance("SHA-256");
         byte[] digest = sha1.digest(cacheName.getBytes(StandardCharsets.UTF_8));
         return Util.toHexString(digest);
      } catch (NoSuchAlgorithmException e) {
         // Won't happen
         return null;
      }
   }

   public static class Builder {
      private final ConfigurationBuilderHolder holder;
      private int size = 1;
      private boolean start = true;
      private TransportFlags flags = new TransportFlags();

      private Builder(ConfigurationBuilderHolder holder) {
         this.holder = holder;
      }

      public Builder global(Consumer<GlobalConfigurationBuilder> consumer) {
         consumer.accept(holder.getGlobalConfigurationBuilder());
         return this;
      }

      public Builder transportFlags(TransportFlags flags) {
         this.flags = flags;
         return this;
      }

      public Builder size(int size) {
         this.size = size;
         return this;
      }

      public Builder start(boolean start) {
         this.start = start;
         return this;
      }

      private void validate() {
         if (size > 1 && holder.getGlobalConfigurationBuilder().transport().transport() == null) {
            throw new IllegalArgumentException("Cannot have more than one node if the configuration is not clustered");
         }
      }

      public EmbeddedTestDriver build() {
         validate();
         return new EmbeddedTestDriver(this);
      }

      public void call(Consumer<EmbeddedTestDriver> consumer) {
         try (EmbeddedTestDriver driver = build()) {
            driver.start();
            consumer.accept(driver);
         }
      }
   }

   public class CacheManagerCleaner extends TestResourceTracker.Cleaner<EmbeddedCacheManager> {

      protected CacheManagerCleaner(EmbeddedCacheManager ref) {
         super(ref);
      }

      @Override
      public void close() {
         Runnable action = () -> {
            if (!ref.getStatus().isTerminated()) {
               log.debugf("Stopping cache manager %s", ref);
               ref.stop();
            }
         };
         Security.doPrivileged(action);
      }
   }
}
