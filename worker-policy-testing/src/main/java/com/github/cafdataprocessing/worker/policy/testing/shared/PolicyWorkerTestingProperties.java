/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cafdataprocessing.worker.policy.testing.shared;

import org.elasticsearch.common.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

/**
 * Properties specific to running policy worker tests.
 */
@Configuration
@PropertySource("classpath:policyworkertesting.properties")
@PropertySource(value = "file:${COREPOLICY_CONFIG}/policyworkertesting.properties", ignoreResourceNotFound = true)
public class PolicyWorkerTestingProperties {
    @Autowired
    private Environment environment;
    private final static Logger logger = LoggerFactory.getLogger(PolicyWorkerTestingProperties.class);

    public String getInputQueueName() {
        return environment.getProperty("rabbit.inputqueue");
    }

    public String getResultQueueName() {
        return environment.getProperty("rabbit.resultqueue");
    }

    /**
     * Returns the number of seconds to wait on a result. If property result.timeout.seconds is not specified defaults to 120.
     * @return Number of seconds to wait on result.
     */
    public int getResultTimeoutSeconds() {
        String timeOutStr = environment.getProperty("result.timeout.seconds");
        if(Strings.isNullOrEmpty(timeOutStr)){
            return 120;
        }
        return Integer.parseInt(timeOutStr);
    }

    public String getRabbitHost() {
        return environment.getProperty("rabbit.host");
    }

    public String getRabbitUser() {
        return environment.getProperty("rabbit.user");
    }

    public String getRabbitPass() {
        return environment.getProperty("rabbit.pass");
    }

    public int getRabbitPort() {
        return Integer.parseInt(environment.getProperty("rabbit.port"));
    }

    public String getWorkerHealthcheckAddress() {
        return environment.getProperty("worker.healthcheck.address");
    }

    public String getDataStorePartialReference(){
        return environment.getProperty("worker.datastore.partialreference");
    }

    public String getJobTrackingPipe(){
        // if we are really using job-tracking or wish to see the debug output of it tracking an item or marking it complete use a
        // real jobtracking worker.
        // in cases where not using job tracking worker and the property is not specified we fall back to null
        String jobTrackingQueue = environment.getProperty("rabbit.workerjobtrackingqueue");
        if(!Strings.isNullOrEmpty(jobTrackingQueue)){
            return jobTrackingQueue;
        }
        return null;
    }

    /**
     * Returns whether tests should be ran that use worker-policy-testing-handlers
     * @return
     */
    public Boolean getRunHandlerTests(){
        return Boolean.parseBoolean(environment.getProperty("testing.runhandlertests"));
    }
}
