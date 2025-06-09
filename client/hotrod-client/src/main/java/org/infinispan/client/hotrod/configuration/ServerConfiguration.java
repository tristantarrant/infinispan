package org.infinispan.client.hotrod.configuration;

import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.configuration.attributes.ConfigurationElement;

/**
 * ServerConfiguration.
 *
 * @author Tristan Tarrant
 * @since 5.3
 */
@BuiltBy(ServerConfigurationBuilder.class)
public class ServerConfiguration extends ConfigurationElement<ServerConfiguration> {
   public static final AttributeDefinition<String> HOST = AttributeDefinition.builder("host", null, String.class).build();
   public static final AttributeDefinition<Integer> PORT = AttributeDefinition.builder("port", ConfigurationProperties.DEFAULT_HOTROD_PORT, Integer.class).build();

   static AttributeSet attributeDefinitionSet() {
      return new AttributeSet(ServerConfiguration.class, HOST, PORT);
   }

   ServerConfiguration(AttributeSet attributes) {
      super("server", attributes);
   }

   public String host() {
      return attributes.attribute(HOST).get();
   }

   public int port() {
      return attributes.attribute(PORT).get();
   }
}
