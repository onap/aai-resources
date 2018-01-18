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

#
# dynamicPayloadGenerator.sh  -- This tool is used for Tenant-Isolation project
#       It is used to load a snapshot into memory and generate payloads for any input nodes
#       
#
# Parameters:
#
#  -d (required) name of the fully qualified Datasnapshot file that you need to load
#  -s (optional) true or false to enable or disable schema, By default it is true for production, 
# 	     you can change to false if the snapshot has duplicates
#  -c (optional) config file to use for loading snapshot into memory.
#  -o (required) output file to store the data files
#  -f (optional) PAYLOAD or DMAAP-MR
#  -n (optional) input file for the script 
#  
#  
#  For example (there are many valid ways to use it):
#  
#  dynamicPayloadGenerator.sh -d '/opt/app/snapshots/snaphot.graphSON' -o '/opt/app/aai-resources/bundleconfig/etc/scriptdata/addmanualdata/tenant_isolation/' 
#              
#  or
#  dynamicPayloadGenerator.sh -d '/opt/app/snapshots/snaphot.graphSON' -s false -c '/opt/app/aai-resources/bundleconfig/etc/appprops/dynamic.properties' 
#					-o '/opt/app/aai-resources/bundleconfig/etc/scriptdata/addmanualdata/tenant_isolation/' -f PAYLOAD -n '/opt/app/aai-resources/bundleconfig/etc/scriptdata/nodes.json'
# 


echo
echo `date` "   Starting $0"

display_usage() {
        cat <<EOF
        Usage: $0 [options]

        1. Usage: dynamicPayloadGenerator -d <graphsonPath> -o  <output-path>
        2. This script has  2 arguments that are required.
           a.	-d (required) Name of the fully qualified Datasnapshot file that you need to load
           b.	-o (required) output file to store the data files
        3. Optional Parameters:
		   a.   -s (optional) true or false to enable or disable schema, By default it is true for production, 
		   b.	-c (optional) config file to use for loading snapshot into memory.
		   c.	-f (optional) PAYLOAD or DMAAP-MR
		   d.	-n (optional) input file for the script
		4. For example (there are many valid ways to use it):
			dynamicPayloadGenerator.sh -d '/opt/app/snapshots/snaphot.graphSON' -o '/opt/app/aai-resources/bundleconfig/etc/scriptdata/addmanualdata/tenant_isolation/' 
				
			dynamicPayloadGenerator.sh -d '/opt/app/snapshots/snaphot.graphSON' -s false -c '/opt/app/aai-resources/bundleconfig/etc/appprops/dynamic.properties' 
					-o '/opt/app/aai-resources/bundleconfig/etc/scriptdata/addmanualdata/tenant_isolation/' -f PAYLOAD -n '/opt/app/aai-resources/bundleconfig/etc/scriptdata/nodes.json'
   
EOF
}
if [ $# -eq 0 ]; then
        display_usage
        exit 1
fi



. /etc/profile.d/aai.sh
PROJECT_HOME=/opt/app/aai-resources

for JAR in `ls $PROJECT_HOME/extJars/*.jar`
do
      CLASSPATH=$CLASSPATH:$JAR
done

for JAR in `ls $PROJECT_HOME/lib/*.jar`
do
     CLASSPATH=$CLASSPATH:$JAR
done


$JAVA_HOME/bin/java -classpath $CLASSPATH -Dhttps.protocols=TLSv1.1,TLSv1.2 -DBUNDLECONFIG_DIR=bundleconfig -DAJSC_HOME=$PROJECT_HOME \
 -Daai.home=$PROJECT_HOME -Dlogback.configurationFile=$PROJECT_HOME/bundleconfig/etc/appprops/dynamicPayloadGenerator-logback.xml  -Xmx9000m -Xms9000m \
 org.onap.aai.dbgen.DynamicPayloadGenerator "$@"

 
echo `date` "   Done $0"

exit 0
