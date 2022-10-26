/**
 * ============LICENSE_START==================================================
 * org.onap.aai
 * ===========================================================================
 * Copyright © 2017-2020 AT&T Intellectual Property. All rights reserved.
 * ===========================================================================
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
 * ============LICENSE_END====================================================
 */

package org.onap.aai.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.onap.aai.PayloadUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class CloudRegionTest extends AbstractSpringRestTest {

    @Test
    public void testCloudRegionWithChildAndDoGetAndExpectChildProperties() throws IOException {

        String endpoint = "/aai/v11/cloud-infrastructure/cloud-regions/cloud-region/testOwner/testRegionOne";

        ResponseEntity responseEntity = null;

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());

        String payload = PayloadUtil.getResourcePayload("cloud-region.json");
        httpEntity = new HttpEntity(payload, headers);
        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.PUT, httpEntity, String.class);
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        responseEntity =
                restTemplate.exchange(baseUrl + endpoint + "/tenants", HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertThat(responseEntity.getBody().toString(), containsString("tenant-id"));
    }
}
