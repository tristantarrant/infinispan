package org.infinispan.persistence.http.configuration;

import static org.infinispan.persistence.http.configuration.HttpStoreConfiguration.BASE_URL;
import static org.infinispan.persistence.http.configuration.HttpStoreConfiguration.BEARER_TOKEN;
import static org.infinispan.persistence.http.configuration.HttpStoreConfiguration.PASSWORD;
import static org.infinispan.persistence.http.configuration.HttpStoreConfiguration.USERNAME;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.Combine;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;

public class HttpStoreConfigurationBuilder extends AbstractStoreConfigurationBuilder<HttpStoreConfiguration, HttpStoreConfigurationBuilder> {

   public HttpStoreConfigurationBuilder(PersistenceConfigurationBuilder builder) {
      this(builder, HttpStoreConfiguration.attributeDefinitionSet());
   }

   public HttpStoreConfigurationBuilder(PersistenceConfigurationBuilder builder, AttributeSet attributeSet) {
      super(builder, attributeSet);
   }

   public HttpStoreConfigurationBuilder baseUrl(String baseUrl) {
      attributes.attribute(BASE_URL).set(baseUrl);
      return self();
   }

   public HttpStoreConfigurationBuilder username(String username) {
      attributes.attribute(USERNAME).set(username);
      return self();
   }

   public HttpStoreConfigurationBuilder password(String password) {
      attributes.attribute(PASSWORD).set(password);
      return self();
   }

   public HttpStoreConfigurationBuilder bearerToken(String bearerToken) {
      attributes.attribute(BEARER_TOKEN).set(bearerToken);
      return self();
   }

   @Override
   public void validate() {
      super.validate();
      if (attributes.attribute(BASE_URL).get() == null) {
         throw new IllegalArgumentException("base-url must not be null");
      }
      String username = attributes.attribute(USERNAME).get();
      String password = attributes.attribute(PASSWORD).get();
      String bearerToken = attributes.attribute(BEARER_TOKEN).get();
      if (bearerToken != null && (username != null || password != null)) {
         throw new IllegalArgumentException("Cannot configure both bearer-token and username/password authentication");
      }
      if ((username == null) != (password == null)) {
         throw new IllegalArgumentException("Both username and password must be specified for basic authentication");
      }
   }

   @Override
   public HttpStoreConfiguration create() {
      return new HttpStoreConfiguration(attributes.protect(), async.create());
   }

   @Override
   public Builder<?> read(HttpStoreConfiguration template, Combine combine) {
      super.read(template, combine);
      return self();
   }

   @Override
   public HttpStoreConfigurationBuilder self() {
      return this;
   }
}
