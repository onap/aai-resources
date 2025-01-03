/*
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.aai.rest.util;

import java.util.ArrayList;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.onap.aai.exceptions.AAIException;
import org.onap.aai.logging.ErrorLogHelper;
import org.onap.aai.restcore.RESTAPI;
import org.onap.aai.util.GraphChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * The Class EchoResponse.
 */
@Component
@Path("/util")
@RequiredArgsConstructor
public class EchoResponse extends RESTAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(EchoResponse.class);

    private static final String UP_RESPONSE="{\"status\":\"UP\",\"groups\":[\"liveness\",\"readiness\"]}";

    private final GraphChecker graphChecker;

    /**
     * Simple health-check API that echos back the X-FromAppId and X-TransactionId to clients.
     * If there is a query string, a transaction gets logged into hbase, proving the application is connected to the
     * data store.
     * If there is no query string, no transacction logging is done to hbase.
     *
     * @param headers the headers
     * @param req the req
     * @param myAction if exists will cause transaction to be logged to hbase
     * @return the response
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Path("/echo")
    public Response echoResult(@Context HttpHeaders headers, @Context HttpServletRequest req) {

        String fromAppId;
        String transId;
        try {
            fromAppId = getFromAppId(headers);
            transId = getTransId(headers);
        } catch (AAIException aaiException) {
            ArrayList<String> templateVars = new ArrayList<>();
            templateVars.add("PUT uebProvider");
            templateVars.add("addTopic");
            LOGGER.error("Error while getting ids", aaiException);
            return generateFailureResponse(headers, templateVars, aaiException);
        }

        ArrayList<String> templateVars = new ArrayList<>();
        templateVars.add(fromAppId);
        templateVars.add(transId);

        try {
            validateDBStatus();
            return generateSuccessResponse();

        } catch (AAIException aaiException) {
            LOGGER.error("Error while processing echo request ", aaiException);
            return generateFailureResponse(headers, templateVars, aaiException);
        } catch (Exception exception) {
            AAIException aaiException = new AAIException("AAI_4000", exception);
            LOGGER.error("Error while generating echo response", exception);
            return generateFailureResponse(headers, templateVars, aaiException);
        }
    }

    /**
     * Validates if Janus Graph can process request using AAIGraphChecker.
     *
     * @param action expected input values 'checkDB' 'checkDBNow'
     * @throws AAIException exception thrown if DB is not available
     */
    private void validateDBStatus() throws AAIException {
        boolean dbAvailable = graphChecker.isAaiGraphDbAvailable();
        if (!dbAvailable) {
            throw new AAIException("AAI_5105", "Error establishing a database connection");
        }

    }

    private Response generateSuccessResponse() {
        return Response.status(Status.OK)
                .entity(UP_RESPONSE)
                .build();
    }

    private Response generateFailureResponse(HttpHeaders headers, ArrayList<String> templateVariables,
            AAIException aaiException) {
        return Response.status(aaiException.getErrorObject().getHTTPResponseCode()).entity(ErrorLogHelper
                .getRESTAPIErrorResponseWithLogging(headers.getAcceptableMediaTypes(), aaiException, templateVariables))
                .build();
    }

}
