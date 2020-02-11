package org.onap.aai.rest;

import org.junit.Test;
import org.onap.aai.PayloadUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;

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

        responseEntity = restTemplate.exchange(baseUrl + endpoint + "/tenants", HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertThat(responseEntity.getBody().toString(), containsString("tenant-id"));
    }
}
