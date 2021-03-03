package org.infinispan.server.daemon;

import static org.infinispan.server.Server.DEFAULT_BIND_PORT;
import static org.infinispan.server.Server.DEFAULT_CONFIGURATION_FILE;
import static org.infinispan.server.Server.DEFAULT_SERVER_CONFIG;
import static org.infinispan.server.Server.DEFAULT_SERVER_DATA;
import static org.infinispan.server.Server.DEFAULT_SERVER_LOG;
import static org.infinispan.server.Server.DEFAULT_SERVER_ROOT_DIR;
import static org.infinispan.server.Server.INFINISPAN_BIND_PORT;
import static org.infinispan.server.Server.INFINISPAN_SERVER_CONFIG_PATH;
import static org.infinispan.server.Server.INFINISPAN_SERVER_DATA_PATH;
import static org.infinispan.server.Server.INFINISPAN_SERVER_HOME_PATH;
import static org.infinispan.server.Server.INFINISPAN_SERVER_LOG_PATH;
import static org.infinispan.server.Server.INFINISPAN_SERVER_ROOT_PATH;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLStreamException;

import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.time.DefaultTimeService;
import org.infinispan.commons.time.TimeService;
import org.infinispan.commons.util.Version;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.server.DefaultExitHandler;
import org.infinispan.server.ExitHandler;
import org.infinispan.server.ExitStatus;
import org.infinispan.server.configuration.ServerConfiguration;
import org.infinispan.server.core.ProtocolServer;
import org.infinispan.server.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.wildfly.security.http.basic.WildFlyElytronHttpBasicProvider;
import org.wildfly.security.http.bearer.WildFlyElytronHttpBearerProvider;
import org.wildfly.security.http.cert.WildFlyElytronHttpClientCertProvider;
import org.wildfly.security.http.digest.WildFlyElytronHttpDigestProvider;
import org.wildfly.security.http.spnego.WildFlyElytronHttpSpnegoProvider;
import org.wildfly.security.sasl.digest.WildFlyElytronSaslDigestProvider;
import org.wildfly.security.sasl.external.WildFlyElytronSaslExternalProvider;
import org.wildfly.security.sasl.gs2.WildFlyElytronSaslGs2Provider;
import org.wildfly.security.sasl.gssapi.WildFlyElytronSaslGssapiProvider;
import org.wildfly.security.sasl.localuser.WildFlyElytronSaslLocalUserProvider;
import org.wildfly.security.sasl.oauth2.WildFlyElytronSaslOAuth2Provider;
import org.wildfly.security.sasl.plain.WildFlyElytronSaslPlainProvider;
import org.wildfly.security.sasl.scram.WildFlyElytronSaslScramProvider;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 12.1
 */
public class Daemon implements AutoCloseable {
   public static final Log log = LogFactory.getLog("DAEMON", Log.class);

   private final TimeService timeService;
   private final File serverHome;
   private final File serverRoot;
   private final File serverConf;
   private final long startTime;
   private final Properties properties;
   private ExitHandler exitHandler = new DefaultExitHandler();
   private ConfigurationBuilderHolder configurationBuilderHolder;
   private Map<String, ProtocolServer> protocolServers;
   private volatile ComponentStatus status;
   private ServerConfiguration serverConfiguration;

   public Daemon() {
      this(
            new File(DEFAULT_SERVER_ROOT_DIR),
            new File(DEFAULT_CONFIGURATION_FILE),
            SecurityActions.getSystemProperties()
      );
   }

   /**
    * Initializes a daemon with the supplied server root, configuration file and properties
    *
    * @param serverRoot
    * @param configuration
    * @param properties
    */
   public Daemon(File serverRoot, File configuration, Properties properties) {
      this(serverRoot, properties);
      if (!configuration.isAbsolute()) {
         configuration = new File(serverConf, configuration.getPath());
      }
      try {
         parseConfiguration(configuration.toURI().toURL());
      } catch (IOException e) {
         throw new CacheConfigurationException(e);
      }
   }

   private Daemon(File serverRoot, Properties properties) {
      this.timeService = DefaultTimeService.INSTANCE;
      this.startTime = timeService.time();
      this.serverHome = new File(properties.getProperty(INFINISPAN_SERVER_HOME_PATH, ""));
      this.serverRoot = serverRoot;
      this.properties = properties;
      this.status = ComponentStatus.INSTANTIATED;

      // Populate system properties unless they have already been set externally
      properties.putIfAbsent(INFINISPAN_SERVER_HOME_PATH, serverHome);
      properties.putIfAbsent(INFINISPAN_SERVER_ROOT_PATH, serverRoot);
      properties.putIfAbsent(INFINISPAN_SERVER_CONFIG_PATH, new File(serverRoot, DEFAULT_SERVER_CONFIG).getAbsolutePath());
      properties.putIfAbsent(INFINISPAN_SERVER_DATA_PATH, new File(serverRoot, DEFAULT_SERVER_DATA).getAbsolutePath());
      properties.putIfAbsent(INFINISPAN_SERVER_LOG_PATH, new File(serverRoot, DEFAULT_SERVER_LOG).getAbsolutePath());
      properties.putIfAbsent(INFINISPAN_BIND_PORT, DEFAULT_BIND_PORT);

      this.serverConf = new File(properties.getProperty(INFINISPAN_SERVER_CONFIG_PATH));

      // Register only the providers that matter to us
      SecurityActions.addSecurityProvider(WildFlyElytronHttpBasicProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronHttpBearerProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronHttpDigestProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronHttpClientCertProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronHttpSpnegoProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronSaslPlainProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronSaslDigestProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronSaslScramProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronSaslExternalProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronSaslLocalUserProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronSaslOAuth2Provider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronSaslGssapiProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronSaslGs2Provider.getInstance());
   }

   private void parseConfiguration(URL config) {
      ParserRegistry parser = new ParserRegistry(Thread.currentThread().getContextClassLoader(), false, properties);
      try {
         configurationBuilderHolder = new ConfigurationBuilderHolder();
         // load the user configuration
         parser.parse(config, configurationBuilderHolder);
         configurationBuilderHolder.validate();
      } catch (IOException | XMLStreamException e) {
         throw new CacheConfigurationException(e);
      }
   }

   public ExitHandler getExitHandler() {
      return exitHandler;
   }

   public void setExitHandler(ExitHandler exitHandler) {
      if (status == ComponentStatus.INSTANTIATED) {
         this.exitHandler = exitHandler;
      } else {
         throw new IllegalStateException("Cannot change exit handler on a running server");
      }
   }

   public synchronized CompletableFuture<ExitStatus> run() {
      CompletableFuture<ExitStatus> r = exitHandler.getExitFuture();
      if (status == ComponentStatus.RUNNING) {
         return r;
      }

      try {
         // Retrieve the server configuration
         GlobalConfiguration globalConfiguration = configurationBuilderHolder.getGlobalConfigurationBuilder().build();
         serverConfiguration = globalConfiguration.module(ServerConfiguration.class);

         // Next we start the single-port endpoints
         DaemonServer daemonServer = null;
         DaemonServerConfiguration daemonServerConfiguration = null;
         daemonServer.start(daemonServerConfiguration, null);

         log.endpointUrl(
               InetAddress.getLocalHost().getHostName(),
               daemonServerConfiguration.ssl().enabled() ? "https" : "http", daemonServerConfiguration.host(), daemonServerConfiguration.port()
         );

         // Change status
         this.status = ComponentStatus.RUNNING;
         log.serverStarted(Version.getBrandName(), Version.getBrandVersion(), timeService.timeDuration(startTime, TimeUnit.MILLISECONDS));
      } catch (Exception e) {
         r.completeExceptionally(e);
      }
      r = r.whenComplete((status, t) -> localShutdown(status));
      return r;
   }

   @Override
   public void close() {
   }
}
