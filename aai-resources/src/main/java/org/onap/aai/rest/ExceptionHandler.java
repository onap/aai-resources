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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.sun.istack.SAXParseException2;

import java.util.ArrayList;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.onap.aai.exceptions.AAIException;
import org.onap.aai.logging.ErrorLogHelper;

/**
 * The Class ExceptionHandler.
 */
@Provider
public class ExceptionHandler implements ExceptionMapper<Exception> {

    private static final String AAI_4007 = "AAI_4007";

    @Context
    private HttpServletRequest request;

    @Context
    private HttpHeaders headers;

    /**
     * @{inheritDoc}
     */
    @Override
    public Response toResponse(Exception exception) {

        // the general case is that cxf will give us a WebApplicationException
        // with a linked exception
        if (exception instanceof WebApplicationException) {
            if (exception.getCause() instanceof SAXParseException2) {
                return buildInputParsingErrorResponse(exception, "UnmarshalException");
            } else {
                return ((WebApplicationException)exception).getResponse();
            }

        } else if (exception instanceof JsonParseException) {
            // jackson does it differently so we get the direct JsonParseException
            return buildInputParsingErrorResponse(exception);
        } else if (exception instanceof JsonMappingException) {
            return buildInputParsingErrorResponse(exception);
        } else {
            return defaultException(exception);
        }
    }

    private Response buildInputParsingErrorResponse(Exception exception) {
        ArrayList<String> templateVars = new ArrayList<>();
        templateVars.add(exception.getClass().getSimpleName());
        return buildInputParsingErrorResponse(exception, templateVars);
    }

    private Response buildInputParsingErrorResponse(Exception exception, String customExceptionName) {
        ArrayList<String> templateVars = new ArrayList<>();
        templateVars.add(customExceptionName);
        return buildInputParsingErrorResponse(exception, templateVars);
    }

    private Response buildInputParsingErrorResponse(Exception exception, ArrayList<String> templateVars) {
        AAIException ex = new AAIException(AAI_4007, exception);
        return Response
            .status(400).entity(ErrorLogHelper
                .getRESTAPIErrorResponse(headers.getAcceptableMediaTypes(), ex, templateVars))
            .build();
    }

    private Response defaultException(Exception exception) {
        ArrayList<String> templateVars = new ArrayList<>();
        templateVars.add(request.getMethod());
        templateVars.add("unknown");
        AAIException ex = new AAIException("AAI_4000", exception);
        // prefer xml, use json otherwise
        return headers.getAcceptableMediaTypes().stream()
            .filter(MediaType.APPLICATION_ATOM_XML_TYPE::isCompatible)
            .findAny()
            .map(xmlType -> 
                Response.status(400).type(MediaType.APPLICATION_XML_TYPE)
                    .entity(ErrorLogHelper.getRESTAPIErrorResponse(
                        headers.getAcceptableMediaTypes(), ex, templateVars))
                    .build())
            .orElseGet(() -> 
                Response.status(400).type(MediaType.APPLICATION_JSON_TYPE)
                .entity(ErrorLogHelper.getRESTAPIErrorResponse(
                    headers.getAcceptableMediaTypes(), ex, templateVars))
                .build());
    }

}
