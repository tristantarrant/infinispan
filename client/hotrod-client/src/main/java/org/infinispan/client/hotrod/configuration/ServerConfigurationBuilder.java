package org.infinispan.client.hotrod.configuration;

import static org.infinispan.client.hotrod.configuration.ServerConfiguration.HOST;
import static org.infinispan.client.hotrod.configuration.ServerConfiguration.PORT;
import static org.infinispan.client.hotrod.logging.Log.HOTROD;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.Combine;
import org.infinispan.commons.configuration.attributes.AttributeSet;

/**
 * ServerConfigurationBuilder.
 *
 * @author Tristan Tarrant
 * @since 5.3
 */
public class ServerConfigurationBuilder extends AbstractConfigurationChildBuilder implements Builder<ServerConfiguration> {
   private final AttributeSet attributes;

   ServerConfigurationBuilder(ConfigurationBuilder builder) {
      super(builder);
      attributes = ServerConfiguration.attributeDefinitionSet();
   }

   @Override
   public AttributeSet attributes() {
      return attributes;
   }

   public ServerConfigurationBuilder host(String host) {
      this.attributes.attribute(HOST).set(host);
      return this;
   }

   public ServerConfigurationBuilder port(int port) {
      this.attributes.attribute(PORT).set(port);
      return this;
   }

   @Override
   public void validate() {
      if (attributes.attribute(HOST).isNull()) {
         throw HOTROD.missingHostDefinition();
      }
   }

   @Override
   public ServerConfiguration create() {
      return new ServerConfiguration(attributes.protect());
   }

   @Override
   public ServerConfigurationBuilder read(ServerConfiguration template, Combine combine) {
      this.attributes.read(template.attributes(), combine);
      return this;
   }

}
