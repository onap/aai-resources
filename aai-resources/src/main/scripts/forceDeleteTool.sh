#!/bin/ksh

###
# ============LICENSE_START=======================================================
# org.openecomp.aai
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
# forceDeleteTool.sh  -- This tool is used to delete nodes that cannot be deleted using
#        the normal REST API because of internal DB problems.  For example, Phantom nodes
#        and duplicate nodes cause errors to happen in "normal" REST API codes and must
#        be deleted using this tool.  
#        Since it is not using the "normal" REST logic, it is also not invoking the "normal" 
#        edge rules that we use to cascade deletes to "child" nodes.  So - this tool can be dangerous.  
#        Ie. if you accidently delete a parent node (like a cloud-region) that has many dependent 
#        child nodes, there will be no way to get to any of those child-nodes after the cloud-region
#        has been deleted.  
#        There are several environment variables defined in aaiconfig.properties to help minimize errors like that.
#                aai.forceDel.protected.nt.list=cloud-region
#                aai.forceDel.protected.edge.count=10
#                aai.forceDel.protected.descendant.count=10
#
# Parameters:
#
#  -action (required) valid values: COLLECT_DATA or DELETE_NODE or DELETE_EDGE
#  -userId (required) must be followed by a userid
#  -params4Collect (followed by a string to tell what properties/values to use 
#  		as part of a COLLECT_DATA request.  Must be in the format
#  		of ?propertName|propValue? use commas to separate if there
#  		are more than one name/value being passed.
#  -vertexId - required for a DELETE_NODE request
#  -edgeId - required for a DELETE_EDGE request
#  -overRideProtection --- WARNING ? This over-rides the protections we introduced!
#       It will let you override a protected vertex or vertex that has more
#       than the allowed number of edges or descendants.
#  -DISPLAY_ALL_VIDS (optional) - in the rare case when you want to see the 
#       vertex-ids (vids) of all the CONNECTED vertices, you can use this.  By 
#       default, we do not show them.
#  
#  
#  For example:
#  
#  forceDeleteTool.sh -action COLLECT_DATA -userId am8383 -params4Collect "tenant-id|junk tenant01 ID 0224"
#  
#  forceDeleteTool.sh -action COLLECT_DATA -userId am8383 -params4Collect "cloud-owner|junkTesterCloudOwner 0224,cloud-region-id|junkTesterCloud REgion ID 0224"
#
#  forceDeleteTool.sh -action DELETE_NODE -userId am8383 -vertexId 1234567
#
#  forceDeleteTool.sh -action DELETE_EDGE -userId am8383 -edgeId 9876543
#
# 


echo
echo `date` "   Starting $0"

echo " NOTE - if you are deleting data, please run the dataSnapshot.sh script first or "
echo "     at least make a note the details of the node that you are deleting. "

userid=$( id | cut -f2 -d"(" | cut -f1 -d")" )
if [ "${userid}" != "aaiadmin" ]; then
    echo "You must be aaiadmin to run $0. The id used $userid."
    exit 1
fi 

. /etc/profile.d/aai.sh

for JAR in `ls $PROJECT_HOME/extJars/*.jar`
do
      CLASSPATH=$CLASSPATH:$JAR
done

for JAR in `ls $PROJECT_HOME/lib/*.jar`
do
     CLASSPATH=$CLASSPATH:$JAR
done


$JAVA_HOME/bin/java -classpath $CLASSPATH -Dhttps.protocols=TLSv1.1,TLSv1.2 -DAJSC_HOME=$PROJECT_HOME  -Daai.home=$PROJECT_HOME \
 org.openecomp.aai.dbgen.ForceDeleteTool "$@"

 
echo `date` "   Done $0"

exit 0
