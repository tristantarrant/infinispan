package org.infinispan.server.memcached.configuration;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.server.core.admin.AdminOperationsHandler;
import org.infinispan.server.core.configuration.AuthenticationConfigurationBuilder;
import org.infinispan.server.core.configuration.ProtocolServerConfigurationBuilder;
import org.infinispan.server.core.configuration.SaslAuthenticationConfiguration;
import org.infinispan.server.core.configuration.SaslAuthenticationConfigurationBuilder;

/**
 * MemcachedServerConfigurationBuilder.
 *
 * @author Tristan Tarrant
 * @since 5.3
 */
public class MemcachedServerConfigurationBuilder extends ProtocolServerConfigurationBuilder<MemcachedServerConfiguration, MemcachedServerConfigurationBuilder, SaslAuthenticationConfiguration> implements
      Builder<MemcachedServerConfiguration> {
   private final SaslAuthenticationConfigurationBuilder authentication = new SaslAuthenticationConfigurationBuilder(this);

   public MemcachedServerConfigurationBuilder() {
      super(MemcachedServerConfiguration.DEFAULT_MEMCACHED_PORT, MemcachedServerConfiguration.attributeDefinitionSet());
      this.defaultCacheName(MemcachedServerConfiguration.DEFAULT_MEMCACHED_CACHE);
   }

   @Override
   public AttributeSet attributes() {
      return attributes;
   }

   @Override
   public MemcachedServerConfigurationBuilder self() {
      return this;
   }

   /**
    * Use {@link ProtocolServerConfigurationBuilder#defaultCacheName(String)} instead
    */
   @Deprecated
   public MemcachedServerConfigurationBuilder cache(String cache) {
      this.defaultCacheName(cache);
      return this;
   }

   @Override
   public AuthenticationConfigurationBuilder<SaslAuthenticationConfiguration> authentication() {
      return authentication;
   }

   @Override
   public MemcachedServerConfigurationBuilder adminOperationsHandler(AdminOperationsHandler handler) {
      // Ignore
      return this;
   }

   /**
    * The encoding to be used by clients of the memcached text protocol. When not specified, "application/octet-stream" is assumed.
    * When encoding is set, the memcached text server will assume clients will be reading and writing values in that encoding, and
    * will perform the necessary conversions between this encoding and the storage format.
    */
   public MemcachedServerConfigurationBuilder clientEncoding(MediaType payloadType) {
      attributes.attribute(MemcachedServerConfiguration.CLIENT_ENCODING).set(payloadType);
      return this;
   }

   @Override
   public MemcachedServerConfiguration create() {
      return new MemcachedServerConfiguration(attributes.protect(), authentication().create(), ssl.create(), ipFilter.create());
   }

   public MemcachedServerConfiguration build(boolean validate) {
      if (validate) {
         validate();
      }
      return create();
   }

   @Override
   public MemcachedServerConfiguration build() {
      return build(true);
   }

   @Override
   public Builder<?> read(MemcachedServerConfiguration template) {
      super.read(template);
      return this;
   }
}
