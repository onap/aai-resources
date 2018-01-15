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
# The script invokes RelationshipputDel java class to PUT/DELETE a relationship
#
# sample json file for an oam-network node to put a relationship to a complex
#{
#         "related-to": "complex",
#         "relationship-data": [         {
#            "relationship-key": "complex.physical-location-id",
#            "relationship-value": "CHCGILCL73W"
#         }]
#}
#
# method checking parameter list for two strings, and determine if
# the second string is a sub-string of the first
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

display_usage() {
        cat <<EOF
        Usage: $0 [options]

        1. Usage: rshipTool.sh <action> <resource-path> <json-file>
        2. This script needs 3 arguments, and the arguments should be an action, a resource-path and a json file.
        3. example action:  PUT or DELETE
        4. example resource path: cloud-infrastructure/pservers/pserver/{hostname}/relationship-list/relationships
        5. example json file name: json file argument should be the location of the file such as /tmp/jsonFile.json
        6. example: format for json file contents 
        		{"related-to": "complex",
         		"relationship-data": [         {
            		"relationship-key": "complex.physical-location-id",
            		"relationship-value": "CHCGILCL73W"
         		}]}
EOF
}

if [ $# -eq 0 ]; then
        display_usage
        exit 1
fi

# Check to see if PUT or DELETE
ACTION=$(echo $1 | tr '[:lower:]' '[:upper:]')
if [ "$ACTION" != "PUT" ]; then
	if [ "$ACTION" != "DELETE" ]; then
		echo "Method: $1 is an Invalid arguments for $0"
		exit 1
	fi
fi

# remove leading slash when present
RESOURCE=$(echo $2 | sed "s,^/,,")

if [ -z $RESOURCE ]; then
		echo "resource parameter is missing"
		echo "usage: $0 resource file [expected-failure-codes]"
		exit 1
fi
echo `date` "   Starting $0 for resource $RESOURCE"

RELATIONSHIP=""
contains $RESOURCE "/relationship-list/relationship"
if [ $? -ne 0 ]; then
        RELATIONSHIP="/relationship-list/relationship"
fi

JSONFILE=$3
if [ -z $JSONFILE ]; then
    	echo "json file parameter is missing"
       	echo "usage: $0 resource file [expected-failure-codes]"
       	exit 1
fi
ALLOWHTTPRESPONSES=$4

XFROMAPPID="AAI-TOOLS"
XTRANSID=`uuidgen`

userid=$( id | cut -f2 -d"(" | cut -f1 -d")" )
if [ "${userid}" != "aaiadmin" ]; then
    echo "You must be aaiadmin to run $0. The id used $userid."
    exit 1
fi 

. /etc/profile.d/aai.sh
PROJECT_HOME=/opt/app/aai-resources
prop_file=$PROJECT_HOME/bundleconfig/etc/appprops/aaiconfig.properties
log_dir=$PROJECT_HOME/logs/misc
today=$(date +\%Y-\%m-\%d)


MISSING_PROP=false
RESTURL=$(grep ^aai.server.url= $prop_file |cut -d'=' -f2 |tr -d "\015")
if [ -z $RESTURL ]; then
		echo "Property [aai.server.url] not found in file $prop_file"
        MISSING_PROP=true
fi
USEBASICAUTH=false
BASICENABLE=$(grep ^aai.tools.enableBasicAuth $prop_file |cut -d'=' -f2 |tr -d "\015")
if [ -z $BASICENABLE ]; then
        USEBASICAUTH=false
else
        USEBASICAUTH=true
        CURLUSER=$(grep ^aai.tools.username $prop_file |cut -d'=' -f2 |tr -d "\015")
        if [ -z $CURLUSER ]; then
                echo "Property [aai.tools.username] not found in file $prop_file"
                MISSING_PROP=true
        fi
        CURLPASSWORD=$(grep ^aai.tools.password $prop_file |cut -d'=' -f2 |tr -d "\015")
        if [ -z $CURLPASSWORD ]; then
                echo "Property [aai.tools.password] not found in file $prop_file"
                MISSING_PROP=true
        fi
fi

if [ $MISSING_PROP = false ]; then
        if [ $USEBASICAUTH = false ]; then
                AUTHSTRING="--cert $PROJECT_HOME/bundleconfig/etc/auth/aaiClientPublicCert.pem --key $PROJECT_HOME/bundleconfig/etc/auth/aaiClientPrivateKey.pem"
        else
                AUTHSTRING="-u $CURLUSER:$CURLPASSWORD"
        fi
        
	RESOURCEVERSION=$(curl --request GET -sL -k $AUTHSTRING -H "X-FromAppId: $XFROMAPPID" -H "X-TransactionId: $XTRANSID" -H "Accept: application/json" $RESTURL$RESOURCE | python -c "import sys, json; print json.load(sys.stdin)['resource-version']")
	if [ $ACTION = "PUT" ]; then
		result=`curl --request PUT -sL -w "%{http_code}" -o /dev/null -k $AUTHSTRING -H "X-FromAppId: $XFROMAPPID" -H "X-TransactionId: $XTRANSID" -H "Accept: application/json" -T $JSONFILE $RESTURL$RESOURCE$RELATIONSHIP?$RESOURCEVERSION`
        #echo "result is $result."
        RC=0;
        if [ $? -eq 0 ]; then
                case $result in
                        +([0-9])?)
                                #if [[ "$result" -eq 412 || "$result" -ge 200 && $result -lt 300 ]]
                                if [[ "$result" -ge 200 && $result -lt 300 ]]
                                then
                                        echo "PUT result is OK,  $result"
                                        curl --request GET -sL -k $AUTHSTRING -H "X-FromAppId: $XFROMAPPID" -H "X-TransactionId: $XTRANSID" -H "Accept: application/json" $RESTURL$RESOURCE | python -mjson.tool
                                else
                                        if [ -z $ALLOWHTTPRESPONSES ]; then
                                                echo "PUT request failed, response code was  $result"
                                                RC=$result
                                        else
                                                contains $ALLOWHTTPRESPONSES $result
                                                if [ $? -ne 0 ]
                                                then
                                                        echo "PUT request failed, unexpected response code was  $result"
                                                        RC=$result
                                                else
                                                        echo "PUT result is expected,  $result"
                                                fi
                                        fi
                                fi
                                ;;
                        *)
                                echo "PUT request failed, response was $result"
                                RC=-1
                                ;;

                esac
        else
                echo "FAILED to send request to $RESTURL"
                RC=-1
        fi
	else				
		result=`curl --request DELETE -sL -w "%{http_code}" -o /dev/null -k $AUTHSTRING -H "X-FromAppId: $XFROMAPPID" -H "X-TransactionId: $XTRANSID" -H "Accept: application/json" -T $JSONFILE $RESTURL$RESOURCE$RELATIONSHIP?$RESOURCEVERSION`
        echo "result is $result."
        RC=0;
        if [ $? -eq 0 ]; then
                case $result in
                        +([0-9])?)
                                if [[ "$result" -ge 200 && $result -lt 300 ]]
                                then
                                        echo "DELETE result is OK,  $result"
                                        curl --request GET -sL -k $AUTHSTRING -H "X-FromAppId: $XFROMAPPID" -H "X-TransactionId: $XTRANSID" -H "Accept: application/json" $RESTURL$RESOURCE | python -mjson.tool
                                else
                                        echo "failed DELETE request, response code was  $result"
                                        RC=$result
                                fi
                                ;;
                        *)
                                echo "DELETE request failed, response was $result"
                                RC=-1
                                ;;

                esac
        else
                echo "FAILED to send request to $RESTURL"
                RC=-1
        fi
		
	fi       

else
        echo "usage: $0 resource"
        RC=-1
fi

echo `date` "   Done $0, returning $RC"
exit $RC