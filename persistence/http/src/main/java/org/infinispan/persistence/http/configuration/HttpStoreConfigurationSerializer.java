package org.infinispan.persistence.http.configuration;

import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.configuration.io.ConfigurationWriter;
import org.infinispan.commons.util.Version;
import org.infinispan.configuration.serializing.AbstractStoreSerializer;
import org.infinispan.configuration.serializing.ConfigurationSerializer;

public class HttpStoreConfigurationSerializer extends AbstractStoreSerializer implements ConfigurationSerializer<HttpStoreConfiguration> {

   @Override
   public void serialize(ConfigurationWriter writer, HttpStoreConfiguration configuration) {
      AttributeSet attributes = configuration.attributes();
      writer.writeStartElement(Element.HTTP_STORE);
      writer.writeDefaultNamespace(HttpStoreConfigurationParser.NAMESPACE + Version.getMajorMinor());
      attributes.write(writer);
      writeCommonStoreSubAttributes(writer, configuration);
      boolean hasBasic = attributes.attribute(HttpStoreConfiguration.USERNAME).isModified();
      boolean hasBearer = attributes.attribute(HttpStoreConfiguration.BEARER_TOKEN).isModified();
      if (hasBasic || hasBearer) {
         writer.writeStartElement(Element.AUTHENTICATION);
         if (hasBasic) {
            attributes.write(writer, HttpStoreConfiguration.USERNAME, Attribute.USERNAME);
            attributes.write(writer, HttpStoreConfiguration.PASSWORD, Attribute.PASSWORD);
         }
         if (hasBearer) {
            attributes.write(writer, HttpStoreConfiguration.BEARER_TOKEN, Attribute.BEARER_TOKEN);
         }
         writer.writeEndElement();
      }
      writeCommonStoreElements(writer, configuration);
      writer.writeEndElement();
   }
}
