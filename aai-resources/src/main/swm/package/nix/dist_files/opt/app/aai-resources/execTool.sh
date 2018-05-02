#!/bin/bash
#
# ============LICENSE_START=======================================================
# org.onap.aai
# ================================================================================
# Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
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



export WORKING_DIR="$( cd "$(dirname "$0")" ; pwd -P )/"

CONTAINER_NAME=$(docker ps | grep 'aai-resources' | awk '{ print $7; }');

SCRIPT_NAME=$1;

shift;

docker exec -u aaiadmin ${CONTAINER_NAME} ls /opt/app/aai-resources/scripts/${SCRIPT_NAME} && {
    docker exec -u aaiadmin ${CONTAINER_NAME} /opt/app/aai-resources/scripts/${SCRIPT_NAME} "$@"
    exit 0;
} || {
    echo "Unable to find the tool in the /opt/app/aai-resources/scripts";
    exit 1;
}
