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
# vmExportCloudRegions.sh  -- This tool is used to generate script payloads of
# vm and interfaces linked to a specific cloud-owner to be imported
# to the same cloud-region in a instance with its own data model and existing
# dependent objects. This script maps predefined objects and relationsships
# beteen the two instances. An archive is generated and manually
# copied to the target instance. A second script, vmValidateCloudRegions.sh,
# is run on the target instance that applies validations and replaces
# values needed in the generated scripss.
# generated files. The script takes 2 arguments

# usage: $0 <cloud-owner> <target-basepath> <target-version>
# <cloud-owner> is the cloud-region-owner
# <target-version> is the replacement to be applied to aai/$version in 
# resource links from the source instance.

# scriptdata/addmanualdata/vm_export/payload is populated with the target
# files, that include json/txt files applied by addManualData script and tx
# files which represent validations to be applied before running addManualData.
# The naming convention of json/txt files ensures the ordering used to correctly
# apply dependent objects. The archive is generated in
# scriptdata/addmanualdata/vm_export/archive

addTemplates() {
	jq -n --arg related vlan-tag --arg rlabel org.onap.relationships.inventory.Uses \
--arg rlink /aai/v1x/cloud-infrastructure/cloud-regions/cloud-region/change-this-to-cloud-owner/change-this-to-cloud-region-id/vlan-ranges/vlan-range/change-this-to-vlan-range-id/vlan-tags/vlan-tag/change-this-to-vlan-tag-id \
'{"related-to": "\($related)", "relationship-label": "\($rlabel)", "related-link": "\($rlink)"}' > $TARGETDIR/vlan-tag-relationship-template.json

	jq -n --arg related lag-interface --arg rlabel org.onap.relationships.inventory.Uses \
--arg rlink /aai/v1x/cloud-infrastructure/pservers/pserver/change-this-to-hostname/lag-interfaces/lag-interface/bond1 \
'{"related-to": "\($related)", "relationship-label": "\($rlabel)", "related-link": "\($rlink)"}' > $TARGETDIR/lag-interface-relationship-template.json

	jq -n --arg dsl "cloud-region('cloud-owner','change-this-to-cloud-owner')('cloud-region-id','change-this-to-cloud-region-id') > vlan-range > [ vlan-tag*('vlan-id-inner', change-this-to-inner), vlan-tag*('vlan-id-outer', change-this-to-outer) ]" \
'{"dsl": "\($dsl)"}' > $TARGETDIR/vlantagquery-template.json
}

getFileSize() {
	echo `wc -c < $1`
}

getCloudRegions() {
	res="cloud-infrastructure/cloud-regions?cloud-owner="$1"&format=pathed"
	cloudres=`$PROJECT_HOME/scripts/getTool.sh "${res}" | sed '1d;$d'`
	hasResults=`echo $cloudres | jq '.results'`
	if [ "$hasResults" = null ]; then
		hasResults="No-cloud-region"
	fi
	if [ "$hasResults" = "No-cloud-region" ]; then
		echo "No-cloud-region"
	else
		echo $cloudres | jq '.results|.[]|."resource-link"' | sed -e 's/\"//g' | cut -d"/" -f4-
	fi
}

getTenants() {
	res=""$1"/tenants?format=pathed"
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	hasResults=`echo $cloudres | jq '.results'`
	if [ "$hasResults" = null ]; then
		hasResults="No-tenant"
	fi
	if [ "$hasResults" = "No-tenant" ]; then
		echo "No-tenant"
	else
		echo $cloudres | jq '.results|.[]|."resource-link"' | sed -e 's/\"//g' | cut -d"/" -f4-
	fi
}

getVservers() {
	res=""$1"/vservers?format=pathed"
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	hasResults=`echo $cloudres | jq '.results'`
	if [ "$hasResults" = null ]; then
		hasResults="No-vserver"
	fi
	if [ "$hasResults" = "No-vserver" ]; then
		echo "No-vserver"
	else
		echo $cloudres | jq '.results|.[]|."resource-link"' | sed -e 's/\"//g' | cut -d"/" -f4-
	fi
}

getLInterfaces() {
	res=""$1"/l-interfaces?format=pathed"
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	hasResults=`echo $cloudres | jq '.results'`
	if [ "$hasResults" = null ]; then
		hasResults="No-l-interface"
	fi
	if [ "$hasResults" = "No-l-interface" ]; then
		echo "No-l-interface"
	else
		echo $cloudres | jq '.results|.[]|."resource-link"' | sed -e 's/\"//g' | cut -d"/" -f4-
	fi
}

getVlans() {
	res=""$1"/vlans?format=pathed"
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	hasResults=`echo $cloudres | jq '.results'`
	if [ "$hasResults" = null ]; then
		hasResults="No-vlan"
	fi
	if [ "$hasResults" = "No-vlan" ]; then
		echo "No-vlan"
	else
		echo $cloudres | jq '.results|.[]|."resource-link"' | sed -e 's/\"//g' | cut -d"/" -f4-
	fi
}

getSriovVfs() {
	res=""$1"/sriov-vfs?format=pathed"
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	hasResults=`echo $cloudres | jq '.results'`
	if [ "$hasResults" = null ]; then
		hasResults="No-sriov-vfs"
	fi
	if [ "$hasResults" = "No-sriov-vfs" ]; then
		echo "No-sriov-vfs"
	else
		echo $cloudres | jq '.results|.[]|."resource-link"' | sed -e 's/\"//g' | cut -d"/" -f4-
	fi
}

getTenant() {
	res=""$1"?depth=0"
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	filepath=$TARGETDIR/$regioncnt-$tenantcnt-tenant
	echo "/"$1 > ${filepath}.txt
	echo $cloudres | jq 'del(."resource-version")' > ${filepath}.json
	echo $filepath
}

getVserver() {
	res=""$1"?depth=0"
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	filepath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-1-vserver
	echo "/"$1 > ${filepath}.txt
	echo $cloudres | jq 'del(."resource-version")|del(."relationship-list")' > ${filepath}.json
	validatepath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-vserver-validate-notexists
	echo "/"$1 > ${validatepath}.tx
	echo $filepath
}

getVfModule() {
	res=""$1"?depth=0"
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	heatStackId=`echo $cloudres | jq '."heat-stack-id"' | sed 's/\"//g'` 
	if [ "$heatStackId" = "null" ]
	then
		echo $heatStackId
	else
		heatStackIdUri=`echo $heatStackId | jq -sRr @uri`
		uri=`echo ${heatStackIdUri%???}`
		filepath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-1-instance-group-$vfmodulecnt
		echo "/network/instance-groups/instance-group/$uri" > ${filepath}.txt
		# no need to validate, since instance-group is created if not present
		#validatepath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-vserver-validate-vfmodule-$vfmodulecnt.tx
		echo $cloudres | jq '{"heat-stack-id": ."heat-stack-id", "vf-module-name": ."vf-module-name"} | .["id"] = ."heat-stack-id" | .["instance-group-name"] = ."vf-module-name" | del(."heat-stack-id", ."vf-module-name") | .+ {"instance-group-type": "HEAT-STACK"}' > ${filepath}.json
		echo $uri
	fi
}

getVserverRelatedTo() {
	res=""$1"?depth=0"
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	hasResults=`echo $cloudres | jq '."relationship-list"'`
	if [ "$hasResults" = null ]; then
		hasResults="No-related-vf-module-or-pserver"
	fi
	if [ "$hasResults" = "No-related-vf-module-or-pserver" ]; then
		echo "No-related-vf-module-or-pserver"
	else
		validatepath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-vserver-validate.tx
		echo $cloudres | jq '[ ."relationship-list".relationship[] | select(."related-to" | contains("pserver")) ]' > ${validatepath}
		sz=`getFileSize $validatepath`
		pservercnt=0
		if [ $sz = "3" ]; then
			rm ${validatepath}
		else
			PSERVERS=`cat ${validatepath} | jq '.[]|."related-link"'|sed -e 's/\"//g' | cut -d"/" -f4-`
			rm ${validatepath}
			for pserver in ${PSERVERS}
			do
				validatepath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-vserver-validate-pserver-$pservercnt-exists.tx
				echo $pserver > $validatepath
				filepath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-0-vserver-related-to-pserver-$pservercnt
				#echo $cloudres |  jq '."relationship-list".relationship[] | select(."related-to" | contains("pserver"))' > ${filepath}.json
				echo $cloudres |  jq '."relationship-list".relationship[] | select(."related-to" | contains("pserver"))' | jq 'del(."relationship-data")' | jq 'del(."related-to-property")' > ${filepath}.json
				echo "/"$1/relationship-list/relationship > ${filepath}.txt
				pservercnt=$(expr "$pservercnt" + 1)
			done
		fi
		validatepath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-vserver-validate.tx
		echo $cloudres | jq '[ ."relationship-list".relationship[] | select(."related-to" | contains("vf-module")) ]' > ${validatepath}
		sz=`getFileSize ${validatepath}`
		if [ $sz = "3" ]; then
			rm ${validatepath}
		else
			VFMODULES=`cat ${validatepath} | jq '.[]|."related-link"'|sed -e 's/\"//g' | cut -d"/" -f4-`
			vfmodulecnt=0
			rm ${validatepath}
			for vfmodule in ${VFMODULES}
			do
				heatId=`getVfModule $vfmodule`
				# add instance-group and keep pserver relationships
				if [ `expr $pservercnt` -ne 0 ]; then
					filepath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-0-vserver-related-to-instance-group
					echo $cloudres | jq '."relationship-list".relationship |= .+ [{ "related-to": "instance-group", "relationship-label": "org.onap.relationships.inventory.MemberOf", "related-link": "/aai/v1x/network/instance-groups/instance-group/change-this-instance-group-id" }]' | jq '."relationship-list".relationship[] | select(."related-to" | contains("instance-group"))' | sed "s#change-this-instance-group-id#$heatId#" > ${filepath}.json
echo $cloudres | jq '."relationship-list".relationship |= .+ [{ "related-to": "instance-group", "relationship-label": "org.onap.relationships.inventory.MemberOf", "related-link": "/aai/v1x/network/instance-groups/instance-group/change-this-instance-group-id" }]' | jq '."relationship-list".relationship[] | select(."related-to" | contains("instance-group"))'
					echo "/"$1/relationship-list/relationship > ${filepath}.txt
					vfmodulecnt=$(expr "$vfmodulecnt" + 1)
				fi
			done 
		fi
		echo $filepath
	fi
}

getLInterface() {
	res=""$1"?depth=0"
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	filepath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-$linterfacecnt-1-l-interface
	echo "/"$1 > ${filepath}.txt
	echo $cloudres | jq 'del(."resource-version")|del(."relationship-list")' > ${filepath}.json
	echo "/"$1 > ${filepath}.txt
	echo $cloudres | jq 'del(."resource-version")|del(."relationship-list")' > ${filepath}.json
	echo $filepath
}

getVlan() {
	res=""$1"?depth=0"
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	filepath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-$linterfacecnt-0-$vlancnt-vlan-validate-vlantag.tx
	echo $cloudres | jq 'del(."resource-version")|del(."relationship-list")' > ${filepath}
	relpath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-$linterfacecnt-0-$vlancnt-vlantag-related-to
	echo "$2/relationship-list/relationship" > ${relpath}.txt
	# payload
	cloudreg=`cat $TARGETDIR/$regioncnt-cloud-region.validate.exists.tx` 
	owner=`echo $cloudreg | cut -f 5 -d "/"`
	cid=`echo $cloudreg | cut -f 6 -d "/"`
	cat $TEMPLATEDIR/vlan-tag-relationship-template.json | sed -e "s/change-this-path-element/$targetVersion/" -e "s/change-this-to-cloud-owner/$owner/" -e "s/change-this-to-cloud-region-id/$cid/"  > ${relpath}.json
	echo $filepath
}

getSriovVf() {
	res=""$1"?depth=0"
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	filepath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-$linterfacecnt-$sriovvfcnt-1-sriov-vf
	echo "/"$1 > ${filepath}.txt
	echo $cloudres | jq 'del(."resource-version")|del(."relationship-list")' > ${filepath}.json
	echo $filepath
}

getLagInterfaceRelatedTo() {
	filepath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-$linterfacecnt-0-laginterface-related-to
	pserverpath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-vserver-validate-pserver-0-exists.tx
	if [ -s $pserverpath ];
	then
		server=`cat $pserverpath`
		echo "$1/relationship-list/relationship" > ${filepath}.txt
		hostname=`echo $server | cut -f4 -d"/"`
		cat $TEMPLATEDIR/lag-interface-relationship-template.json | sed -e "s/change-this-path-element/$targetVersion/" -e "s/change-this-to-hostname/$hostname/" > ${filepath}.json
		validatepath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-$linterfacecnt-laginterface-validate-exists
		echo "/cloud-infrastructure/pservers/pserver/$hostname/lag-interfaces/lag-interface/bond1" > ${validatepath}.tx
	fi
}

getSriovVfRelatedTo() {
	res=""$1"?depth=0"
	cloudres=`$PROJECT_HOME/scripts/getTool.sh $res | sed '1d;$d'`
	hasResults=`echo $cloudres | jq '."relationship-list"'`
	if [ "$hasResults" = null ]; then
		hasResults="No-related-to-sriov-pf"
	fi
	if [ "$hasResults" = "No-related-to-sriov-pf" ]; then
		echo "No-related-to-sriov-pf"
	else
		validatepath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-$linterfacecnt-$sriovvfcnt-sriovvf-validate-sriov-pf.tx
		#echo "getSrioVfRelatedTo:" $validatepath $res >> /tmp/test.out
		#echo "getSrioVfRelatedTo:" $validatepath $cloudres >> /tmp/test.out
		echo $cloudres | jq '[ ."relationship-list".relationship[] | select(."related-to" | contains("sriov-pf")) ]' > ${validatepath}
		sz=`getFileSize ${validatepath}`
		sriovpfcnt=0
		if [ $sz = "3" ]; then
			rm ${validatepath}
		else
			SRIOVPFS=`cat ${validatepath} | jq '.[]|."related-link"'|sed -e 's/\"//g' | cut -d"/" -f4-`
			rm ${validatepath}
			for sriovpf in ${SRIOVPFS}
			do
				validatepath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-$linterfacecnt-$sriovvfcnt-sriovvf-validate-sriov-pf-$sriovpfcnt.tx
				echo $sriovpf > $validatepath
				#echo "getSrioVfRelatedTo:" $validatepath $sriovpf >> /tmp/test.out
				filepath=$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-$linterfacecnt-$sriovvfcnt-0-sriovvf-related-to-sriov-pf-$sriovpfcnt
				echo $cloudres |  jq '."relationship-list".relationship[] | select(."related-to" | contains("sriov-pf"))' | jq 'del(."relationship-label")' | jq 'del(."relationship-data")' > ${filepath}.json
				echo "/"$1/relationship-list/relationship > ${filepath}.txt
				#echo "getSrioVfRelatedTo:" $filepath $cloudres >> /tmp/test.out
				sriovpfcnt=$(expr "$sriovpfcnt" + 1)
			done 
		fi
		echo $filepath
	fi
}

addValidateCloudRegion() {
	filepath=$TARGETDIR/$regioncnt-cloud-region
	echo "/"$1 > ${filepath}.validate.exists.tx
}

addValidateVserver() {
	filepath=$TARGETDIR/$regioncnt-cloud-region
	echo "/"$1 > ${filepath}.validate.exists.tx
}
COMMON_ENV_PATH=$( cd "$(dirname "$0")" ; pwd -P )
. ${COMMON_ENV_PATH}/common_functions.sh



PROJECT_HOME=/opt/app/aai-resources
PROGNAME=$(basename $0)

TS=$(date "+%Y_%m_%d_%H_%M_%S")

CHECK_USER="aaiadmin"
userid=$( id | cut -f2 -d"(" | cut -f1 -d")" )
if [ "${userid}" != $CHECK_USER ]; then
    echo "You must be  $CHECK_USER to run $0. The id used $userid."
    exit 1
fi
TARGETDIR=$PROJECT_HOME/resources/etc/scriptdata/addmanualdata/vm_export/payload
TEMPLATEDIR=$TARGETDIR
mkdir -p $TARGETDIR
rm -f $TARGETDIR/*

if [ "$#" -ne 3 ]; then
    echo "usage: $0 <cloud-owner> <target-basepath> <target-verson>"
    exit 1
fi

addTemplates

targetVersion="$2\/$3"

echo "${TS}, getting vms and interfaces for cloud-region-owner: $1 with targetVersion $2/$3"
regioncnt=0
tenantcnt=0
vservercnt=0
linterfacecnt=0
vlancnt=0
sriovvfcnt=0
totaltenants=0
totalvms=0
totalinterfaces=0
totalvlans=0
totalsriovvfs=0

missingvms=0
missinginterfaces=0
missingvlans=0
missingsriovvfs=0



CLOUDS=`getCloudRegions $1`
for cloud in ${CLOUDS}
do
	if [ $cloud = "No-cloud-region" ]; then
		echo "No cloud-region found for owner " $1
	else
		addValidateCloudRegion $cloud
		TENANTS=`getTenants $cloud`
		for tenant in ${TENANTS}
		do
			if [ $tenant = "No-tenant" ]; then
				echo "No tenant found for cloud-region " $cloud
			else
				#echo "Process tenant " $tenant
				add=`getTenant $tenant`
				#echo "adding tenant: " $add
				VSERVERS=`getVservers $tenant`
				for vserver in ${VSERVERS}
				do
					if [ $vserver = "No-vserver" ]; then
						#echo "No vserver found for tenant " $tenant
						missingvms=$(expr "$missingvms" + 1)
					else
						#echo "Process vserver " $vserver
						add=`getVserver $vserver`
						#echo "adding vserver: " $add
						add=`getVserverRelatedTo $vserver`
						#echo "adding vserver: related-to " $add
						LINTERFACES=`getLInterfaces $vserver`
						linterfacecnt=0
						for linterface in ${LINTERFACES}
						do
							if [ $linterface = "No-l-interface" ]; then
								#echo "No l-interface found for vserver " $vserver
								missinginterfaces=$(expr "$missinginterfaces" + 1)
							else
								#echo "Process l-interface " $linterface
								add=`getLInterface $linterface`
								#echo "adding l-interface: " $add
								VLANS=`getVlans $linterface`
								for vlan in ${VLANS}
								do
									if [ $vlan = "No-vlan" ]; then
										#echo "No vlan found for l-interface " $linterface
										missingvlans=$(expr "$missingvlans" + 1)
									else
										#echo "Process vlan " $vlan
										add=`getVlan $vlan $linterface`
										#echo "adding vlan: " $add
										vlancnt=$(expr "$vlancnt" + 1)
										totalvlans=$(expr "$totalvlans" + 1)
									fi
								done
								SRIOVVFS=`getSriovVfs $linterface`
								for sriovvf in ${SRIOVVFS}
								do
									if [ $sriovvf = "No-sriov-vfs" ]; then
										#echo "No sriov-vf found for l-interface " $linterface
										# need to add relationship for pserver/lag-interface bond1
										getLagInterfaceRelatedTo $linterface
										missingsriovvfs=$(expr "$missingsriovvfs" + 1)
									else
										#echo "Processing l-interface" $linterface >> /tmp/test.out
										#echo "Processing with sriov-vf" $sriovvf >> /tmp/test.out
										add=`getSriovVf $sriovvf`
										#echo "adding sriovvf: " $add
										add=`getSriovVfRelatedTo $sriovvf`
										#echo "adding sriovvf: related-to " $add
										sriovvfcnt=$(expr "$sriovvfcnt" + 1)
										totalsriovvfs=$(expr "$totalsriovvfs" + 1)
									fi
								done
								vlancnt=0
								sriovvfcnt=0
								linterfacecnt=$(expr "$linterfacecnt" + 1)
								totalinterfaces=$(expr "$totalinterfaces" + 1)
							fi
						done
						linterfacecnt=0
						# check that vserver related-to is present
						if [ -e "$TARGETDIR/$regioncnt-$tenantcnt-$vservercnt-0-vserver-related-to-instance-group.json" ]; then
							vservercnt=$(expr "$vservercnt" + 1)
							totalvms=$(expr "$totalvms" + 1)
						else
							rm $TARGETDIR/$regioncnt-$tenantcnt-$vservercnt*
							echo "no related vf-module, skip export for $vserver"
						fi
					fi
				done
				vservercnt=0
				tenantcnt=$(expr "$tenantcnt" + 1)
				totaltenants=$(expr "$totaltenants" + 1)
			fi
		done
		tenantcnt=0
		regioncnt=$(expr "$regioncnt" + 1)
	fi
done
sed -i "s/aai\/v1./$targetVersion/g" $TARGETDIR/*related-to*.json
echo "Total cloud-regions to process " $regioncnt
echo "Total tenants to add " $totaltenants
echo "Total vms to add " $totalvms
#echo "Total interfaces to add " $totalinterfaces
#echo "Total vlans to add " $totalvlans
#echo "Total sriovvfs to add " $totalsriovvfs

TS=$(date "+%Y_%m_%d_%H_%M_%S")
ARCHIVEDIR=$PROJECT_HOME/resources/etc/scriptdata/addmanualdata/vm_export/archive
if [ ! -d ${ARCHIVEDIR} ]
then
        mkdir -p ${ARCHIVEDIR}
        chown aaiadmin:aaiadmin ${ARCHIVEDIR}
        chmod u+w ${ARCHIVEDIR}
fi
cd ${TARGETDIR}
tar c * -f ${ARCHIVEDIR}/vmExportArchive_${TS}.tar --exclude=payload
if [ $? -ne 0 ]
then
        echo " Unable to tar ${TARGETDIR}"
        exit 1
fi
cd ${ARCHIVEDIR}
gzip ${ARCHIVEDIR}/vmExportArchive_${TS}.tar

if [ $? -ne 0 ]
then
        echo " Unable to gzip ${ARCHIVE_DIRECTORY}/vmExportArchive_${TS}.tar"
        exit 1
fi
echo "Completed successfully: ${ARCHIVE_DIRECTORY}/vmExportArchive_${TS}.tar"
exit 0