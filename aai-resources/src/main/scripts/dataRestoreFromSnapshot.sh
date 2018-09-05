#!/bin/ksh
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

#
# This script uses the dataSnapshot and SchemaGenerator (via GenTester) java classes to restore 
# data to a database by doing three things: 
#   1) clear out whatever data and schema are currently in the db 
#   2) rebuild the schema (using the SchemaGenerator)
#   3) reload data from the passed-in datafile (which must found in the dataSnapShots directory and
#      contain an xml view of the db data).
#

COMMON_ENV_PATH=$( cd "$(dirname "$0")" ; pwd -P )
. ${COMMON_ENV_PATH}/common_functions.sh

start_date;
check_user;

if [ "$#" -lt 1 ]; then
    echo "Illegal number of parameters"
    echo "usage: $0 previous_snapshot_filename"
    exit 1
fi

source_profile;
export PRE_JAVA_OPTS=${PRE_JAVA_OPTS:--Xms6g -Xmx8g};

#### Step 1) clear out the database
execute_spring_jar org.onap.aai.dbgen.DataSnapshot ${PROJECT_HOME}/resources/etc/appprops/dataSnapshot-logback.xml "CLEAR_ENTIRE_DATABASE" "$1"
if [ "$?" -ne "0" ]; then
    echo "Problem clearing out database."
    exit 1
fi
 
#### Step 2) rebuild the db-schema
execute_spring_jar org.onap.aai.dbgen.GenTester ${PROJECT_HOME}/resources/etc/appprops/createDBSchema-logback.xml "GEN_DB_WITH_NO_DEFAULT_CR"
if [ "$?" -ne "0" ]; then
    echo "Problem rebuilding the schema (SchemaGenerator)."
    exit 1
fi

#### Step 3) reload the data from a snapshot file

execute_spring_jar org.onap.aai.dbgen.DataSnapshot ${PROJECT_HOME}/resources/etc/appprops/dataSnapshot-logback.xml "RELOAD_DATA" "$1"
if [ "$?" -ne "0" ]; then
    echo "Problem reloading data into the database."
    end_date;
    exit 1
fi
 
end_date;
exit 0
