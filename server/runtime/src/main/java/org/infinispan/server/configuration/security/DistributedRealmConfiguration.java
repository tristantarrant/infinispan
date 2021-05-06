package org.infinispan.server.configuration.security;

import java.util.ArrayList;
import java.util.List;

import org.infinispan.commons.configuration.ConfigurationInfo;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.configuration.elements.DefaultElementDefinition;
import org.infinispan.commons.configuration.elements.ElementDefinition;
import org.infinispan.server.configuration.Element;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 13.0
 **/
public class DistributedRealmConfiguration implements ConfigurationInfo {
   static final AttributeDefinition<String> NAME = AttributeDefinition.builder("name", null, String.class).build();
   static final AttributeDefinition<List<String>> REALMS = AttributeDefinition.builder("realms", new ArrayList<>(), (Class<List<String>>) (Class<?>) List.class)
         .initializer(ArrayList::new).immutable().build();

   static AttributeSet attributeDefinitionSet() {
      return new AttributeSet(DistributedRealmConfiguration.class, NAME, REALMS);
   }

   private static ElementDefinition ELEMENT_DEFINITION = new DefaultElementDefinition(Element.TOKEN_REALM.toString());
   private final AttributeSet attributes;

   DistributedRealmConfiguration(AttributeSet attributes) {
      this.attributes = attributes.checkProtection();
   }

   public String name() {
      return attributes.attribute(NAME).get();
   }

   public List<String> realms() {
      return attributes.attribute(REALMS).get();
   }
}
