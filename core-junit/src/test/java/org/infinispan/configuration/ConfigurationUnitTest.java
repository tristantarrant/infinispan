package org.infinispan.configuration;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.infinispan.transaction.TransactionMode.NON_TRANSACTIONAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.infinispan.Cache;
import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.Combine;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.marshall.BufferSizePredictor;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.util.FileLookup;
import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.commons.util.Version;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.IsolationLevel;
import org.infinispan.configuration.cache.MemoryConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.persistence.dummy.DummyInMemoryStoreConfigurationBuilder;
import org.infinispan.test.EmbeddedTestDriver;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.EmbeddedTransactionManagerLookup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;

public class ConfigurationUnitTest {

   @RegisterExtension
   static EmbeddedTestDriver EMBEDDED = EmbeddedTestDriver.clustered().build();

   @Test
   public void testBuild() {
      // Simple test to ensure we can actually build a config
      ConfigurationBuilder cb = new ConfigurationBuilder();
      cb.build();
   }

   @Test
   public void testEvictionSize() {
      Configuration configuration = new ConfigurationBuilder().memory().size(20).build();
      assertEquals(20, configuration.memory().size());
   }

   @Test
   public void testDistSyncAutoCommit() {
      Configuration configuration = new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).transaction().autoCommit(true).build();
      assertTrue(configuration.transaction().autoCommit());
      assertEquals(CacheMode.DIST_SYNC, configuration.clustering().cacheMode());
   }

   @Test
   public void testDummyTMGetCache() {
      ConfigurationBuilder cb = new ConfigurationBuilder();
      cb.transaction().use1PcForAutoCommitTransactions(true).transactionManagerLookup(new EmbeddedTransactionManagerLookup());
      EMBEDDED.cache(cb);
   }

   @Test
   public void testGetCache() {
      EMBEDDED.cache(new ConfigurationBuilder());
   }

   @Test
   public void testDefineNamedCache() {
      EMBEDDED.cacheManager().defineConfiguration(EMBEDDED.cacheName(), new ConfigurationBuilder().build());
   }

   @Test
   public void testGetAndPut() {
      Cache<String, String> cache = EMBEDDED.cache(new ConfigurationBuilder());
      cache.put("Foo", "2");
      cache.put("Bar", "4");
      assertEquals("2", cache.get("Foo"));
      assertEquals("4", cache.get("Bar"));
   }

   @Test
   public void testInvocationBatchingAndNonTransactional() {
      ConfigurationBuilder cb = new ConfigurationBuilder();
      cb.transaction().transactionMode(NON_TRANSACTIONAL).invocationBatching().enable();
      assertThatThrownBy(() -> EMBEDDED.cache(cb)).isInstanceOf(CacheConfigurationException.class).hasMessageMatching("ISPN(\\d)*: Cannot enable Invocation Batching when the Transaction Mode is NON_TRANSACTIONAL, set the transaction mode to TRANSACTIONAL");
   }

   @Test
   public void testDisableL1() {
      ConfigurationBuilder cb = new ConfigurationBuilder();
      cb.clustering().cacheMode(CacheMode.DIST_SYNC).l1().disable();
      Cache<Object, Object> cache = EMBEDDED.cache(cb);
      assertFalse(cache.getCacheConfiguration().clustering().l1().enabled());
   }

   @Test
   public void testClearStores() {
      Configuration c = new ConfigurationBuilder().persistence().addStore(DummyInMemoryStoreConfigurationBuilder.class).persistence().clearStores().build();
      assertEquals(0, c.persistence().stores().size());
   }

   @Test
   public void testClusterNameNull() {
      GlobalConfigurationBuilder gc = new GlobalConfigurationBuilder();
      gc.transport().clusterName(null);
      assertThatThrownBy(gc::build).isInstanceOf(CacheConfigurationException.class);
   }

   //@Test(enabled = false, description = "JGRP-2590")
   public void testSchema() throws Exception {
      FileLookup lookup = FileLookupFactory.newInstance();
      String schemaFilename = String.format("schema/infinispan-config-%s.xsd", Version.getMajorMinor());
      URL schemaFile = lookup.lookupFileLocation(schemaFilename, Thread.currentThread().getContextClassLoader());
      if (schemaFile == null) {
         throw new NullPointerException("Failed to find a schema file " + schemaFilename);
      }
      Source xmlFile = new StreamSource(lookup.lookupFile(String.format("configs/all/%s.xml", Version.getMajorMinor()), Thread.currentThread().getContextClassLoader()));
      try {
         SchemaFactory factory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
         factory.setResourceResolver(new TestResolver());
         factory.newSchema(schemaFile).newValidator().validate(xmlFile);
      } catch (IllegalArgumentException e) {
         fail("Unable to validate schema", e);
      }
   }

   @Test
   public void testNumOwners() {
      ConfigurationBuilder cb = new ConfigurationBuilder();
      cb.clustering().cacheMode(CacheMode.DIST_SYNC);
      cb.clustering().hash().numOwners(5);

      Configuration c = cb.build();
      assertEquals(5, c.clustering().hash().numOwners());

      // negative test
      assertThatThrownBy(() -> cb.clustering().hash().numOwners(0)).isInstanceOf(IllegalArgumentException.class);
   }

   @Test
   public void numVirtualNodes() {
      ConfigurationBuilder cb = new ConfigurationBuilder();
      cb.clustering().cacheMode(CacheMode.DIST_SYNC);
      cb.clustering().hash().numSegments(5);

      Configuration c = cb.build();
      assertEquals(5, c.clustering().hash().numSegments());

      // negative test
      assertThatThrownBy(() -> cb.clustering().hash().numSegments(0)).isInstanceOf(IllegalArgumentException.class);
   }

   @Test
   public void testNoneIsolationLevel() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.locking().isolationLevel(IsolationLevel.NONE);
      Cache<Object, Object> cache = EMBEDDED.cache(builder);
      Configuration cfg = cache.getCacheConfiguration();
      assertEquals(IsolationLevel.NONE, cfg.locking().lockIsolationLevel());
   }

   @Test
   public void testNoneIsolationLevelInCluster() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.locking().isolationLevel(IsolationLevel.NONE).clustering().cacheMode(CacheMode.REPL_SYNC).build();
      Cache<Object, Object> cache = EMBEDDED.cache(builder);
      Configuration cfg = cache.getCacheConfiguration();
      assertEquals(IsolationLevel.READ_COMMITTED, cfg.locking().lockIsolationLevel());
   }

   @Test
   public void testConfigureMarshaller() {
      GlobalConfigurationBuilder gc = new GlobalConfigurationBuilder();
      gc.serialization().marshaller(new Marshaller() {
         @Override
         public byte[] objectToByteBuffer(Object obj, int estimatedSize) throws IOException, InterruptedException {
            return new byte[0];
         }

         @Override
         public byte[] objectToByteBuffer(Object obj) throws IOException, InterruptedException {
            return new byte[0];
         }

         @Override
         public Object objectFromByteBuffer(byte[] buf) throws IOException, ClassNotFoundException {
            return null;
         }

         @Override
         public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
            return null;
         }

         @Override
         public ByteBuffer objectToBuffer(Object o) throws IOException, InterruptedException {
            return null;
         }

         @Override
         public boolean isMarshallable(Object o) throws Exception {
            return false;
         }

         @Override
         public BufferSizePredictor getBufferSizePredictor(Object o) {
            return null;
         }

         @Override
         public MediaType mediaType() {
            return null;
         }
      });
      EmbeddedTestDriver.fromHolder(new ConfigurationBuilderHolder(gc)).call(driver -> {
         driver.cache(new ConfigurationBuilder());
      });
   }

   @Test
   public void testClusteredCacheInLocal() {
      EmbeddedTestDriver.local().call(driver -> {
         ConfigurationBuilder config = new ConfigurationBuilder();
         config.clustering().cacheMode(CacheMode.REPL_ASYNC);
         assertThatThrownBy(() -> driver.cache(config)).isInstanceOf(CacheConfigurationException.class);
      });
   }

   @Test
   public void testIndexingOnInvalidationCache() {
      ConfigurationBuilder c = new ConfigurationBuilder();
      c.clustering().cacheMode(CacheMode.INVALIDATION_SYNC);
      c.indexing().enable();
      assertThatThrownBy(c::validate)
            .isInstanceOf(CacheConfigurationException.class)
            .hasMessageMatching("ISPN(\\d)*: Indexing can not be enabled on caches in Invalidation mode");
   }

   @Test
   public void testIndexingRequiresOptionalModule() {
      ConfigurationBuilder c = new ConfigurationBuilder();
      c.indexing().enable();
      assertThatThrownBy(() -> c.validate(GlobalConfigurationBuilder.defaultClusteredBuilder().build()))
            .isInstanceOf(CacheConfigurationException.class)
            .hasMessageMatching("ISPN(\\d)*: Indexing can only be enabled if infinispan-query.jar is available on your classpath, and this jar has not been detected.");
   }

   @Test
   public void testInvalidBatchingAndTransactionConfiguration() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.invocationBatching().enable();
      builder.transaction().transactionMode(TransactionMode.TRANSACTIONAL);
      builder.transaction().useSynchronization(false);
      builder.transaction().recovery().enable();
      assertThatThrownBy(builder::validate)
            .isInstanceOf(CacheConfigurationException.class)
            .hasMessageMatching("ISPN(\\d)*: A cache configured with invocation batching can't have recovery enabled");
   }

   @Test
   public void testInvalidRecoveryWithNonTransactional() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL).useSynchronization(false).recovery().enable();
      assertThatThrownBy(builder::validate)
            .isInstanceOf(CacheConfigurationException.class)
            .hasMessageMatching("ISPN(\\d)*: Recovery not supported with non transactional cache");
   }

   @Test
   public void testInvalidRecoveryWithSynchronization() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.transaction().transactionMode(TransactionMode.TRANSACTIONAL).useSynchronization(true).recovery().enable();
      assertThatThrownBy(builder::validate)
            .isInstanceOf(CacheConfigurationException.class)
            .hasMessageMatching("ISPN(\\d)*: Recovery not supported with Synchronization");
   }

   @Test
   public void testValidRecoveryConfiguration() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.transaction().transactionMode(TransactionMode.TRANSACTIONAL).useSynchronization(false).recovery().enable();
      assertTrue(EMBEDDED.cache(builder).getCacheConfiguration().transaction().recovery().enabled());
   }

   @Test
   public void testTransactionConfigurationUnmodified() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      Configuration configuration = builder.build();
      assertFalse(configuration.transaction().attributes().isModified());
   }

   @Test
   public void testMemoryConfigurationUnmodified() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.memory().maxCount(1000);
      Configuration configuration = builder.build();
      assertFalse(configuration.memory().attributes().attribute(MemoryConfiguration.STORAGE).isModified());
      assertFalse(configuration.memory().attributes().attribute(MemoryConfiguration.WHEN_FULL).isModified());
   }

   @Test
   public void testMultipleValidationErrors() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.transaction().reaperWakeUpInterval(-1);
      builder.addModule(NonValidatingBuilder.class);
      try {
         builder.validate();
         fail("Expected CacheConfigurationException");
      } catch (CacheConfigurationException e) {
         assertEquals(2, e.getSuppressed().length);
         assertThat(e.getMessage()).startsWith("ISPN000919");
         assertThat(e.getSuppressed()[0].getMessage()).startsWith("ISPN000344");
         assertEquals("MODULE ERROR", e.getSuppressed()[1].getMessage());
      }

      GlobalConfigurationBuilder global = new GlobalConfigurationBuilder();
      global.security().authorization().enable().principalRoleMapper(null);
      global.addModule(NonValidatingBuilder.class);
      try {
         global.validate();
         fail("Expected CacheConfigurationException");
      } catch (CacheConfigurationException e) {
         assertEquals(2, e.getSuppressed().length);
         assertThat(e.getMessage()).startsWith("ISPN000919");
         assertThat(e.getSuppressed()[0].getMessage()).startsWith("ISPN000288");
         assertEquals("MODULE ERROR", e.getSuppressed()[1].getMessage());
      }
   }

   @Test
   public void testPreloadAndPurgeOnStartupPersistence() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.persistence().addStore(DummyInMemoryStoreConfigurationBuilder.class).preload(true).purgeOnStartup(true);
      assertThatThrownBy(builder::validate).isInstanceOf(CacheConfigurationException.class).hasMessageMatching("ISPN(\\d)*: .*preload and purgeOnStartup");
   }

   @Test
   public void testPassivationAndIgnoreModificationsPersistence() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.persistence().passivation(true).addStore(DummyInMemoryStoreConfigurationBuilder.class).ignoreModifications(true);
      assertThatThrownBy(builder::validate).isInstanceOf(CacheConfigurationException.class).hasMessageMatching("ISPN(\\d)*: .*passivation if it is read only!");
   }

   public static class NonValidatingBuilder implements Builder<Object> {
      public NonValidatingBuilder(GlobalConfigurationBuilder builder) {
      }

      public NonValidatingBuilder(ConfigurationBuilder builder) {
      }

      @Override
      public AttributeSet attributes() {
         return AttributeSet.EMPTY;
      }

      @Override
      public void validate() {
         throw new RuntimeException("MODULE ERROR");
      }

      @Override
      public Object create() {
         return null;
      }

      @Override
      public Builder<?> read(Object template, Combine combine) {
         return this;
      }
   }

   public static class TestResolver implements LSResourceResolver {
      Map<String, String> entities = new HashMap<>();

      public TestResolver() {
         entities.put("urn:org:jgroups", "jgroups-5.3.xsd");
         entities.put("urn:jgroups:relay:1.0", "relay.xsd");
         entities.put("fork", "fork-stacks.xsd");
      }

      @Override
      public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
         String entity = entities.get(namespaceURI);
         if (entity != null) {
            InputStream is = this.loadResource(entity);
            if (is != null) {
               InputSource inputSource = new InputSource(is);
               inputSource.setSystemId(systemId);
               return new LSInputImpl(type, namespaceURI, publicId, systemId, baseURI, inputSource);
            }
         }
         return null;
      }

      private InputStream loadResource(String resource) {
         ClassLoader classLoader = this.getClass().getClassLoader();
         InputStream inputStream = loadResource(classLoader, resource);
         if (inputStream == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
            inputStream = this.loadResource(classLoader, resource);
         }

         return inputStream;
      }

      private InputStream loadResource(ClassLoader loader, String resource) {
         URL url = loader.getResource(resource);
         if (url == null) {
            if (resource.endsWith(".dtd")) {
               resource = "dtd/" + resource;
            } else if (resource.endsWith(".xsd")) {
               resource = "schema/" + resource;
            }

            url = loader.getResource(resource);
         }

         InputStream inputStream = null;
         if (url != null) {
            try {
               inputStream = url.openStream();
            } catch (IOException e) {
            }
         }

         return inputStream;
      }
   }

   public static class LSInputImpl implements LSInput {
      private final String type;
      private final String namespaceURI;
      private final String publicId;
      private final String systemId;
      private final String baseURI;
      private final InputSource inputSource;

      public LSInputImpl(String type, String namespaceURI, String publicId, String systemId, String baseURI, InputSource inputSource) {
         this.type = type;
         this.namespaceURI = namespaceURI;
         this.publicId = publicId;
         this.systemId = systemId;
         this.baseURI = baseURI;
         this.inputSource = inputSource;
      }

      @Override
      public Reader getCharacterStream() {
         return null;
      }

      @Override
      public void setCharacterStream(Reader characterStream) {
      }

      @Override
      public InputStream getByteStream() {
         return this.inputSource.getByteStream();
      }

      @Override
      public void setByteStream(InputStream byteStream) {

      }

      @Override
      public String getStringData() {
         return null;
      }

      @Override
      public void setStringData(String stringData) {

      }

      @Override
      public String getSystemId() {
         return systemId;
      }

      @Override
      public void setSystemId(String systemId) {

      }

      @Override
      public String getPublicId() {
         return publicId;
      }

      @Override
      public void setPublicId(String publicId) {

      }

      @Override
      public String getBaseURI() {
         return baseURI;
      }

      @Override
      public void setBaseURI(String baseURI) {

      }

      @Override
      public String getEncoding() {
         return null;
      }

      @Override
      public void setEncoding(String encoding) {

      }

      @Override
      public boolean getCertifiedText() {
         return false;
      }

      @Override
      public void setCertifiedText(boolean certifiedText) {

      }
   }
}
