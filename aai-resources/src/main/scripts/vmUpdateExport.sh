#!/bin/ksh
#
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
# vmUpdateExport.sh  -- This tool updates the files generated in A&AI
# The script takes no arguments

addTemplates() {
	jq -n --arg dsl "pserver('hostname','change-this-to-hostname') > p-interface > [ sriov-pf* ]" \
'{"dsl": "\($dsl)"}' > sriovpfquery-template.json
}

COMMON_ENV_PATH=$( cd "$(dirname "$0")" ; pwd -P )
. ${COMMON_ENV_PATH}/common_functions.sh



PROJECT_HOME=/opt/app/aai-resources
PROGNAME=$(basename $0)

CHECK_USER="aaiadmin"
userid=$( id | cut -f2 -d"(" | cut -f1 -d")" )
if [ "${userid}" != $CHECK_USER ]; then
    echo "You must be  $CHECK_USER to run $0. The id used $userid."
    exit 1
fi
VMEXPORTDIR=$PROJECT_HOME/resources/etc/scriptdata/addmanualdata/vm_export
cd $VMEXPORTDIR
addTemplates
URIS=`cat *vserver-validate-pserver*tx | sort -u`
for uri in $URIS
do
        hostname=`echo $uri | cut -d "/" -f 4`
	shortname=`echo $hostname | cut -d "." -f1`
	sed -i -e "s#relationship-value\": \"$hostname#relationship-value\" : \"$shortname#" *json
	sed -i -e "s#property-value\": \"$hostname#property-value\" : \"$shortname#" *json
	sed -i -e "s#pserver/$hostname#pserver/$shortname#" *json
	sed -i -e "s#pserver/$hostname#pserver/$shortname#" *tx
	cat sriovpfquery-template.json | sed -e "s/change-this-to-hostname/$shortname/" > sriovf.query.$shortname.json
	res=`$PROJECT_HOME/scripts/putTool.sh "/dsl?format=pathed" sriovf.query.$shortname.json -display | sed '1d;$d'`
	echo $res | jq '.results|.[]|."resource-link"' | sed -e 's/\"//g' | cut -d"/" -f4- >  sriovf.result.$shortname
done
txfiles=`ls *sriovvf-validate-sriov-pf-*tx`
for txfile in $txfiles
do
	uri=`cat $txfile`
        hostname=`echo $uri | cut -d "/" -f 4`
	pfPciId=`echo $uri | cut -d "/" -f 10`
	interfacename=`echo $uri | cut -d "/" -f 7`
	newuri=`grep $pfPciId sriovf.result.$hostname`
	#echo "old uri " + $uri
	#echo "new uri " + $newuri
	sed -i -e "s#$uri#$newuri#" *sriovvf-related-to-sriov-pf*.json
	sed -i -e "s#$uri#$newuri#" *sriovvf-validate-sriov-pf*.tx
done