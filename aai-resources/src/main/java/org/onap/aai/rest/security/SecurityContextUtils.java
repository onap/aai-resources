/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.aai.rest.security;

import org.onap.aai.rest.data.Permissions;
import org.onap.aai.rest.demoSpecific.DemoUserRepository;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import java.util.UUID;
import org.keycloak.KeycloakSecurityContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityContextUtils {

    private final UserRepository userRepository = new DemoUserRepository();

    public boolean isAuthorized(UUID resource) {
        Set<String> roles = getRoles();

        return isAuthorized(resource, List.ofAll(userRepository.getPermissions()), roles);
    }

    public boolean isAuthorized(UUID resource, KeycloakSecurityContext context) {
        Set<String> roles = getRoles(context);

        return isAuthorized(resource, List.ofAll(userRepository.getPermissions()), roles);
    }

    public Set<String> getRoles(KeycloakSecurityContext context) {
        return Option.of(context)
            .flatMap(ctx -> Option.of(ctx.getToken()))
            .flatMap(token -> Option.of(token.getRealmAccess()))
            .flatMap(access -> Option.of(access.getRoles()))
            .map(HashSet::ofAll)
            .getOrElse(HashSet.empty());
    }

    public Set<String> getRoles() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        if (null != authentication) {
            Object principal = authentication.getPrincipal();
            Set<String> roles = HashSet.ofAll(authentication.getAuthorities())
                .map(GrantedAuthority::getAuthority);

            System.err.printf("Roles for principal '%s': %s%n", principal, roles);
            return roles;
        } else {
            return HashSet.empty();
        }
    }

    public static boolean isAuthorized(UUID resource, List<Permissions> permissions, Set<String> userRoles) {
        Map<UUID, HashSet<String>> permissionsMap = Permissions.toMap(List.ofAll(permissions));

        return isAuthorized(resource, permissionsMap, userRoles);
    }

    public static boolean isAuthorized(UUID resource, Map<UUID, HashSet<String>> permissions, Set<String> userRoles) {
        System.err.printf("User roles: %s%n", userRoles);

        return permissions.get(resource)
            .map(perm -> {
                System.err.printf("Found permissions for resource '%s': %s%n", resource, perm);
                return !userRoles.intersect(perm).isEmpty();
            })
            .getOrElse(false);
    }
}
