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

# this script now requires a release parameter.
# the tool finds and sorts *.txt files within the
# resources/etc/scriptdata/addmanualdata/$release directory containing
# one resource to be added to the graph. The directory contains a second
# file with the same name, but the extension is .json. This json file
# is passed to the PutTool as the payload. The parameters passed to the
# PutTool will have 412 failures ignored. After calling the PutTool, the
# GetTool is called to include the object put into the graph.
# this script is run at every installation, logging the manual data  applied.

# Returns 0 if the specified string contains the specified substring,
# otherwise returns 1.
contains() {
    string="$1"
    substring="$2"
    if test "${string#*$substring}" != "$string"
    then
        return 0    # $substring is in $string
    else
        return 1    # $substring is not in $string
    fi
}

. /etc/profile.d/aai.sh
PROJECT_HOME=/opt/app/aai-resources

PROGNAME=$(basename $0)
OUTFILE=$PROJECT_HOME/logs/misc/${PROGNAME}.log.$(date +\%Y-\%m-\%d)
#OUTFILE=/c/temp/${PROGNAME}.log.$(date +\%Y-\%m-\%d)

TS=$(date "+%Y-%m-%d %H:%M:%S")

CHECK_USER="aaiadmin"
userid=$( id | cut -f2 -d"(" | cut -f1 -d")" )
if [ "${userid}" != $CHECK_USER ]; then
    echo "You must be  $CHECK_USER to run $0. The id used $userid."
    exit 1
fi


if [ "$#" -ne 1 ]; then
    echo "Release or tenant_isolation parameter is required, e.g. 1610, 1702, tenant_isolation, etc"
    echo "usage: $0 release"
    exit 1
fi

error_exit () {
        echo "${PROGNAME}: failed for ${1:-"Unknown error"} on cmd $2 in $3" 1>&2
        echo "${PROGNAME}: failed for ${1:-"Unknown error"} on cmd $2 in $3" >> $OUTFILE
#       exit ${2:-"1"}
}

rel="/"$1"/"
k=0

if [ "$1" = "tenant_isolation" ]
then
	CR_TEXT_PATH=`find $PROJECT_HOME/resources/etc/scriptdata/addmanualdata/tenant_isolation/cloud-region -name "*.txt" -print | sort -f`
    AZ_TEXT_PATH=`find $PROJECT_HOME/resources/etc/scriptdata/addmanualdata/tenant_isolation/availability-zone -name "*.txt" -print | sort -f`
    COMPLEX_TEXT_PATH=`find $PROJECT_HOME/resources/etc/scriptdata/addmanualdata/tenant_isolation/complex -name "*.txt" -print | sort -f`
    ZONE_TEXT_PATH=`find $PROJECT_HOME/resources/etc/scriptdata/addmanualdata/tenant_isolation/zone -name "*.txt" -print | sort -f`
    PSERVER_TEXT_PATH=`find $PROJECT_HOME/resources/etc/scriptdata/addmanualdata/tenant_isolation/pserver -name "*.txt" -print | sort -f`
    TEXT_PATH="${CR_TEXT_PATH} ${AZ_TEXT_PATH} ${COMPLEX_TEXT_PATH} ${ZONE_TEXT_PATH} ${PSERVER_TEXT_PATH}"
    COMMAND=${TEXT_PATH}
else
	TEXT_PATH=$PROJECT_HOME/resources/etc/scriptdata/addmanualdata/*/*.txt
	COMMAND=`ls ${TEXT_PATH} | sort -f`
fi

ls  ${TEXT_PATH} >/dev/null 2>&1
if [ $? -ne 0 ]
then
echo "No manual data to add for $1";
exit 0;
fi

for filepath in ${COMMAND}
do
contains $filepath $rel
if [ $? -eq 0 ]
then
jsonfile=${filepath%???}json
j=0
while IFS=\n read -r i
do
echo "##### Begin putTool for $i ##### from file $filepath" | tee -a $OUTFILE
resource=`echo $i | tr -d '\r'`
$PROJECT_HOME/scripts/putTool.sh $resource $jsonfile 1 0 na 1 >> $OUTFILE 2>&1 || error_exit "$resource" $j $filepath
echo "##### End putTool for $resource #####" | tee -a $OUTFILE
echo "Begin getTool for $resource" | tee -a $OUTFILE
$PROJECT_HOME/scripts/getTool.sh $resource >> $OUTFILE 2>&1 || error_exit "$i" $j $filepath
echo "End getTool for $resource" | tee -a $OUTFILE

j=$(expr "$j" + 1)
k=$(expr "$k" + 1)
done < $filepath

fi

done
if [ $k -eq 0 ]
then
echo "No manual data to add for release $1";
exit 0;
fi

echo "See output and error file: $OUTFILE"

exit 0
