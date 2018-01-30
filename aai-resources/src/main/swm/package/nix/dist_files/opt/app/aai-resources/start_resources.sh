#!/bin/bash

. /etc/profile.d/aai.sh
PROJECT_HOME=/opt/app/aai-resources

docker-compose -f ${PROJECT_HOME}/docker-compose.yaml up -d || exit 200
