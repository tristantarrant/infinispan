package org.infinispan.persistence.http.configuration;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.ConfigurationFor;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.serializing.SerializedWith;
import org.infinispan.persistence.http.HttpStore;

@ConfigurationFor(HttpStore.class)
@BuiltBy(HttpStoreConfigurationBuilder.class)
@SerializedWith(HttpStoreConfigurationSerializer.class)
public class HttpStoreConfiguration extends AbstractStoreConfiguration<HttpStoreConfiguration> {

   static final AttributeDefinition<String> BASE_URL = AttributeDefinition.builder(Attribute.BASE_URL, null, String.class).immutable().build();
   static final AttributeDefinition<String> USERNAME = AttributeDefinition.builder(Attribute.USERNAME, null, String.class).immutable().autoPersist(false).build();
   static final AttributeDefinition<String> PASSWORD = AttributeDefinition.builder(Attribute.PASSWORD, null, String.class).immutable().autoPersist(false).build();
   static final AttributeDefinition<String> BEARER_TOKEN = AttributeDefinition.builder(Attribute.BEARER_TOKEN, null, String.class).immutable().autoPersist(false).build();

   public static AttributeSet attributeDefinitionSet() {
      return new AttributeSet(HttpStoreConfiguration.class, AbstractStoreConfiguration.attributeDefinitionSet(),
            BASE_URL, USERNAME, PASSWORD, BEARER_TOKEN);
   }

   public HttpStoreConfiguration(AttributeSet attributes, AsyncStoreConfiguration async) {
      super(Element.HTTP_STORE, attributes, async);
   }

   public String baseUrl() {
      return attributes.attribute(BASE_URL).get();
   }

   public String username() {
      return attributes.attribute(USERNAME).get();
   }

   public String password() {
      return attributes.attribute(PASSWORD).get();
   }

   public String bearerToken() {
      return attributes.attribute(BEARER_TOKEN).get();
   }
}
