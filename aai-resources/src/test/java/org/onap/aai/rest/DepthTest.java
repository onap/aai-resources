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

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.onap.aai.PayloadUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class DepthTest extends AbstractSpringRestTest {

    @Test
    public void testOldVersionReturnAllChildrenAndNewVersionReturnDepthZero() throws IOException {

        String id = "customer-987654321-91";
        String endpoint = "/aai/v9/business/customers/customer/"+ id;

        String body = PayloadUtil.getResourcePayload("customer.json");

        httpEntity = new HttpEntity(body, headers);

        ResponseEntity responseEntity;
        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.PUT, httpEntity, String.class);
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.CREATED));

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        String responseBody = responseEntity.getBody().toString();
        assertThat(responseBody, not(containsString("service-instance-id")));
        endpoint = "/aai/v8/business/customers/customer/"+ id;

        responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        responseBody = responseEntity.getBody().toString();
        assertThat(responseBody, containsString("service-instance-id"));
    }
}
