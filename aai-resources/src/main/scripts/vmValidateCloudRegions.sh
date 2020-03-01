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

updateRenames () {
	echo $1 >> $PAYLOADDIR/rename.txt
}

renamefiles() {
	FILESTORENAME=`ls $PAYLOADDIR/$1*$2`
	for f in $FILESTORENAME
	do
		mv $f $f.bak
	done
	
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

error_exit () {
        echo "${PROGNAME}: failed for ${1:-"Unknown error"} on cmd $2 in $3" 1>&2
        echo "${PROGNAME}: failed for ${1:-"Unknown error"} on cmd $2 in $3" >> $OUTFILE
#       exit ${2:-"1"}
}

notfound="not-found"
found="found"

getCloudRegion() {
	res=$1?depth=0
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	hasResults=`echo $cloudres | jq '."cloud-owner"'`
	if [ "$hasResults" = null ]; then
		echo $notfound
	else
		echo $found
	fi
}

getVserver() {
	res=$1?depth=0
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	hasResults=`echo $cloudres | jq '."vserver-id"'`
	if [ "$hasResults" = null ]; then
		echo $notfound
	else
		echo $found
	fi
}

getPserver() {
	res=$1?depth=0
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	hasResults=`echo $cloudres | jq '."hostname"'`
	if [ "$hasResults" = null ]; then
		echo $notfound
	else
		echo $found
	fi
}

getSriovPf() {
	res=$1?depth=0
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	hasResults=`echo $cloudres | jq '."pf-pci-id"'`
	if [ "$hasResults" = null ]; then
		echo $notfound
	else
		echo $found
	fi
}

getLagInterface() {
	res=$1?depth=0
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	hasResults=`echo $cloudres | jq '."interface-name"'`
	if [ "$hasResults" = null ]; then
		echo $notfound
	else
		echo $found
	fi
}

getVlanTag() {
	vlanindex=`echo $1 | cut -f 10 -d "/" | cut -f 1-6 -d "-"`
	cloudindex=`echo $vlanindex | cut -f 1 -d "-"`
	cloudregion=`cat $PAYLOADDIR/$cloudindex-cloud-region.validate.exists.tx`
	cowner=`echo $cloudregion | cut -f 5 -d "/"`
	cid=`echo $cloudregion | cut -f 6 -d "/"`
	inner=`cat $1 | jq '."vlan-id-inner"' | sed 's/\"//g'`
	outer=`cat $1 | jq '."vlan-id-outer"' | sed 's/\"//g'`
	cat $TEMPLATEDIR/vlantagquery-template.json | sed -e "s/change-this-to-cloud-owner/$cowner/" -e "s/change-this-to-cloud-region-id/$cid/" -e "s/change-this-to-inner/$inner/" -e "s/change-this-to-outer/$outer/" > $PAYLOADDIR/$vlanindex-vlan-validate-vlantag.json 
	$PROJECT_HOME/scripts/putTool.sh "/dsl?format=pathed" $PAYLOADDIR/$vlanindex-vlan-validate-vlantag.json -display > $PAYLOADDIR/$vlanindex-vlan-validate-vlantag-result.json 2>&1
	cloudres=`cat $PAYLOADDIR/$vlanindex-vlan-validate-vlantag-result.json | sed '1,4d;$d'`
	hasResults=`echo $cloudres | jq '.results'`
	if [ "$hasResults" = null ]; then
		hasResults=$notfound
	fi
	if [ "$hasResoults" = $notfound ]; then
		echo $notfound
	else
		len=`echo $cloudres | jq '.results | length'`
		if [ $len -eq 0 ];
		then
			echo "vlan-tag-not-found"
		elif [ $len -gt 1 ];
			then
				echo "multiple-vlan-tag-found"
		else
			vlantag=`echo $cloudres | jq '.results|.[]|."resource-link"' | sed -e 's/\"//g' | cut -d"/" -f4-`
			rangeId=`echo $vlantag| cut -f 8 -d "/"`
			tagId=`echo $vlantag | cut -f 11 -d "/"`
			sed -i -e "s/change-this-to-vlan-range-id/$rangeId/" -e "s/change-this-to-vlan-tag-id/$tagId/" $PAYLOADDIR/$vlanindex-vlantag-related-to.json
			echo $found
		fi
	fi

}

PAYLOADDIR=$PROJECT_HOME/resources/etc/scriptdata/addmanualdata/vm_export
TEMPLATEDIR=$PROJECT_HOME/resources/etc/scriptdata/addmanualdata/vm_export
COMMAND=`find $PAYLOADDIR -name "*cloud-region.validate.exists.tx" -print | sort -f`

skipValidations=$1
if [ -z $skipValidations ]; then
	skipValidations="1"
fi

validatecnt=0
validcnt=0

echo "${TS}, starting validations"

for filepath in ${COMMAND}
do
	cloudindex=`echo ${filepath} | cut -f 10 -d "/" | cut -f 1 -d "-"`
	while IFS=\n read -r i
	do
		cloudregion=`getCloudRegion $i`
		if [[ $cloudregion == $found ]]
		then
			validcnt=$(expr $validcnt + 1)
		else
			echo "not importing vms under region $i"
			renamefiles "$cloudindex" "txt"
			renamefiles "$cloudindex" "tx"
		fi
		validatecnt=$(expr $validatecnt + 1)
	done < $filepath
done

skipvalidate=false
contains $skipValidations "vserver-validate-notexists"
if [ $? -eq 0 ] ; then
	skipvalidate=true
fi

COMMAND=`find $PAYLOADDIR -name "*vserver-validate-notexists.tx" -print | sort -f`
for filepath in ${COMMAND}
do
	cloudindex=`echo ${filepath} | cut -f 10 -d "/" | cut -f 1-3 -d "-"`
	while IFS=\n read -r i
	do
		vserver=`getVserver $i`
		if [[ $vserver == $notfound || $skipvalidate = true ]]
		then
			validcnt=$(expr $validcnt + 1)
		else
			echo "vm exists, $i not importing vm"
			updateRenames "$cloudindex"
		fi
		validatecnt=$(expr $validatecnt + 1)
	done < $filepath
done

COMMAND=`find $PAYLOADDIR -name "*vserver-validate-pserver-*-exists.tx" -print | sort -f`

for filepath in ${COMMAND}
do
	cloudindex=`echo ${filepath} | cut -f 10 -d "/" | cut -f 1-3 -d "-"`
	while IFS=\n read -r i
	do
		pserver=`getPserver $i`
		if [[ $pserver == $found ]]
		then
			validcnt=$(expr $validcnt + 1)
		else
			echo "missing pserver, $i, not importing related vms"
			updateRenames "$cloudindex"
		fi
		validatecnt=$(expr $validatecnt + 1)
	done < $filepath
done

COMMAND=`find $PAYLOADDIR -name "*sriovvf-validate-sriov-pf-*.tx" -print | sort -f`

for filepath in ${COMMAND}
do
	cloudindex=`echo ${filepath} | cut -f 10 -d "/" | cut -f 1-3 -d "-"`
	while IFS=\n read -r i
	do
		pserver=`getSriovPf $i`
		if [[ $pserver == $found ]]
		then
			validcnt=$(expr $validcnt + 1)
		else
			echo "missing sriov-pf, $i, not importing related vms"
			updateRenames "$cloudindex"
		fi
		validatecnt=$(expr $validatecnt + 1)
	done < $filepath
done

COMMAND=`find $PAYLOADDIR -name "*laginterface-validate-exists.tx" -print | sort -f`

for filepath in ${COMMAND}
do
	cloudindex=`echo ${filepath} | cut -f 10 -d "/" | cut -f 1-3 -d "-"`
	while IFS=\n read -r i
	do
		laginterface=`getLagInterface $i`
		if [[ $laginterface == $found ]]
		then
			validcnt=$(expr $validcnt + 1)
		else
			echo "missing lag-interface, $i, not importing related vms"
			updateRenames "$cloudindex"
		fi
		validatecnt=$(expr $validatecnt + 1)
	done < $filepath
done

COMMAND=`find $PAYLOADDIR -name "*vlan-validate-vlantag.tx" -print | sort -f`

for filepath in ${COMMAND}
do
	cloudindex=`echo ${filepath} | cut -f 10 -d "/" | cut -f 1-3 -d "-"`
	vlantag=`getVlanTag ${filepath}`
	if [[ $vlantag == $found ]]
	then
		validcnt=$(expr $validcnt + 1)
	elif [[ $vlantag == "vlan-tag-not-found" ]]
	then
		echo "no matching vlan-tag, $filepath, not importing related vms"
		updateRenames "$cloudindex"
	elif [[ $vlantag == "multiple-vlan-tag-found" ]]
	then
		echo "multipe vlan-tag matches, $filepath, not importing related vms"
		updateRenames "$cloudindex"
	else
		echo "missing vlan-tag, $filepath, not importing related vms"
		updateRenames "$cloudindex"
	fi
	validatecnt=$(expr $validatecnt + 1)
done

if [ -s $PAYLOADDIR/rename.txt ];
then
	cat $PAYLOADDIR/rename.txt | sed 's/^$//' | sort -u > $PAYLOADDIR/renamed.txt

	while IFS=\n read -r i
	do
		renamefiles $i "txt"
		renamefiles $i "tx"
	done <  "$PAYLOADDIR/renamed.txt"
	rm  $PAYLOADDIR/rename*
fi
TS=$(date "+%Y-%m-%d %H:%M:%S")
echo "${TS}, validations completed"
echo "Total validations done " $validatecnt
echo "Total validations passed " $validcnt

exit 0
