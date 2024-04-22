package org.infinispan.cli.commands;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

import javax.security.auth.x500.X500Principal;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.option.Option;
import org.infinispan.cli.impl.ContextAwareCommandInvocation;
import org.infinispan.cli.user.UserTool;
import org.infinispan.cli.wizard.Checkbox;
import org.infinispan.cli.wizard.Choice;
import org.infinispan.cli.wizard.Directory;
import org.infinispan.cli.wizard.File;
import org.infinispan.cli.wizard.Number;
import org.infinispan.cli.wizard.Page;
import org.infinispan.cli.wizard.Secret;
import org.infinispan.cli.wizard.Table;
import org.infinispan.cli.wizard.Text;
import org.infinispan.cli.wizard.Values;
import org.infinispan.cli.wizard.Wizard;
import org.infinispan.cli.wizard.YesNo;
import org.infinispan.cli.wizard.driver.swing.SwingDriver;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.kohsuke.MetaInfServices;
import org.wildfly.security.x500.GeneralName;
import org.wildfly.security.x500.cert.BasicConstraintsExtension;
import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;
import org.wildfly.security.x500.cert.SubjectAlternativeNamesExtension;
import org.wildfly.security.x500.cert.X509CertificateBuilder;

@MetaInfServices(Command.class)
@CommandDefinition(name = "setup", description = "Setup")
public class Setup extends CliCommand {
   static final Wizard WIZARD = new Wizard("Infinispan Server setup", "Setup",
         new Page("start", "Infinispan Server setup", "Welcome to the Infinispan Server setup wizard.", Page.NEXT),
         new Page("server", "Server", "Server", Page.NEXT,
               new Directory("server-root", "The root directory for the Infinispan Server installation.")
         ),
         new Page("admin", "Admin user", "Create an admin user", Page.NEXT,
               new Text("username", "Username", "admin"),
               new Secret("password", "Password", "admin")
         ),
         new Page("users", "Users", "Create users", Page.NEXT,
               new Table("credentials", "Users",
                     new Text("username", "Username", "user"),
                     new Secret("password", "Password", "password")
               )
         ),
         new Page("tls", "Enable TLS", "Configure Infinispan Server for TLS connections.",
               v -> switch (v.asString("tls.tls")) {
                  case "create_ca" -> Page.GOTO("create_ca");
                  case "use_ca" -> Page.GOTO("use_ca");
                  default -> Page.GOTO("endpoint");
               },
               new Choice("tls", "TLS",
                     new Choice.Item("create_ca", "Create a Certificate Authority"),
                     new Choice.Item("use_ca", "Use an existing Certificate Authority"),
                     new Choice.Item("no_tls", "Don't use TLS")
               )
         ),
         new Page("create_ca", "Create Certificate Authority", "Create a Certificate Authority which will be used to sign all server and client certificates. ",
               v -> Page.GOTO("endpoint_cert"),
               new Text("ca_dn", "Certificate Authority DN", "CN=ca,DC=infinispan,DC=org"),
               new Number("validity", "Validity (days)", 365),
               new Number("key_size", "Key size", 2048),
               new Choice("key_algorithm", "Key algorithm",
                     new Choice.Item("RSA", "RSA"),
                     new Choice.Item("EC", "EC")
               ),
               new Choice("key_signature_algorithm", "Key Signature Algorithm",
                     new Choice.Item("SHA256withRSA", "SHA256withRSA")
               ),
               new Secret("ca_password", "CA password", "secret")
         ),
         new Page("use_ca", "Use Certificate Authority", "Use an existing Certificate Authority which will be used to sign all server and client certificates",
               v -> Page.GOTO("endpoint_cert"),
               new File("ca_file", "CA file"),
               new File("ca_key", "CA signing key"),
               new Secret("ca_password", "CA password", "secret")
         ),
         new Page("endpoint_cert", "Create server endpoint certificate", "...",
               v -> v.asBoolean("endpoint_cert.mtls") ? Page.GOTO("client_cert") : Page.GOTO("cluster_cert"),
               new Text("cn", "Server CN", "CN=server"),
               new Text("base_dn", "Base DN", "DC=infinispan,DC=org"),
               new Secret("password", "Certificate password", "secret"),
               new Text("dns_names", "DNS names", "localhost"),
               new Text("ip_addresses", "IP addresses", "127.0.0.1"),
               new YesNo("mtls", "Enable mTLS")
         ),
         new Page("client_cert", "Create client cluster certificates", "...", Page.NEXT,
               new Table("clients", "Client names",
                     new Text("cn", "Client CN", "CN=client"),
                     new

                           Secret("password", "Certificate password", "secret")
               )
         ),
         new Page("cluster_cert", "Create server cluster certificates", "...", Page.NEXT,
               new Table("nodes", "Nodes",
                     new Text("cn", "Node CN", "CN=node"),
                     new

                           Secret("password", "Certificate password", "secret")
               )
         ),
         new Page("endpoint", "Endpoints", "Choose which connectors you wish to enable.", Page.NEXT,
               new Checkbox("connectors", "Enabled connectors",
                     new Checkbox.Item("hotrod", "Hot Rod", true),
                     new Checkbox.Item("resp", "Redis", true),
                     new Checkbox.Item("memcached", "Memcached", true)
               )
         )
   );

   @Option(shortName = 'h', hasValue = false, overrideRequired = true)
   protected boolean help;

   @Override
   public boolean isHelp() {
      return help;
   }

   @Override
   public CommandResult exec(ContextAwareCommandInvocation invocation) {
      Values values = new Values();
      values.setValue("server.server-root", Paths.get(System.getProperty("user.home"), "server"));
      runWizard(values);
      return CommandResult.SUCCESS;
   }

   private ConfigurationBuilderHolder loadConfiguration(String serverRoot, String configFile) {
      ParserRegistry parser = new ParserRegistry();
      ConfigurationBuilderHolder holder;
      try {
         return parser.parse(Paths.get(serverRoot, "conf", configFile));
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private void runWizard(Values values) {
      new SwingDriver(WIZARD, values).run().ifPresent(System.out::println);

      //.ifPresent(this::createConfiguration);
   }

   private void createConfiguration(Values v) {
      String serverRoot = v.asString("server.server-root");
      Path conf = Paths.get(serverRoot, "conf");
      //ConfigurationBuilderHolder holder = loadConfiguration();

      // Admin user
      UserTool userTool = new UserTool(serverRoot, null, null);
      String username = v.asString("admin.username");
      char[] password = v.asCharArray("admin.password");
      String realm = v.asString("admin.realm");
      createUser(userTool, username, password, realm);

      // Other users
      for(Values user : v.asIterable("users.credentials")) {
         username = user.asString("username");
         password = user.asCharArray("password");
         createUser(userTool, username, password, realm);
      }

      KeyStore caStore = null;
      PrivateKey caKey = null;
      // Create CA
      if (v.hasValue("create_ca.ca_dn")) {
         SelfSignedX509CertificateAndSigningKey ca = SelfSignedX509CertificateAndSigningKey.builder()
               .setDn(new X500Principal(v.asString("create_ca.ca_dn")))
               .setSignatureAlgorithmName(v.asString("create_ca.key_signature_algorithm"))
               .setKeyAlgorithmName(v.asString("create_ca.key_algorithm"))
               .addExtension(false, "BasicConstraints", "CA:true,pathlen:2147483647")
               .build();
         caStore = writeKeyStore(
               conf.resolve("ca.pfx"),
               v.asCharArray("create_ca.ca_password"),
               ks -> {
                  try {
                     ks.setCertificateEntry("ca", ca.getSelfSignedCertificate());
                  } catch (KeyStoreException e) {
                     throw new RuntimeException(e);
                  }
               });
         caKey = ca.getSigningKey();
      } else if (v.hasValue("use_ca.ca_file")) {
         try (InputStream is = Files.newInputStream(Paths.get(v.asString("use_ca.ca_file")))) {
            caStore = KeyStore.getInstance(KeyStore.getDefaultType());
            caStore.load(is, v.asCharArray("use_ca.ca_password"));
            String alias = caStore.aliases().nextElement();
            X509Certificate caCertificate = (X509Certificate) caStore.getCertificate(alias);
            caCertificate.getSubjectX500Principal().getName();
         } catch (Exception e) {
         }
         try {
            String pem = Files.readString(Paths.get(v.asString("use_ca.ca_key")));
            pem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                  .replace("-----END PRIVATE KEY-----", "")
                  .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // TODO detect type
            caKey = keyFactory.generatePrivate(keySpec);
         } catch (Exception e) {
         }
      }
      if (caStore != null) {
         try {
            KeyStore endpointTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            KeyStore clusterTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            X509Certificate caCertificate = (X509Certificate) caStore.getCertificate(caStore.aliases().nextElement());
            String baseDN = v.asString("endpoint_cert.base_dn");
            // Create server certificate
            createSignedCertificate(
                  caCertificate,
                  caKey,
                  v.asString("endpoint_cert.cn"),
                  baseDN,
                  endpointTrustStore,
                  v.asCharArray("endpoint_cert.password"),
                  v.asString("endpoint_cert.dns_names").split(","),
                  v.asString("endpoint_cert.ip_addresses").split(","),
                  conf
            );
            // Create client certificates
            for (Values client : v.asIterable("client_cert.clients")) {
               createSignedCertificate(
                     caCertificate,
                     caKey,
                     client.asString("cn"),
                     baseDN,
                     endpointTrustStore,
                     client.asCharArray("password"),
                     new String[0],
                     new String[0],
                     conf
               );
            }
            writeKeyStore(endpointTrustStore, conf.resolve("endpoint.truststore.p12"), "".toCharArray());

            // Create server cluster certificates
            for (Values node : v.asIterable("cluster_cert.nodes")) {
               createSignedCertificate(
                     caCertificate,
                     caKey,
                     node.asString("cn"),
                     baseDN,
                     clusterTrustStore,
                     node.asCharArray("password"),
                     new String[0],
                     new String[0],
                     conf
               );
            }
            writeKeyStore(clusterTrustStore, conf.resolve("cluster.truststore.p12"), "".toCharArray());
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
      }
   }

   private static void createUser(UserTool userTool, String username, char[] password, String realm) {
      if (userTool.userExists(username)) {
         userTool.modifyUser(
               username,
               new String(password),
               realm,
               UserTool.Encryption.DEFAULT, null, null
         );
      } else {
         userTool.createUser(
               username,
               new String(password),
               realm,
               UserTool.Encryption.DEFAULT, null, null
         );
      }
   }

   private KeyStore writeKeyStore(KeyStore keyStore, Path path, char[] password) {
      try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE)) {
         keyStore.store(os, password);
         return keyStore;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private KeyStore writeKeyStore(Path path, char[] password, Consumer<KeyStore> consumer) {
      try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE)) {
         KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
         keyStore.load(null, null);
         consumer.accept(keyStore);
         keyStore.store(os, password);
         return keyStore;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   X509Certificate createSignedCertificate(X509Certificate caCertificate,
                                           PrivateKey caKey,
                                           String cn,
                                           String baseDN,
                                           KeyStore trustStore,
                                           char[] password,
                                           String[] dnsNames,
                                           String[] ipAddresses,
                                           Path dir) throws CertificateException, NoSuchAlgorithmException {

      String name = cn.replace("CN=", "");

      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(caKey.getAlgorithm());
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      PrivateKey signingKey = keyPair.getPrivate();
      PublicKey publicKey = keyPair.getPublic();

      List<GeneralName> sANs = new ArrayList<>();
      for (String dnsName : dnsNames) {
         sANs.add(new GeneralName.DNSName(dnsName));
      }
      for (String ipAddress : ipAddresses) {
         sANs.add(new GeneralName.IPAddress(ipAddress));
      }

      X509Certificate certificate = new X509CertificateBuilder()
            .setIssuerDn(caCertificate.getSubjectX500Principal())
            .setSubjectDn(new X500Principal(baseDN))
            .setSignatureAlgorithmName(caCertificate.getSigAlgName())
            .setSigningKey(caKey)
            .setPublicKey(publicKey)
            .setSerialNumber(BigInteger.valueOf(1))
            .addExtension(new BasicConstraintsExtension(false, false, -1))
            .addExtension(new SubjectAlternativeNamesExtension(false, sANs))
            .build();

      if (trustStore != null) {
         try {
            trustStore.setCertificateEntry(name, certificate);
         } catch (KeyStoreException e) {
            throw new RuntimeException(e);
         }
      }
      writeKeyStore(dir.resolve(name + ".pfx"), password, ks -> {
         try {
            ks.setKeyEntry(name, signingKey, password, new X509Certificate[]{certificate, caCertificate});
         } catch (KeyStoreException e) {
            throw new RuntimeException(e);
         }
      });

      try (Writer w = Files.newBufferedWriter(dir.resolve(name + ".pem"))) {
         w.write("-----BEGIN PRIVATE KEY-----\n");
         w.write(Base64.getEncoder().encodeToString(caKey.getEncoded()));
         w.write("\n-----END PRIVATE KEY-----\n");
         w.write("-----BEGIN CERTIFICATE-----\n");
         w.write(Base64.getEncoder().encodeToString(certificate.getEncoded()));
         w.write("\n-----END CERTIFICATE-----\n");
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      try (Writer w = Files.newBufferedWriter(dir.resolve(name + ".crt"))) {
         w.write("-----BEGIN CERTIFICATE-----\n");
         w.write(Base64.getEncoder().encodeToString(certificate.getEncoded()));
         w.write("\n-----END CERTIFICATE-----\n");
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      try (Writer w = Files.newBufferedWriter(dir.resolve(name + ".key"))) {
         w.write("-----BEGIN PRIVATE KEY-----\n");
         w.write(Base64.getEncoder().encodeToString(caKey.getEncoded()));
         w.write("\n-----END PRIVATE KEY-----\n");
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      return certificate;
   }

   public static void main(String[] args) throws Exception {
      Setup setup = new Setup();
      setup.runWizard(new Values());
   }
}
