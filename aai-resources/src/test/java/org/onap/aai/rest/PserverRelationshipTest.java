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

package org.onap.aai.rest;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.onap.aai.PayloadUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class PserverRelationshipTest extends AbstractSpringRestTest {

    @Test
    public void testGetRelationshipThrowUnrecognizedAAIObjectException() {

        String endpoint = "/aai/v12/cloud-infrastructure/pservers/pserver/test/relationship-list/relationship";

        ResponseEntity<String> responseEntity = null;

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        String body = responseEntity.getBody().toString();

        Set<HttpMethod> httpMethodSet = new HashSet<>();

        httpMethodSet.add(HttpMethod.PUT);
        httpMethodSet.add(HttpMethod.DELETE);
        httpMethodSet.add(HttpMethod.OPTIONS);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertThat(body, containsString("Unrecognized AAI function"));
        assertThat(responseEntity.getHeaders().getAllow(), containsInAnyOrder(httpMethodSet.toArray()));
    }

    // @Test
    public void testPutPserverAndCloudRegionAndReturnEdgesWithLabel() throws Exception {

        String hostname = "test-pserver1";
        String endpoint = "/aai/v12/cloud-infrastructure/pservers/pserver/" + hostname;

        ResponseEntity<String> responseEntity = null;

        restTemplate.exchange(baseUrl + endpoint, HttpMethod.PUT, httpEntity, String.class);

        String cloudRegionId = "test-region-1";
        String cloudOwnerId = "test-owner-1";
        endpoint = "/aai/v12/cloud-infrastructure/cloud-regions/cloud-region/" + cloudOwnerId + "/" + cloudRegionId;

        Map<String, String> map = new HashMap<>();
        map.put("hostname", hostname);
        map.put("cloud-region-id", cloudRegionId);
        map.put("cloud-owner", cloudOwnerId);

        String payload = PayloadUtil.getTemplatePayload("pserver-to-cloud-region.json", map);
        httpEntity = new HttpEntity<String>(payload, headers);
        restTemplate.exchange(baseUrl + endpoint, HttpMethod.PUT, httpEntity, String.class);

        httpEntity = new HttpEntity<String>(headers);
        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertThat(responseEntity.getBody().toString(), containsString("relationship-label"));

        endpoint = "/aai/v11/cloud-infrastructure/cloud-regions/cloud-region/" + cloudOwnerId + "/" + cloudRegionId;

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertThat(responseEntity.getBody().toString(), not(containsString("relationship-label")));
    }
}
