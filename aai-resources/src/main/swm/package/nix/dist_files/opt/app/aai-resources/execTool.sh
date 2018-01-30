#!/bin/bash


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
