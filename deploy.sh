#!/bin/bash
#
# ============LICENSE_START=======================================================
# org.onap.aai
# ================================================================================
# Copyright © 2017 AT&T Intellectual Property. All rights reserved.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END=========================================================
#
# ECOMP is a trademark and service mark of AT&T Intellectual Property.
#


if [ -f "/opt/docker/docker-compose" ];
then
    DOCKER_COMPOSE_CMD="/opt/docker/docker-compose"
else
    DOCKER_COMPOSE_CMD="docker-compose"
fi

export DOCKER_REGISTRY="${DOCKER_REGISTRY:-localhost:5000}";
export HBASE_IMAGE="${HBASE_IMAGE:-wc9368/aai-hbase-1.2.3}";
export GREMLIN_SERVER_IMAGE="${GREMLIN_SERVER_IMAGE:-gremlin-server}";
export AAI_HAPROXY_IMAGE="${AAI_HAPROXY_IMAGE:-aai-haproxy}";

function wait_for_container() {

    CONTAINER_NAME="$1";
    START_TEXT="$2";

    TIMEOUT=120

    # wait for the real startup
    AMOUNT_STARTUP=$(docker logs ${CONTAINER_NAME} 2>&1 | grep "$START_TEXT" | wc -l)
    while [[ ${AMOUNT_STARTUP} -ne 1 ]];
    do
        echo "Waiting for '$CONTAINER_NAME' deployment to finish ..."
        AMOUNT_STARTUP=$(docker logs ${CONTAINER_NAME} 2>&1 | grep "$START_TEXT" | wc -l)
        if [ "$TIMEOUT" = "0" ];
        then
            echo "ERROR: $CONTAINER_NAME deployment failed."
            exit 1
        fi
        let TIMEOUT-=1
        sleep 1
    done
}

# cleanup
$DOCKER_COMPOSE_CMD stop
$DOCKER_COMPOSE_CMD rm -f -v

# deploy
$DOCKER_COMPOSE_CMD up -d aai_haproxy

HBASE_CONTAINER_NAME=$($DOCKER_COMPOSE_CMD up -d hbase 2>&1 | awk '{ print $2; }');
wait_for_container $HBASE_CONTAINER_NAME '^starting regionserver';

GREMLIN_CONTAINER_NAME=$($DOCKER_COMPOSE_CMD up -d gremlin 2>&1 | awk '{ print $2; }');
wait_for_container $GREMLIN_CONTAINER_NAME 'Channel started at port 8182';

RESOURCES_CONTAINER_NAME=$($DOCKER_COMPOSE_CMD up -d aai-resources 2>&1 | awk '{ print $2; }');
wait_for_container $RESOURCES_CONTAINER_NAME '0.0.0.0:8447';

$DOCKER_COMPOSE_CMD up -d aai-graph-query

