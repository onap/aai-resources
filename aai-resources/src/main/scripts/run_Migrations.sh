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

echo
echo $(date) "   Starting $0"

userid=$( id | cut -f2 -d"(" | cut -f1 -d")" )
if [ "${userid}" != "aaiadmin" ]; then
    echo "You must be aaiadmin to run $0. The id used $userid."
    exit 1
fi

if [ -f "/etc/profile.d/aai.sh" ]; then
    source /etc/profile.d/aai.sh
else
    echo "File not found: /etc/profile.d/aai.sh";
    exit
fi

JAVA=$JAVA_HOME/bin/java
PROJECT_HOME=/opt/app/aai-resources

ARGS="-c ${PROJECT_HOME}/bundleconfig/etc/appprops/titan-realtime.properties $@"

for JAR in $(ls $PROJECT_HOME/extJars/*.jar)
do
   	CLASSPATH=$CLASSPATH:$JAR
done

UUID=$(uuidgen)

unzip -o $PROJECT_HOME/lib/ajsc-runner-5.0.0-RC16.0.5.jar -d /tmp/ajsc-war-$UUID/ > /dev/null
unzip -o /tmp/ajsc-war-$UUID/ajsc-war-5.0.0-RC16.0.5.war -d /tmp/ajsc-war-$UUID/ > /dev/null

for JAR in $(ls /tmp/ajsc-war-$UUID/WEB-INF/lib/*.jar)
do
	if [[ ! "$JAR" =~ .*logback-classic-.*.jar ]]; 
	then 
		CLASSPATH=$CLASSPATH:$JAR 
	fi
done

for JAR in $(ls /opt/app/swm/dme2/lib/*.jar)
do
    CLASSPATH=$CLASSPATH:$JAR
done

for JAR in $(ls $PROJECT_HOME/lib/*.jar)
do
     CLASSPATH=$CLASSPATH:$JAR
done

CLASSPATH=$CLASSPATH:${PROJECT_HOME}"/bundleconfig/etc/tmp-config/"

$JAVA -Dhttps.protocols=TLSv1.1,TLSv1.2 -DAJSC_HOME=$PROJECT_HOME  -Daai.home=$PROJECT_HOME -DBUNDLECONFIG_DIR="bundleconfig" -Dlogback.configurationFile=$PROJECT_HOME/bundleconfig/etc/appprops/migration-logback.xml -cp $CLASSPATH org.onap.aai.migration.MigrationController $ARGS

rm -r  /tmp/ajsc-war-$UUID/
