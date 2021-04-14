package org.infinispan.commons.configuration.io.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

import org.infinispan.commons.configuration.io.ConfigurationReader;
import org.infinispan.commons.configuration.io.NamingStrategy;
import org.infinispan.commons.configuration.io.PropertyReplacer;
import org.infinispan.commons.configuration.io.URLConfigurationResourceResolver;
import org.infinispan.commons.util.Version;
import org.junit.Test;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 13.0
 **/
public class JsonConfigurationReaderTest {
   public static final String DEFAULT_NAMESPACE = "urn:infinispan:config:" + Version.getSchemaVersion();

   @Test
   public void testJsonFile() throws IOException {
      try (Reader r = new InputStreamReader(JsonConfigurationReaderTest.class.getResourceAsStream("/json.json"))) {
         Properties properties = new Properties();
         properties.put("opinion", "JSON is pretty nice");
         properties.put("fact", "JSON is ok");
         JsonConfigurationReader json = new JsonConfigurationReader(r, new URLConfigurationResourceResolver(null), properties, PropertyReplacer.DEFAULT, NamingStrategy.KEBAB_CASE);
         json.require(ConfigurationReader.ElementType.START_DOCUMENT);
         json.nextElement();
         json.require(ConfigurationReader.ElementType.START_ELEMENT, DEFAULT_NAMESPACE, "item1");
         assertEquals(2, json.getAttributeCount());
         assertAttribute(json, "item5", "v5");
         assertAttribute(json, "item6", "v6");
         json.nextElement();
         json.require(ConfigurationReader.ElementType.START_ELEMENT, DEFAULT_NAMESPACE, "item2");
         assertEquals(4, json.getAttributeCount());
         assertAttribute(json, "a", "1");
         assertAttribute(json, "b", "2");
         assertAttribute(json, "c", "3");
         assertAttribute(json, "d", "4");
         json.nextElement();
         json.require(ConfigurationReader.ElementType.START_ELEMENT, DEFAULT_NAMESPACE, "camel-item3");
         assertEquals("camel-attribute", json.getAttributeName(0));
         assertEquals(properties.getProperty("opinion"), json.getAttributeValue(0));
         assertEquals("another-camel-attribute", json.getAttributeName(1));
         assertEquals(properties.getProperty("fact"), json.getAttributeValue(1));
         json.nextElement();
         json.require(ConfigurationReader.ElementType.END_ELEMENT, DEFAULT_NAMESPACE, "camel-item3");
         json.nextElement();
         json.require(ConfigurationReader.ElementType.END_ELEMENT, DEFAULT_NAMESPACE, "item2");
         json.nextElement();
         json.require(ConfigurationReader.ElementType.START_ELEMENT, DEFAULT_NAMESPACE, "item7");
         json.nextElement();
         json.require(ConfigurationReader.ElementType.START_ELEMENT, DEFAULT_NAMESPACE, "item7");
         assertEquals("v7", json.getElementText());
         json.nextElement();
         json.require(ConfigurationReader.ElementType.START_ELEMENT, DEFAULT_NAMESPACE, "item7");
         assertEquals("v8", json.getElementText());
         json.nextElement();
         json.require(ConfigurationReader.ElementType.END_ELEMENT, DEFAULT_NAMESPACE, "item7");
         json.nextElement();
         json.require(ConfigurationReader.ElementType.END_ELEMENT, DEFAULT_NAMESPACE, "item1");
         json.nextElement();
         json.require(ConfigurationReader.ElementType.END_DOCUMENT);
      }
   }

   private void assertAttribute(JsonConfigurationReader json, String name, String value) {
      for (int i = 0; i < json.getAttributeCount(); i++) {
         if (name.equals(json.getAttributeName(i))) {
            assertEquals(value, json.getAttributeValue(i));
            return;
         }
      }
      fail("Could not find attribute '" + name + " in element '" + json.getLocalName() + "'");
   }
}
