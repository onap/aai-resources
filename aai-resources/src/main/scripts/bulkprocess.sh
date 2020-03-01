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
#     http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END=========================================================
###

#
# This script is used to run the bulkprocess api for resources given the input of a directory, and headers. 
# This script would call bulkprocess for each json file in the directory, using the json file's contents as the payload for the call, and headers would be used accordingly.
# The output of each run will be saved to a new file. 
# The input files in the directory will be {someKey}-{0-9} for instance cloudRegion-1.json, cloudRegion-2.json etc. these need to be ran in sequential order based on the number.
# Nomenclature for the output is saved as <jsonfilename without json>.YYYYMMDDhhmmss.results.json


inputFolder=$1
if [ -z "$1" ]; then
	echo "Input folder string is empty."
   exit 1
fi

if [ ! -d "/opt/bulkprocess_load/$1" ]; then
	echo "Input folder could not be found."
   exit 1
fi

XFROMAPPID=$2
if [ -z "$2" ]; then
	echo "Missing XFROMAPPID."
   exit 1
fi

case "${XFROMAPPID}" in
  [a-zA-Z0-9][a-zA-Z0-9]*-[a-zA-Z0-9][a-zA-Z0-9]*) ;;
  *) echo "XFROMAPPID doesn't match the following regex [a-zA-Z0-9][a-zA-Z0-9]*-[a-zA-Z0-9][a-zA-Z0-9]*"; exit 1; ;;
esac

XTRANSID=$3

for input_file in $(ls -v /opt/bulkprocess_load/${inputFolder}/*);
do
    output_file=$(basename $input_file | sed 's/.json//g');
    /opt/app/aai-resources/scripts/putTool.sh /bulkprocess ${input_file} -display $XFROMAPPID  $XTRANSID > /tmp/${output_file}.$(date +"%Y%m%d%H%M%S").results.json;
done;

