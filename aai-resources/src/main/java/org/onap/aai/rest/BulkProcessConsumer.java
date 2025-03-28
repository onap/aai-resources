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

import io.micrometer.core.annotation.Timed;

import jakarta.ws.rs.Path;

import org.onap.aai.restcore.HttpMethod;

@Path("{version: v[1-9][0-9]*|latest}/bulkprocess")
@Timed
public class BulkProcessConsumer extends BulkConsumer {

    @Override
    protected boolean functionAllowed(HttpMethod method) {

        return method.equals(HttpMethod.PUT) || method.equals(HttpMethod.DELETE)
                || method.equals(HttpMethod.MERGE_PATCH);
    }

    @Override
    protected String getModule() {
        return "bulk process";
    }

    @Override
    protected boolean enableResourceVersion() {
        // TODO Auto-generated method stub
        return true;
    }

}
