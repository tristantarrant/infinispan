package org.infinispan.server.configuration.security;

import static org.infinispan.server.configuration.security.DistributedRealmConfiguration.NAME;
import static org.infinispan.server.configuration.security.DistributedRealmConfiguration.REALMS;

import java.util.Arrays;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.wildfly.security.auth.realm.DistributedSecurityRealm;
import org.wildfly.security.auth.server.SecurityRealm;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 13.0
 **/
public class DistributedRealmConfigurationBuilder implements Builder<DistributedRealmConfiguration> {

   private final RealmConfigurationBuilder realmBuilder;
   private final AttributeSet attributes;
   private DistributedSecurityRealm securityRealm;


   public DistributedRealmConfigurationBuilder(RealmConfigurationBuilder realmConfigurationBuilder) {
      this.realmBuilder = realmConfigurationBuilder;
      this.attributes = DistributedRealmConfiguration.attributeDefinitionSet();
   }

   public DistributedRealmConfigurationBuilder name(String name) {
      attributes.attribute(NAME).set(name);
      return this;
   }

   public DistributedRealmConfigurationBuilder realms(String[] realms) {
      attributes.attribute(REALMS).set(Arrays.asList(realms));
      return this;
   }

   @Override
   public void validate() {

   }

   @Override
   public DistributedRealmConfiguration create() {
      return new DistributedRealmConfiguration(attributes.protect());
   }

   @Override
   public Builder<?> read(DistributedRealmConfiguration template) {
      attributes.read(template.attributes());
      return this;
   }

   public void build() {
      if (securityRealm == null) {
         SecurityRealm realms[] = attributes.attribute(REALMS).get()
               .stream().map(name -> realmBuilder.getRealm(name)).toArray(SecurityRealm[]::new);
         securityRealm = new DistributedSecurityRealm(realms);
         realmBuilder.addRealm(attributes.attribute(NAME).get(), securityRealm);
      }
   }
}
