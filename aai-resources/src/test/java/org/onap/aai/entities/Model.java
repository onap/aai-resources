/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2024 Deutsche Telekom AG Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.aai.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Describes the model returned by aai-resources
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Model {

  @JsonProperty("model-invariant-id")
  private String modelInvariantId;

  @JsonProperty("model-role")
  private String modelRole;

  @JsonProperty("data-owner")
  private String dataOwner;

  @JsonProperty("data-source")
  private String dataSource;

  @JsonProperty("data-source-version")
  private String dataSourceVersion;

  @JsonProperty("resource-version")
  private String resourceVersion;

  @JsonProperty("model-vers")
  List<ModelVersion> modelVersions;
}
