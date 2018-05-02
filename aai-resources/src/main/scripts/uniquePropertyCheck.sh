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
# The script invokes UniqueProperty java class to see if the passed property is unique in the db and if
#    not, to display where duplicate values are found.
#
# For example:    uniquePropertyCheck.sh subscriber-name
#

COMMON_ENV_PATH=$( cd "$(dirname "$0")" ; pwd -P )	
. ${COMMON_ENV_PATH}/common_functions.sh
start_date;
check_user;
source_profile;

#execute_spring_jar org.onap.aai.util.UniquePropertyCheck ${PROJECT_HOME}/resources/etc/appprops/uniquePropertyCheck-logback.xml "$@"
execute_spring_jar org.onap.aai.util.UniquePropertyCheck ${PROJECT_HOME}/resources/uniquePropertyCheck-logback.xml "$@"
ret_code=$?
if [ $ret_code != 0 ]; then
  end_date;
  exit $ret_code
fi

end_date;
exit 0