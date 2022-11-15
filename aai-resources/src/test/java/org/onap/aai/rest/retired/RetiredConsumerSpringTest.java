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

package org.onap.aai.rest.retired;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.onap.aai.rest.AbstractSpringRestTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RetiredConsumerSpringTest extends AbstractSpringRestTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetiredConsumerSpringTest.class);

    private Map<String, HttpStatus> httpStatusMap;

    @Test
    public void testOldVersionsEndpointReturnRetired() {
        setupOldVersions();
        executeRestCalls();
    }

    @Test
    public void testOldModelsRetired() {
        setupModelsRetired();
        executeRestCalls();
    }

    @Test
    public void testOldNamedQueriesRetired() {
        setupNamedQueriesRetired();
        executeRestCalls();
    }

    @Test
    public void testEdgeTagQueryRetired() {
        setupEdgeTagQueriesRetired();
        executeRestCalls();
    }

    @Test
    public void testSDNZoneQueryRetired() {
        setupSDNZoneQueryRetired();
        executeRestCalls();
    }

    private void setupSDNZoneQueryRetired() {
        httpStatusMap = new HashMap<>();

        httpStatusMap.put("/aai/v2/search/sdn-zone-query", HttpStatus.GONE);
        httpStatusMap.put("/aai/v3/search/sdn-zone-query", HttpStatus.GONE);
        httpStatusMap.put("/aai/v4/search/sdn-zone-query", HttpStatus.GONE);
        httpStatusMap.put("/aai/v5/search/sdn-zone-query", HttpStatus.GONE);
        httpStatusMap.put("/aai/v6/search/sdn-zone-query", HttpStatus.GONE);
        httpStatusMap.put("/aai/v7/search/sdn-zone-query", HttpStatus.GONE);
        httpStatusMap.put("/aai/v8/search/sdn-zone-query", HttpStatus.GONE);
        httpStatusMap.put("/aai/v9/search/sdn-zone-query", HttpStatus.GONE);
        httpStatusMap.put("/aai/v10/search/sdn-zone-query", HttpStatus.GONE);
        httpStatusMap.put("/aai/v11/search/sdn-zone-query", HttpStatus.GONE);
        httpStatusMap.put("/aai/v12/search/sdn-zone-query", HttpStatus.GONE);
        httpStatusMap.put("/aai/v12/search/sdn-zone-query/fjaisdofjasdjf", HttpStatus.GONE);
    }

    private void setupEdgeTagQueriesRetired() {

        httpStatusMap = new HashMap<>();

        httpStatusMap.put("/aai/v2/search/edge-tag-query", HttpStatus.GONE);
        httpStatusMap.put("/aai/v2/search/edge-tag-query/", HttpStatus.GONE);
        httpStatusMap.put("/aai/v2/search/edge-tag-query/something", HttpStatus.GONE);
        httpStatusMap.put("/aai/v3/search/edge-tag-query/aifjsodifjiasjdfioadjsiofjaiosdj", HttpStatus.GONE);
        httpStatusMap.put("/aai/v4/search/edge-tag-query/aifjsodifjiasjdfioadjsiofjaiosdj", HttpStatus.GONE);
        httpStatusMap.put("/aai/v5/search/edge-tag-query/aifjsodifjiasjdfioadjsiofjaiosdj", HttpStatus.GONE);
        httpStatusMap.put("/aai/v6/search/edge-tag-query/aifjsodifjiasjdfioadjsiofjaiosdj", HttpStatus.GONE);
        httpStatusMap.put("/aai/v7/search/edge-tag-query/aifjsodifjiasjdfioadjsiofjaiosdj", HttpStatus.GONE);
        httpStatusMap.put("/aai/v8/search/edge-tag-query/aifjsodifjiasjdfioadjsiofjaiosdj", HttpStatus.GONE);
        httpStatusMap.put("/aai/v9/search/edge-tag-query/aifjsodifjiasjdfioadjsiofjaiosdj", HttpStatus.GONE);
        httpStatusMap.put("/aai/v10/search/edge-tag-query/aifjsodifjiasjdfioadjsiofjaiosdj", HttpStatus.GONE);
        httpStatusMap.put("/aai/v11/search/edge-tag-query/aifjsodifjiasjdfioadjsiofjaiosdj", HttpStatus.GONE);
        httpStatusMap.put("/aai/v12/search/edge-tag-query/aifjsodifjiasjdfioadjsiofjaiosdj", HttpStatus.GONE);
        httpStatusMap.put("/aai/v13/search/edge-tag-query/aifjsodifjiasjdfioadjsiofjaiosdj", HttpStatus.GONE);
        httpStatusMap.put("/aai/v13/search/edge-tag-query", HttpStatus.GONE);
    }

    private void setupNamedQueriesRetired() {
        httpStatusMap = new HashMap<>();

        httpStatusMap.put("/aai/v13/cloud-infrastructure/pservers/pserver/samomaisdjfajsfoas", HttpStatus.NOT_FOUND);

        httpStatusMap.put("/aai/v8/service-design-and-creation/named-queries/named-query/samomaisdjfajsfoas",
                HttpStatus.GONE);
    }

    protected void executeRestCalls() {
        httpStatusMap.forEach((url, status) -> {
            ResponseEntity<String> responseEntity;
            responseEntity = restTemplate.exchange(baseUrl + url, HttpMethod.GET, httpEntity, String.class);
            LOGGER.debug("For url {} expected status {} actual status {} and body {}", url, status,
                    responseEntity.getStatusCodeValue(), responseEntity.getBody());
            assertEquals(status, responseEntity.getStatusCode());
        });
    }

    private void setupOldVersions() {
        httpStatusMap = new HashMap<>();

        httpStatusMap.put("/aai/v2/cloud-infrastructure/pservers/pserver/test-pserver1", HttpStatus.GONE);
        httpStatusMap.put("/aai/v3/cloud-infrastructure/pservers/pserver/test-pserver1", HttpStatus.GONE);
        httpStatusMap.put("/aai/v4/cloud-infrastructure/pservers/pserver/test-pserver1", HttpStatus.GONE);
        httpStatusMap.put("/aai/v5/cloud-infrastructure/pservers/pserver/test-pserver1", HttpStatus.GONE);
        httpStatusMap.put("/aai/v6/cloud-infrastructure/pservers/pserver/test-pserver1", HttpStatus.GONE);
        httpStatusMap.put("/aai/v7/cloud-infrastructure/pservers/pserver/test-pserver1", HttpStatus.GONE);
        httpStatusMap.put("/aai/v8/cloud-infrastructure/pservers/pserver/test-pserver1", HttpStatus.GONE);
        httpStatusMap.put("/aai/v9/cloud-infrastructure/pservers/pserver/test-pserver1", HttpStatus.GONE);
    }

    private void setupModelsRetired() {

        httpStatusMap = new HashMap<>();

        httpStatusMap.put("/aai/v8/cloud-infrastructure/pservers/pserver/samomaisdjfajsfoas", HttpStatus.GONE);

        httpStatusMap.put("/aai/v8/service-design-and-creation/models/model/samomaisdjfajsfoas", HttpStatus.GONE);
    }
}
