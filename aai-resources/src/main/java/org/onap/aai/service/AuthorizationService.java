/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2025 Deutsche Telekom. All rights reserved.
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

package org.onap.aai.service;

import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

import org.onap.aai.config.AuthProperties;
import org.springframework.stereotype.Service;

/**
 * Should be removed once Spring Security-based auth works
 */
@Service
public class AuthorizationService {

  // Saved in this format for best performance
  private final Set<String> authorizedHeaders;

  public AuthorizationService(AuthProperties authProperties) {
    authorizedHeaders = getAuthorizedHeaders(authProperties);
    String s = "";
  }

  public boolean isAuthorized(String authHeaderValue) {
    return authorizedHeaders.contains(authHeaderValue);
  }

  /**
   * Returns valid Bearer auth headers for all users.
   * @param authProperties
   * @param encoder
   * @return
   */
  private Set<String> getAuthorizedHeaders(AuthProperties authProperties) {
    Base64.Encoder encoder = Base64.getEncoder();
    return authProperties.getUsers().stream()
      .map(user -> user.getUsername() + ":" + user.getPassword())
      .map(usernamePasswordPair -> encoder.encode(usernamePasswordPair.getBytes()))
      .map(String::new)
      .map(encodedPair -> "Basic " + encodedPair)
      .collect(Collectors.toSet());
  }


}
