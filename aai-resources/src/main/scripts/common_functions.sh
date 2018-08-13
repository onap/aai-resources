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


# Common functions that can be used throughout multiple scripts
# In order to call these functions, this file needs to be sourced

# Checks if the user that is currently running is aaiadmin
check_user(){

    userid=$( id | cut -f2 -d"(" | cut -f1 -d")" )

    if [ "${userid}" != "aaiadmin" ]; then
        echo "You must be aaiadmin to run $0. The id used $userid."
        exit 1
    fi
}

# Sources the profile and sets the project home
source_profile(){
    . /etc/profile.d/aai.sh
    PROJECT_HOME=/opt/app/aai-resources
}

# Runs the spring boot jar based on which main class
# to execute and which logback file to use for that class
execute_spring_jar(){

    className=$1;
    logbackFile=$2;

    shift 2;

    EXECUTABLE_JAR=$(ls ${PROJECT_HOME}/lib/*.jar);

    JAVA_OPTS="${JAVA_PRE_OPTS} -DAJSC_HOME=$PROJECT_HOME";
    JAVA_OPTS="$JAVA_OPTS -DBUNDLECONFIG_DIR=resources";
    JAVA_OPTS="$JAVA_OPTS -Daai.home=$PROJECT_HOME ";
    JAVA_OPTS="$JAVA_OPTS -Dhttps.protocols=TLSv1.1,TLSv1.2";
    JAVA_OPTS="$JAVA_OPTS -Dloader.main=${className}";
    JAVA_OPTS="$JAVA_OPTS -Dloader.path=${PROJECT_HOME}/resources";
    JAVA_OPTS="$JAVA_OPTS -Dlogback.configurationFile=${logbackFile}";

    export SOURCE_NAME=$(grep '^schema.source.name=' ${PROJECT_HOME}/resources/application.properties | cut -d"=" -f2-);
    # Needed for the schema ingest library beans
    eval $(grep '^schema\.' ${PROJECT_HOME}/resources/application.properties | \
     sed 's/^\(.*\)$/JAVA_OPTS="$JAVA_OPTS -D\1"/g' | \
     sed 's/${server.local.startpath}/${PROJECT_HOME}\/resources/g'| \
     sed 's/${schema.source.name}/'${SOURCE_NAME}'/g'\
    )

    JAVA_OPTS="${JAVA_OPTS} ${JAVA_POST_OPTS}";

    ${JAVA_HOME}/bin/java ${JVM_OPTS} ${JAVA_OPTS} -jar ${EXECUTABLE_JAR} "$@"
}

# Prints the start date and the script that the user called
start_date(){
    echo
    echo `date` "   Starting $0"
}

# Prints the end date and the script that the user called
end_date(){
    echo
    echo `date` "   Done $0"
}

# Inserts GEN_DB_WITH_NO_SCHEMA as a paranmter if it isn't there already
force_GEN_DB_WITH_NO_SCHEMA () {
  for p in "$@"
    do
    if [ "$p" == "GEN_DB_WITH_NO_SCHEMA" ]
    then
      echo "$@"
      return
    fi
    done
    echo "GEN_DB_WITH_NO_SCHEMA $@"
    return
}

