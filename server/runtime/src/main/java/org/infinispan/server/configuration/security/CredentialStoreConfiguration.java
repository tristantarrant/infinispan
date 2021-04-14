package org.infinispan.server.configuration.security;

import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.configuration.attributes.ConfigurationElement;
import org.infinispan.server.configuration.Attribute;
import org.infinispan.server.configuration.Element;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 12.0
 **/
public class CredentialStoreConfiguration extends ConfigurationElement<CredentialStoresConfiguration> {
   public static final AttributeDefinition<String> NAME = AttributeDefinition.builder(Attribute.NAME, null, String.class).build();
   public static final AttributeDefinition<String> PATH = AttributeDefinition.builder(Attribute.PATH, null, String.class).build();
   public static final AttributeDefinition<String> RELATIVE_TO = AttributeDefinition.builder(Attribute.RELATIVE_TO, null, String.class).build();
   public static final AttributeDefinition<String> TYPE = AttributeDefinition.builder(Attribute.TYPE, "pkcs12", String.class).build();
   public static final AttributeDefinition<String> CREDENTIAL = AttributeDefinition.builder(Attribute.CREDENTIAL, null, String.class).autoPersist(false).build();

   static AttributeSet attributeDefinitionSet() {
      return new AttributeSet(CredentialStoreConfiguration.class, NAME, PATH, RELATIVE_TO, TYPE, CREDENTIAL);
   }

   CredentialStoreConfiguration(AttributeSet attributes) {
      super(Element.CREDENTIAL_STORE, attributes);
   }
}
