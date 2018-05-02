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
# This script is used to change the cardinality of an existing database property.  
# It currently just allows you to change TO a SET cardinality with dataType = String.
# It does not currently let you preserve the data (since we're just doing this for one
#      field which nobody uses yet).    
#
# Note also - This script just makes changes to the schema that is currently live.
#    If you were to create a new schema in a brandy-new environment, it would look like
#    whatever ex5.json (as of Jan 2016) told it to look like.   So, part of making a 
#    change to the db schema should Always first be to make the change in ex5.json so that
#    future environments will have the change.  This script is just to change existing
#    instances of the schema since schemaGenerator (as of Jan 2016) does not update things - it 
#    just does the initial creation.
#
# Boy, this is getting to be a big comment section...
#
# To use this script, you need to pass four parameters:
#      propertyName    -- the name of the property that you need to change Cardinality on.
#      targetDataType  -- whether it's changing or not, you need to give it:  For now -- we only allow "String"
#      targetCardinality -- For now -- only accepts "SET".   In the future we should support ("SET", "LIST" or "SINGLE")
#      preserveDataFlag -- true or false.     For now -- only supports "false"
#
# Ie.    changePropertyCardinality.sh supplier-release-list String SET false
#

COMMON_ENV_PATH=$( cd "$(dirname "$0")" ; pwd -P )
. ${COMMON_ENV_PATH}/common_functions.sh

start_date;
check_user;

if [ "$#" -ne 4 ]; then
    echo "Illegal number of parameters"
    echo "usage: $0 propertyName targetDataType targetCardinality preserveDataFlag"
    exit 1
fi

source_profile;

execute_spring_jar org.onap.aai.dbgen.ChangePropertyCardinality ${PROJECT_HOME}/resources/etc/appprops/schemaMod-logback.xml "$1 $2 $3 $4"
if [ "$?" -ne "0" ]; then
    echo "Problem executing ChangePropertyCardinality "
    end_date;
    exit 1
fi

end_date;
exit 0
