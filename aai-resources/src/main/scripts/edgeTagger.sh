#!/bin/ksh
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

#
# This script is used to synch-up the data in the database with the "edge-tags" definitions found in the  DbEdgeRules.java file.  
# It is not needed during normal operation, but if a new tag (really a property) is defined for an edge or if an
#    existing tag is changed, then this script should be run to migrate existing data so that it matches the new
#    definition.   Note: it is only dealing with the "tags" defined after position 2.  For example, for our existing
#    rules, we have have "isParent" defined in position 2, and then other tags in positions 3, 4 and 5 as 
#    mapped in DbEdgeRules.EdgeInfoMap:
#
#  public static final Map<Integer, String> EdgeInfoMap; 
#    static { 
#        EdgeInfoMap = new HashMap<Integer, String>(); 
#        EdgeInfoMap.put(0, "edgeLabel");
#        EdgeInfoMap.put(1, "direction");
#        EdgeInfoMap.put(2, "isParent" );
#        EdgeInfoMap.put(3, "usesResource" );
#        EdgeInfoMap.put(4, "hasDelTarget" );
#        EdgeInfoMap.put(5, "SVC-INFRA" );
#    }
#  
#   -- The map above is used to interpret the data in the DbEdgeRules.EdgeRules map:
#
#   public static final Multimap<String, String> EdgeRules =
#       new ImmutableSetMultimap.Builder<String, String>() 
#       .putAll("availability-zone|complex","groupsResourcesIn,OUT,false,false,false,reverse")
#       .putAll("availability-zone|service-capability","supportsServiceCapability,OUT,false,false,false,false")
#       .putAll("complex|ctag-pool","hasCtagPool,OUT,true,false,false,false")
#       .putAll("complex|l3-network","usesL3Network,OUT,false,false,false,true")
#       etc...
#
#   -- Valid values for the "tags" can be "true", "false" or "reverse".  Read the T-space
#     write-up for a detailed explanation of this... 
#
#
# To use this script, You can either pass the parameter, "all" to update all the edge rules, or
#   you can pass the KEY to a single edge rule that you would like to update.   NOTE - the 
#   key is that first part of each edge rule that looks like, "nodeTypeA|nodeTypeB".
#
# Ie.   ./edgeTagger.sh "all"
#    or ./edgeTagger.sh "complex|ctag-pool"
#

COMMON_ENV_PATH=$( cd "$(dirname "$0")" ; pwd -P )
. ${COMMON_ENV_PATH}/common_functions.sh

start_date;

echo " NOTE - if you are deleting data, please run the dataSnapshot.sh script first or "
echo "     at least make a note the details of the node that you are deleting. "

check_user;
source_profile;

execute_spring_jar org.onap.aai.dbgen.UpdateEdgeTags "" "$@"

PROCESS_STATUS=$?;

if [ ${PROCESS_STATUS} -ne 0 ]; then
    echo "Problem executing UpdateEdgeTags";
    exit 1;
fi;

end_date;
exit 0
