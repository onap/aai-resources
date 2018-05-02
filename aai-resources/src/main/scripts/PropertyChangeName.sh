#!/bin/ksh
#
# ============LICENSE_START=======================================================
# org.onap.aai
# ================================================================================
# Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
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

#
# This script is used to add a target property name with the same value in the database schema 
# to a vertex which has an existing property. The existing property is not removed.
#
# Note also - This script just makes changes to the schema that is currently live.
#    If you were to create a new schema in a brandy-new environment, it would look like
#    whatever the oxm (as of July 2016) told it to look like.   So, part of making a 
#    change to the db schema should Always first be to make the change in the oxm so that
#    future environments will have the change.  This script is just to change existing
#    instances of the schema since schemaGenerator (as of July 2015) does not update things - it 
#    just does the initial creation.
#
# To use this script, you need to pass four parameters:
#      propertyName    -- the name of the property that has the value to be used in the targetProperty
#      targetPropertyName  -- the name of the targetProperty
#      targetNodeType -- NA if all propertyName instances in the DB are impacted, otherwise limit the change to this nodeType
#      skipCommit -- true or false.     For testing, skips the commit when set to true.
#
# Ie.    propertyNameChange service-id persona-model-id service-instance true
#

echo "RETIRED: Reach out to delivery team if this needs to be execued."
#COMMON_ENV_PATH=$( cd "$(dirname "$0")" ; pwd -P )	
#. ${COMMON_ENV_PATH}/common_functions.sh
#start_date;
#check_user;


#if [ "$#" -ne 4 ]; then
#    echo "Illegal number of parameters"
#    echo "usage: $0 propertyName targetPropertyName targetNodeType skipCommit"
#    exit 1
#fi

#source_profile;
#execute_spring_jar org.onap.aai.dbgen.PropertyNameChange ${PROJECT_HOME}/resources/etc/appprops/schemaMod-logback.xml "$1 $2 $3 $4"
#if [ "$?" -ne "0" ]; then
#    echo "Problem executing propertyNameChange "
#	 end_date;
#    exit 1
#fi

 
#end_date;
#exit 0
