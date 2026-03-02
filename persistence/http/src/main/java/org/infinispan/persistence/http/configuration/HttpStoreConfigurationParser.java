package org.infinispan.persistence.http.configuration;

import static org.infinispan.persistence.http.configuration.HttpStoreConfigurationParser.NAMESPACE;

import org.infinispan.commons.configuration.io.ConfigurationReader;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.parsing.CacheParser;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ConfigurationParser;
import org.infinispan.configuration.parsing.Namespace;
import org.infinispan.configuration.parsing.ParseUtils;
import org.infinispan.configuration.parsing.Parser;
import org.kohsuke.MetaInfServices;

@MetaInfServices
@Namespace(root = "http-store")
@Namespace(uri = NAMESPACE + "*", root = "http-store")
public class HttpStoreConfigurationParser implements ConfigurationParser {

   static final String NAMESPACE = Parser.NAMESPACE + "store:http:";

   @Override
   public void readElement(ConfigurationReader reader, ConfigurationBuilderHolder holder) {
      ConfigurationBuilder builder = holder.getCurrentConfigurationBuilder();
      Element element = Element.forName(reader.getLocalName());
      switch (element) {
         case HTTP_STORE: {
            parseHttpStore(reader, builder.persistence().addStore(HttpStoreConfigurationBuilder.class));
            break;
         }
         default: {
            throw ParseUtils.unexpectedElement(reader);
         }
      }
   }

   private void parseHttpStore(ConfigurationReader reader, HttpStoreConfigurationBuilder builder) {
      for (int i = 0; i < reader.getAttributeCount(); i++) {
         ParseUtils.requireNoNamespaceAttribute(reader, i);
         String value = reader.getAttributeValue(i);
         Attribute attribute = Attribute.forName(reader.getAttributeName(i));

         switch (attribute) {
            case BASE_URL: {
               builder.baseUrl(value);
               break;
            }
            default: {
               CacheParser.parseStoreAttribute(reader, i, builder);
            }
         }
      }

      while (reader.inTag()) {
         Element element = Element.forName(reader.getLocalName());
         switch (element) {
            case AUTHENTICATION: {
               parseAuthentication(reader, builder);
               break;
            }
            default: {
               CacheParser.parseStoreElement(reader, builder);
            }
         }
      }
   }

   private void parseAuthentication(ConfigurationReader reader, HttpStoreConfigurationBuilder builder) {
      for (int i = 0; i < reader.getAttributeCount(); i++) {
         ParseUtils.requireNoNamespaceAttribute(reader, i);
         String value = reader.getAttributeValue(i);
         Attribute attribute = Attribute.forName(reader.getAttributeName(i));
         switch (attribute) {
            case USERNAME: {
               builder.username(value);
               break;
            }
            case PASSWORD: {
               builder.password(value);
               break;
            }
            case BEARER_TOKEN: {
               builder.bearerToken(value);
               break;
            }
            default:
               throw ParseUtils.unexpectedAttribute(reader, i);
         }
      }
      ParseUtils.requireNoContent(reader);
   }

   @Override
   public Namespace[] getNamespaces() {
      return ParseUtils.getNamespaceAnnotations(getClass());
   }
}
