package org.infinispan.security;

import java.util.concurrent.CompletionStage;

/**
 * @since 14.0
 **/
public interface MutableRolePermissionMapper extends RolePermissionMapper {
   CompletionStage<Void> addRole(Role role);

   CompletionStage<Void> removeRole(String role);
}
