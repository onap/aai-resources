#!/bin/bash
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

##############################################################################
#       Script to initialize the chef-repo branch and.chef
#
##############################################################################

cd /var/chef;

if [ ! -d "aai-config" ]; then

    git clone --depth 1 -b ${CHEF_BRANCH} --single-branch ${CHEF_CONFIG_GIT_URL}/${CHEF_CONFIG_REPO}.git aai-config || {
        echo "Error: Unable to clone the aai-config repo with url: ${CHEF_GIT_URL}/${CHEF_CONFIG_REPO}.git";
        exit 1;
    }

fi

if [ -d "aai-config/cookbooks/aai-resources" ]; then

    (cd aai-config/cookbooks/aai-resources/ && \
        for f in $(ls); do mv $f ../; done && \
        cd ../ && rmdir aai-resources);

fi;

if [ ! -d "aai-data" ]; then

    git clone --depth 1 -b ${CHEF_BRANCH} --single-branch ${CHEF_DATA_GIT_URL}/aai-data.git aai-data || {
        echo "Error: Unable to clone the aai-data repo with url: ${CHEF_GIT_URL}";
        exit 1;
    }

fi

chef-solo \
   -c /var/chef/aai-data/chef-config/dev/.knife/solo.rb \
   -j /var/chef/aai-config/cookbooks/runlist-aai-resources.json \
   -E ${AAI_CHEF_ENV};

TITAN_REALTIME="/opt/app/aai-resources/bundleconfig/etc/appprops/titan-realtime.properties";

if [ ! -f ${TITAN_REALTIME} ]; then
	echo "Unable to find the titan realtime file";
	exit 1;
fi

HBASE_HOSTNAME=$(grep "storage.hostname" ${TITAN_REALTIME} | cut -d"=" -f2-);
HBASE_PORT="${HBASE_PORT:-2181}";
NUM_OF_RETRIES=${NUM_OF_RETRIES:-500};
retry=0;

while ! nc -z ${HBASE_HOSTNAME} ${HBASE_PORT} ;
do
	if [ $retry -eq $NUM_OF_RETRIES ]; then
		echo "Unable to connect to hbase after $NUM_OF_RETRIES retries, please check if hbase server is properly configured and be able to connect"; 
		exit 1;
	fi;

	echo "Waiting for hbase to be up";
	sleep 5;

	retry=$((retry + 1));
done

HBASE_STARTUP_ARTIFIIAL_DELAY=${HBASE_STARTUP_ARTIFIIAL_DELAY:-50};

# By default the artificial delay will be introduced
# the user can override it by set DISABLE_HBASE_STARTUP_ARTIFICIAL_DELAY to some string

if [ -z "${DISABLE_HBASE_STARTUP_ARTIFICIAL_DELAY}" ]; then
	sleep ${HBASE_STARTUP_ARTIFIIAL_DELAY};
fi;

/opt/app/aai-resources/bin/createDBSchema.sh || {
    echo "Error: Unable to create the db schema, please check if the hbase host is configured and up";
    exit 1;
}
