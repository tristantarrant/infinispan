package org.infinispan.security;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

import javax.security.auth.Subject;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.AuthorizationConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalAuthorizationConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.security.mappers.ClusterPermissionMapper;
import org.infinispan.security.mappers.ClusterRoleMapper;
import org.infinispan.test.SingleCacheManagerTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.Test;

@Test(groups="functional", testName="security.DynamicRBACTest")
public class DynamicRBACTest extends SingleCacheManagerTest {
   static final Subject ADMIN = TestingUtil.makeSubject("admin");
   static final Subject SUBJECT_A = TestingUtil.makeSubject("A");
   static final Subject SUBJECT_B = TestingUtil.makeSubject("B");
   private ClusterRoleMapper crm;
   private ClusterPermissionMapper cpm;

   @Override
   protected EmbeddedCacheManager createCacheManager() throws Exception {
      GlobalConfigurationBuilder global = new GlobalConfigurationBuilder();
      GlobalAuthorizationConfigurationBuilder globalRoles = global.security().authorization().enable()
            .principalRoleMapper(new ClusterRoleMapper())
            .rolePermissionMapper(new ClusterPermissionMapper());
      ConfigurationBuilder config = TestCacheManagerFactory.getDefaultCacheConfiguration(true);
      AuthorizationConfigurationBuilder authConfig = config.security().authorization().enable();

      globalRoles
            .role("reader").permission(AuthorizationPermission.ALL_READ)
            .role("writer").permission(AuthorizationPermission.ALL_WRITE)
            .role("admin").permission(AuthorizationPermission.ALL);
      authConfig.role("reader").role("writer").role("admin");
      return TestCacheManagerFactory.createCacheManager(global, config);
   }

   @Override
   protected void setup() throws Exception {
      Security.doAs(ADMIN, (PrivilegedExceptionAction<Void>) () -> {
         cacheManager = createCacheManager();
         crm = (ClusterRoleMapper) cacheManager.getCacheManagerConfiguration().security().authorization().principalRoleMapper();
         crm.grant("admin", "admin");
         cpm = (ClusterPermissionMapper) cacheManager.getCacheManagerConfiguration().security().authorization().rolePermissionMapper();
         cache = cacheManager.getCache();
         return null;
      });
   }

   public void testClusterPrincipalMapper() {
      crm.grant("writer", "A");
      Security.doAs(SUBJECT_A, (PrivilegedAction<Void>) () -> {
         cacheManager.getCache().put("key", "value");
         return null;
      });
      crm.grant("reader", "B");
      Security.doAs(SUBJECT_B, (PrivilegedAction<Void>) () -> {
         assertEquals("value", cacheManager.getCache().get("key"));
         return null;
      });
   }

   public void testClusterPermissionMapper() {
      Map<String, Role> roles = cpm.getAllRoles();
      assertEquals(0, roles.size());
      cpm.addRole(Role.newRole("wizard", true, AuthorizationPermission.ALL_WRITE));
      cpm.addRole(Role.newRole("cleric", true, AuthorizationPermission.ALL_READ));
      roles = cpm.getAllRoles();
      assertEquals(2, roles.size());
      assertTrue(roles.containsKey("wizard"));
      assertTrue(roles.containsKey("cleric"));
      Cache<String, String> cpmCache = Security.doAs(ADMIN, (PrivilegedAction<Cache<String, String>>) () -> {
         ConfigurationBuilder builder = new ConfigurationBuilder();
         builder.security().authorization().enable().roles("admin", "wizard", "cleric");
         return cacheManager.createCache("cpm", builder.build(cacheManager.getCacheManagerConfiguration()));
      });
      Security.doAs(TestingUtil.makeSubject("wizard"), (PrivilegedAction<Void>) () -> {
         cpmCache.put("key", "value");
         return null;
      });
      Security.doAs(TestingUtil.makeSubject("cleric"), (PrivilegedAction<Void>) () -> {
         assertEquals("value", cpmCache.get("key"));
         return null;
      });
   }

   @Override
   protected void teardown() {
      Security.doAs(ADMIN, (PrivilegedAction<Void>) () -> {
         DynamicRBACTest.super.teardown();
         return null;
      });
   }

   @Override
   protected void clearContent() {
      Security.doAs(ADMIN, (PrivilegedAction<Void>) () -> {
         cacheManager.getCache().clear();
         return null;
      });
   }
}
