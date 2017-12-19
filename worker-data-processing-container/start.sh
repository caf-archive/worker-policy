#!/bin/bash
#
# Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
#
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
#


logFile="/opt/Copied/policy.yaml"

####################################################
####################################################
# Utility funcs
####################################################
####################################################


####################################################
# Get access to environment 
# vars which include dots or non-alphas in the name.
####################################################
function get_env_var() {
   local variableName="${1:-}" ; shift

   # helpful debug.
   # echo 'requesting environment variable name:' $variableName   
   # perl -le 'print $ENV{'$variableName'}';
   
   result=$(perl -le 'print $ENV{"'$variableName'"};')
}

####################################################
# Returns 1 - start Elasticsearch service
#             (default, or if POLICY_ELASTICSEARCH_DISABLED=false)
#		  0 - do not start Elasticsearch service
#			  (e.g. if POLICY_ELASTICSEARCH_DISABLED=true)
####################################################
should_start_elasticsearch() {

	get_env_var "POLICY_ELASTICSEARCH_DISABLED"
	# "POLICY_ELASTICSEARCH_DISABLED"

	[[ -n "$result" ]] && {

        if [ "$result" == "true" ]; then

			echo 'POLICY_ELASTICSEARCH_DISABLED is set to true - NOT enabling Elasticsearch service';
			return 0;

		fi;

        if [ "$result" == "false" ]; then

            echo 'POLICY_ELASTICSEARCH_DISABLED found as: false - enabling Elasticsearch service';
            return 1;

		fi;

		echo 'POLICY_ELASTICSEARCH_DISABLED found as:' $result ' - enabling Elasticsearch service';
		return 1;
	}

	echo 'POLICY_ELASTICSEARCH_DISABLED is not declared - enabling Elasticsearch service';
	return 1;
}

start_elasticsearch() {
    /opt/elasticsearchConfig/configureElasticsearch.sh
    echo "Attempting to start Elasticsearch ..."
    /opt/elasticsearch/scripts/start.sh |& sed -ue 's/^/Elasticsearch: /' &
    if [ -n "$POLICY_ELASTICSEARCH_VERIFY_ATTEMPTS" ];
	then
	  remainingChecks=$POLICY_ELASTICSEARCH_VERIFY_ATTEMPTS
	else
	  remainingChecks=10
	fi
    while [ "$remainingChecks" -ne "0" ]
    do
        if curl --silent http://localhost:9200/_cluster/health | grep -q 'cluster_name';
        then
            echo "Elasticsearch started."
            remainingChecks=0
        else
            remainingChecks=$[remainingChecks-1]
            if [ "$remainingChecks" -ne "0" ];
            then
                echo "Elasticsearch not started yet, re-verifying after a short delay ..."
                sleep 5s
            else
                echo "Elasticsearch failed to start in a reasonable time. Exiting..."
                exit 1
            fi
        fi
    done
}

####################################################
# Sets the log file path to the mesos mount if the file exists there.
####################################################
function set_logging_file_location_if_mounted(){

  if [ -e "/mnt/mesos/sandbox/policy.yaml" ]
  then
    logFile="/mnt/mesos/sandbox/policy.yaml"
    echo "Detected mesos policy logging location: " $logFile
  else
    echo "Uses default policy logging location: " $logFile
  fi
}

####################################################
# Sets the log file path to environment property if 
# there and found on disk, otherwise fallback to 
# the mesos mount if the file exists there.
####################################################
function set_logging_file_location(){

    get_env_var "POLICY_LOGGING_FILE"

    # test if we got anything from this environment option if so check it exists, 
    # if so use it now instead of default.
    [[ -n "$result" ]] && {

        if [ -e "$result" ]
        then
            echo "Detected log file location as: " $result
            logFile="$result"
            return;
        fi
    }

    # otherwise use fallback mesos directory if mounted.
    set_logging_file_location_if_mounted
}

####################################################
####################################################
# Start of actual execution.
####################################################
####################################################


echo "Starting setup"

#Attempt to Start Elasticsearch if configured on.
should_start_elasticsearch
if (($?==1)); then
	start_elasticsearch
fi;

set_logging_file_location

# If the CAF_APPNAME and CAF_CONFIG_PATH environment variables are not set, then use the
# JavaScript-encoded config files that are built into the container
if [ -z "$CAF_APPNAME" ] && [ -z "$CAF_CONFIG_PATH" ];
then
  export CAF_APPNAME=caf/worker
  export CAF_CONFIG_PATH=/opt/config
  export CAF_CONFIG_DECODER=JavascriptDecoder
  export CAF_CONFIG_ENABLE_SUBSTITUTOR=false
fi

#Launch the policyworker
echo "CAF_WORKER_JAVA_OPTS " $CAF_WORKER_JAVA_OPTS
exec java $CAF_WORKER_JAVA_OPTS -server -cp "/opt/PolicyWorker/*:/opt/PolicyWorker/lib/*:/opt/PolicyWorker/Handlers/*:/mnt/mesos/sandbox/*:/mnt/mesos/sandbox/handlers/*:/mnt/mesos/sandbox/converters/*:" com.hpe.caf.worker.core.WorkerApplication server ${logFile} |& sed -ue 's/^/Policy Worker: /' &
echo "ending setup"
