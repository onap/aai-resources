#!/bin/ksh

###
# ============LICENSE_START=======================================================
# org.onap.aai
# ================================================================================
# Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END=========================================================
###

COMMON_ENV_PATH=$( cd "$(dirname "$0")" ; pwd -P )
. ${COMMON_ENV_PATH}/common_functions.sh

start_date;
check_user;
source_profile;

prop_file=$PROJECT_HOME/resources/application.properties

CERTPATH=${PROJECT_HOME}/resources/etc/auth/
KEYNAME=aaiClientPrivateKey.pem
CERTNAME=aaiClientPublicCert.pem
CERTIFICATE_FILE=${CERTPATH}aai-client-cert.p12

CERTMAN_PATH=`grep ^server.certs.location $prop_file |cut -d'=' -f2 |tr -d "\015"`
if [ -z $CERTMAN_PATH ]; then
    echo "Property [server.certs.location] not found in file $prop_file, continuing with default"
    pw=$(execute_spring_jar org.onap.aai.util.AAIConfigCommandLinePropGetter "" "aai.keystore.passwd" 2> /dev/null | tail -1)
else
    # Assume AAF certificate container use
    pw=$(< ${CERTMAN_PATH}/.password)
    CERTIFICATE_FILE=${CERTMAN_PATH}/certificate.pkcs12
fi

openssl pkcs12 -in ${CERTIFICATE_FILE} -out $CERTPATH$CERTNAME -nokeys -nodes -passin pass:$pw
openssl pkcs12 -in ${CERTIFICATE_FILE} -nocerts -out $CERTPATH$KEYNAME -nodes -passin pass:$pw
end_date;
exit 0
