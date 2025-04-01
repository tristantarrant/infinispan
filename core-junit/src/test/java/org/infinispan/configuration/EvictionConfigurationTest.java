package org.infinispan.configuration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.infinispan.configuration.cache.StorageType.BINARY;
import static org.infinispan.configuration.cache.StorageType.HEAP;
import static org.infinispan.configuration.cache.StorageType.OBJECT;
import static org.infinispan.configuration.cache.StorageType.OFF_HEAP;
import static org.infinispan.eviction.EvictionStrategy.REMOVE;
import static org.infinispan.eviction.EvictionType.COUNT;
import static org.infinispan.eviction.EvictionType.MEMORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.wildfly.common.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.Queue;

import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.configuration.Combine;
import org.infinispan.commons.configuration.io.ConfigurationResourceResolvers;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.MemoryConfiguration;
import org.infinispan.configuration.cache.MemoryStorageConfiguration;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class EvictionConfigurationTest {
   private static final ParserRegistry REGISTRY = new ParserRegistry();

   @Test
   public void testReuseLegacyBuilder() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.memory().size(200);

      Configuration configuration = builder.build();

      assertEquals(-1, configuration.memory().maxSizeBytes());
      assertEquals(200, configuration.memory().maxCount());
      assertEquals(HEAP, configuration.memory().storageType());

      Configuration fromSameBuilder = builder.build();
      assertEquals(configuration, fromSameBuilder);
   }

   @Test
   public void testReuseChangeLegacy() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.memory().storageType(OBJECT).size(12);

      Configuration conf1 = builder.build();
      assertEquals(OBJECT, conf1.memory().storageType());
      assertEquals(12, conf1.memory().size());

      builder.memory().storageType(BINARY);
      Configuration build2 = builder.build();
      assertEquals(BINARY, build2.memory().storageType());
      assertEquals(12, build2.memory().size());
   }

   @Test
   public void testEvictionDisabled() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.memory().storageType(StorageType.BINARY);

      Configuration configuration = builder.build();

      assertEquals(StorageType.BINARY, configuration.memory().storageType());
      assertEquals(-1, configuration.memory().maxSizeBytes());
      assertEquals(-1, configuration.memory().maxCount());

      Configuration same = builder.build();
      assertEquals(configuration, same);
   }

   @Test
   public void testLegacyConfigAvailable() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.memory().maxSize("1.5 GB").storage(HEAP).whenFull(REMOVE);
      builder.encoding().mediaType(MediaType.APPLICATION_PROTOSTREAM_TYPE);

      Configuration configuration = builder.build();

      assertEquals(1_500_000_000, configuration.memory().maxSizeBytes());
      assertEquals(-1, configuration.memory().maxCount());
      assertEquals(REMOVE, configuration.memory().whenFull());
      assertEquals(HEAP, configuration.memory().storageType());
      assertEquals(REMOVE, configuration.memory().evictionStrategy());
      assertEquals(1_500_000_000, configuration.memory().size());
      assertEquals(MEMORY, configuration.memory().evictionType());

      Configuration same = builder.build();
      assertEquals(configuration.memory(), same.memory());

      Configuration larger = builder.memory().maxSize("2.0 GB").build();

      assertEquals(2_000_000_000, larger.memory().maxSizeBytes());
      assertEquals(-1, larger.memory().maxCount());
      assertEquals(REMOVE, larger.memory().whenFull());
      assertEquals(HEAP, larger.memory().storage());
      assertEquals(HEAP, larger.memory().storageType());
      assertEquals(REMOVE, larger.memory().evictionStrategy());
      assertEquals(2_000_000_000, larger.memory().size());
      assertEquals(MEMORY, larger.memory().evictionType());
   }

   @Test
   public void testUseDefaultEviction() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.encoding().mediaType(MediaType.APPLICATION_PROTOSTREAM_TYPE);

      Configuration configuration = builder.build();

      assertFalse(configuration.memory().isEvictionEnabled());
      assertEquals(HEAP, configuration.memory().storage());
      assertEquals(HEAP, configuration.memory().storageType());
   }

   @Test
   public void testPreventUsingLegacyAndNew() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.memory().size(44).evictionType(COUNT);

      Configuration conf = builder.build();
      assertEquals(44, conf.memory().maxCount());

      builder.memory().maxCount(10).size(12);
      assertThatThrownBy(builder::build).isInstanceOf(CacheConfigurationException.class);
   }

   @Test
   public void testMinimal() {
      Configuration configuration = new ConfigurationBuilder().build();

      assertFalse(configuration.memory().isOffHeap());
      assertEquals(-1L, configuration.memory().maxCount());
      assertEquals(-1L, configuration.memory().maxSizeBytes());
      assertEquals(HEAP, configuration.memory().storageType());
   }

   @Test
   public void testChangeFromMinimal() {
      ConfigurationBuilder initial = new ConfigurationBuilder();
      Configuration initialConfig = initial.build();
      assertEquals(HEAP, initialConfig.memory().storageType());
      assertEquals(-1, initialConfig.memory().size());

      initial.memory().size(3);
      Configuration larger = initial.build();
      assertEquals(HEAP, larger.memory().storageType());
      assertEquals(3, larger.memory().size());
      assertEquals(3, larger.memory().maxCount());
      assertEquals(HEAP, larger.memory().storage());
   }

   @Test
   public void testRuntimeConfigChanges() {
      Configuration countBounded = new ConfigurationBuilder().memory().maxCount(1000).build();
      Configuration sizeBounded = new ConfigurationBuilder().memory().maxSize("10 MB").storage(OFF_HEAP).build();

      countBounded.memory().maxCount(1200);
      sizeBounded.memory().maxSize("20MB");

      assertEquals(1200, countBounded.memory().maxCount());
      assertEquals(20_000_000, sizeBounded.memory().maxSizeBytes());
      assertThatThrownBy(() -> countBounded.memory().maxSize("30MB")).isInstanceOf(CacheConfigurationException.class);
      assertThatThrownBy(() -> sizeBounded.memory().maxCount(2000)).isInstanceOf(CacheConfigurationException.class);
   }

   @Test
   public void testParseXML() {
      String xml = """
            <infinispan>
               <cache-container>
                  <local-cache name="local">
                     <memory storage="OFF_HEAP" max-size="200 MB" when-full="MANUAL" />
                  </local-cache>
               </cache-container>
            </infinispan>""";

      testSerializationAndBack(xml);

      ConfigurationBuilderHolder parsed = new ParserRegistry().parse(xml);
      ConfigurationBuilder parsedBuilder = parsed.getNamedConfigurationBuilders().get("local");
      Configuration afterParsing = parsedBuilder.build();

      assertEquals(200_000_000, afterParsing.memory().maxSizeBytes());
      assertEquals(-1, afterParsing.memory().maxCount());
      assertEquals(OFF_HEAP, afterParsing.memory().storageType());
      // Remove is forced
      assertEquals(REMOVE, afterParsing.memory().evictionStrategy());
      assertEquals(200_000_000, afterParsing.memory().size());
      assertEquals(MEMORY, afterParsing.memory().evictionType());
      assertEquals(REMOVE, afterParsing.memory().heapConfiguration().evictionStrategy());
   }

   @Test
   public void testParseXML2() {
      String xmlNew = """
            <infinispan>
               <cache-container>
                  <local-cache name="local">
                     <memory max-count="2000" when-full="REMOVE" />
                  </local-cache>
               </cache-container>
            </infinispan>""";

      testSerializationAndBack(xmlNew);

      ConfigurationBuilderHolder parsed = new ParserRegistry().parse(xmlNew);
      ConfigurationBuilder parsedBuilder = parsed.getNamedConfigurationBuilders().get("local");
      Configuration afterParsing = parsedBuilder.build();

      assertEquals(2000, afterParsing.memory().maxCount());
      assertEquals(REMOVE, afterParsing.memory().whenFull());

      assertEquals(HEAP, afterParsing.memory().storageType());
      assertEquals(REMOVE, afterParsing.memory().evictionStrategy());
      assertEquals(2000, afterParsing.memory().size());
      assertEquals(COUNT, afterParsing.memory().evictionType());
      assertEquals(REMOVE, afterParsing.memory().heapConfiguration().evictionStrategy());
   }

   @Test
   public void testParseXML3() {
      String xmlNew = """
            <infinispan>
               <cache-container>
                  <local-cache name="local">
                     <encoding media-type="application/json" />
                     <memory storage="HEAP" max-size="1MB" when-full="REMOVE"/>
                  </local-cache>
               </cache-container>
            </infinispan>""";

      testSerializationAndBack(xmlNew);

      ConfigurationBuilderHolder parsed = new ParserRegistry().parse(xmlNew);
      ConfigurationBuilder parsedBuilder = parsed.getNamedConfigurationBuilders().get("local");
      Configuration afterParsing = parsedBuilder.build();

      assertTrue(afterParsing.memory().isEvictionEnabled());
      assertEquals(HEAP, afterParsing.memory().storage());
      assertEquals(1_000_000, afterParsing.memory().maxSizeBytes());
      assertEquals(REMOVE, afterParsing.memory().whenFull());
      assertEquals(HEAP, afterParsing.memory().storageType());
      assertEquals(1_000_000, afterParsing.memory().size());
      assertEquals(REMOVE, afterParsing.memory().evictionStrategy());
      assertEquals(MEMORY, afterParsing.memory().evictionType());
   }


   @Test
   public void testParseJSON() {
      ConfigurationBuilderHolder holder = new ParserRegistry().parse("""
            {
               "local-cache": {
                  "memory": {
                     "storage": "HEAP",
                     "when-full":"REMOVE",
                     "max-count":5000
                  }
               }
            }""", MediaType.APPLICATION_JSON);
      Configuration fromJson = holder.getCurrentConfigurationBuilder().build();
      assertEquals(-1, fromJson.memory().maxSizeBytes());
      assertEquals(5000, fromJson.memory().maxCount());
      assertEquals(HEAP, fromJson.memory().storageType());
      assertEquals(REMOVE, fromJson.memory().evictionStrategy());
      assertEquals(5000, fromJson.memory().size());
      assertEquals(COUNT, fromJson.memory().evictionType());
      assertEquals(REMOVE, fromJson.memory().heapConfiguration().evictionStrategy());
   }

   @Test
   public void testParseLegacyJSON() {
      ConfigurationBuilderHolder holder = new ParserRegistry().parse("""
            {
               "local-cache": {
                  "memory": {
                     "storage":"OBJECT",
                     "when-full":"REMOVE",
                     "max-count":5000
                  }
               }
            }""", MediaType.APPLICATION_JSON);
      Configuration fromJson = holder.getCurrentConfigurationBuilder().build();
      assertEquals(-1, fromJson.memory().maxSizeBytes());
      assertEquals(5000, fromJson.memory().maxCount());
      assertEquals(OBJECT, fromJson.memory().storageType());
      assertEquals(REMOVE, fromJson.memory().evictionStrategy());
      assertEquals(5000, fromJson.memory().size());
      assertEquals(COUNT, fromJson.memory().evictionType());
      assertEquals(REMOVE, fromJson.memory().heapConfiguration().evictionStrategy());
   }

   @Test
   public void testBuildWithLegacyConfiguration() {
      ConfigurationBuilder configBuilder = new ConfigurationBuilder();
      configBuilder.memory().storageType(OFF_HEAP).size(1_000).evictionType(COUNT);
      Configuration configuration = configBuilder.build();
      Configuration afterRead = new ConfigurationBuilder().read(configuration, Combine.DEFAULT).build();

      assertEquals(OFF_HEAP, afterRead.memory().storage());
      assertEquals(REMOVE, afterRead.memory().whenFull());
      assertEquals(1_000, afterRead.memory().maxCount());
      assertEquals(-1, afterRead.memory().maxSizeBytes());
      assertEquals(OFF_HEAP, afterRead.memory().storageType());
      assertEquals(REMOVE, afterRead.memory().evictionStrategy());
      assertEquals(1_000, afterRead.memory().size());
      assertEquals(COUNT, afterRead.memory().evictionType());
      assertEquals(REMOVE, afterRead.memory().heapConfiguration().evictionStrategy());
   }

   @Test
   public void testBuildWithLegacyConfiguration2() {
      ConfigurationBuilder configBuilder = new ConfigurationBuilder();
      configBuilder.memory().storageType(HEAP).size(120);
      Configuration afterRead = new ConfigurationBuilder().read(configBuilder.build(), Combine.DEFAULT).build();

      assertEquals(-1, afterRead.memory().maxSizeBytes());
      assertEquals(120, afterRead.memory().maxCount());
      assertEquals(HEAP, afterRead.memory().storageType());
      assertEquals(120, afterRead.memory().size());
      assertEquals(COUNT, afterRead.memory().evictionType());

      ConfigurationBuilder override = new ConfigurationBuilder().read(afterRead, Combine.DEFAULT);
      Configuration overridden = override.memory().size(400).build();
      assertEquals(-1, overridden.memory().maxSizeBytes());
      assertEquals(400, overridden.memory().maxCount());
      assertEquals(HEAP, overridden.memory().storageType());
      assertEquals(400, overridden.memory().size());
      assertEquals(COUNT, overridden.memory().evictionType());
   }

   @Test
   public void testListenToCountChanges() {
      ConfigurationBuilder countBuilder = new ConfigurationBuilder();
      countBuilder.memory().storage(HEAP).maxCount(20);

      Configuration configuration = countBuilder.build();
      assertEquals(COUNT, configuration.memory().evictionType());

      Queue<Object> sizeListenerQueue = new ArrayDeque<>(1);
      Queue<Object> maxCountListenerQueue = new ArrayDeque<>(1);
      Queue<Object> maxSizeListenerQueue = new ArrayDeque<>(1);
      setUpListeners(configuration, sizeListenerQueue, maxCountListenerQueue, maxSizeListenerQueue);

      configuration.memory().size(100);
      assertCountUpdate(configuration, 100, sizeListenerQueue, maxCountListenerQueue, maxSizeListenerQueue);

      configuration.memory().heapConfiguration().size(200);
      assertCountUpdate(configuration, 200, sizeListenerQueue, maxCountListenerQueue, maxSizeListenerQueue);

      configuration.memory().maxCount(300);
      assertCountUpdate(configuration, 300, sizeListenerQueue, maxCountListenerQueue, maxSizeListenerQueue);
   }

   @Test
   public void testListenToSizeChanges() {
      ConfigurationBuilder sizeBuilder = new ConfigurationBuilder();
      sizeBuilder.memory().storage(HEAP).maxSize("20");
      sizeBuilder.encoding().mediaType(MediaType.APPLICATION_PROTOSTREAM_TYPE);

      Configuration configuration = sizeBuilder.build();
      assertEquals(MEMORY, configuration.memory().evictionType());

      Queue<Object> sizeListenerQueue = new ArrayDeque<>(1);
      Queue<Object> maxSizeListenerQueue = new ArrayDeque<>(1);
      Queue<Object> maxCountListenerQueue = new ArrayDeque<>(1);
      setUpListeners(configuration, sizeListenerQueue, maxCountListenerQueue, maxSizeListenerQueue);

      configuration.memory().size(100);
      assertSizeUpdate(configuration, 100, sizeListenerQueue, maxCountListenerQueue, maxSizeListenerQueue);

      configuration.memory().heapConfiguration().size(200);
      assertSizeUpdate(configuration, 200, sizeListenerQueue, maxCountListenerQueue, maxSizeListenerQueue);

      configuration.memory().maxSize("300");
      assertSizeUpdate(configuration, 300, sizeListenerQueue, maxCountListenerQueue, maxSizeListenerQueue);
   }

   private void setUpListeners(Configuration configuration, Queue<Object> sizeListenerQueue, Queue<Object> maxCountListenerQueue, Queue<Object> maxSizeListenerQueue) {
      configuration.memory().heapConfiguration().attributes().attribute(MemoryStorageConfiguration.SIZE).addListener((attribute, oldValue) -> sizeListenerQueue.add(attribute.get()));
      configuration.memory().attributes().attribute(MemoryConfiguration.MAX_COUNT).addListener((attribute, oldValue) -> maxCountListenerQueue.add(attribute.get()));
      configuration.memory().attributes().attribute(MemoryConfiguration.MAX_SIZE).addListener((attribute, oldValue) -> maxSizeListenerQueue.add(attribute.get()));
   }

   private void assertCountUpdate(Configuration configuration, long newValue, Queue<Object> sizeListenerQueue, Queue<Object> maxCountListenerQueue, Queue<Object> maxSizeListenerQueue) {
      assertEquals(newValue, configuration.memory().size());
      assertEquals(newValue, sizeListenerQueue.poll());

      assertEquals(newValue, configuration.memory().maxCount());
      assertEquals(newValue, maxCountListenerQueue.poll());

      assertEquals(-1L, configuration.memory().maxSizeBytes());
      assertEquals(0, maxSizeListenerQueue.size());
   }

   private void assertSizeUpdate(Configuration configuration, long newValue, Queue<Object> sizeListenerQueue, Queue<Object> maxCountListenerQueue, Queue<Object> maxSizeListenerQueue) {
      assertEquals(newValue, configuration.memory().size());
      assertEquals(newValue, sizeListenerQueue.poll());

      assertEquals(String.valueOf(newValue), configuration.memory().maxSize());
      assertEquals(newValue, configuration.memory().maxSizeBytes());
      assertEquals(String.valueOf(newValue), maxSizeListenerQueue.poll());

      assertEquals(-1L, configuration.memory().maxCount());
      assertEquals(0, maxCountListenerQueue.size());
   }

   @Test
   public void testErrorForMultipleThresholds() {
      ConfigurationBuilder configBuilder = new ConfigurationBuilder();
      configBuilder.memory().storage(OFF_HEAP).maxCount(10).maxSize("10TB");

      assertThatThrownBy(configBuilder::build)
            .isInstanceOf(CacheConfigurationException.class)
            .hasMessageMatching(".*Cannot configure both maxCount and maxSize.*");
   }

   private void testSerializationAndBack(String xml) {
      // Parse config
      ConfigurationBuilderHolder configurationBuilderHolder = REGISTRY.parse(xml);
      ConfigurationBuilder builder = configurationBuilderHolder.getNamedConfigurationBuilders().get("local");
      Configuration before = builder.build();

      // Serialize the parsed config
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      REGISTRY.serialize(baos, "local", before);
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      ConfigurationBuilderHolder holderAfter = REGISTRY.parse(bais, ConfigurationResourceResolvers.DEFAULT, MediaType.APPLICATION_XML);

      // Parse again from the serialized
      ConfigurationBuilder afterParsing = holderAfter.getNamedConfigurationBuilders().get("local");

      Configuration after = afterParsing.build();
      assertEquals(after.memory(), before.memory());
   }
}
