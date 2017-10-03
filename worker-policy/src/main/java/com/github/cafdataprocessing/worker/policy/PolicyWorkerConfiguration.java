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
package com.github.cafdataprocessing.worker.policy;

import com.github.cafdataprocessing.worker.policy.shared.PolicyWorkerConstants;
import com.hpe.caf.api.worker.WorkerConfiguration;

/**
 * Represents configuration read in for Policy Worker
 */
public class PolicyWorkerConfiguration extends WorkerConfiguration {
    /**
     * Whether policy handlers should be registered and updated.
     */
    private Boolean registerHandlers = true;

    //the name of the queue to output results to
    private String resultQueue;

    private String workerIdentifier;

    private int workerThreads = 1;

    //no-argument constructor required to allow for deserialization
    public PolicyWorkerConfiguration(){}

    /**
     * Get configuration property indicating whether attempt to register or update policy handlers should occur.
     * @return Indication of if registration/update of handlers should occur.
     */
    public boolean getRegisterHandlers(){
        return registerHandlers == null ? true : registerHandlers;
    }

    /**
     * Set configuration property indicating whether attempt to register or update policy handlers should occur.
     */
    public void setRegisterHandlers(Boolean registerHandlers){
        this.registerHandlers = registerHandlers;
    }

    public String getResultQueue() {
        return resultQueue;
    }

    public void setResultQueue(String resultQueue) {
        this.resultQueue = resultQueue;
    }

    public String getWorkerIdentifier(){
        return workerIdentifier == null ? PolicyWorkerConstants.WORKER_NAME : workerIdentifier;
    }

    public void setWorkerIdentifier(String workerIdentifier) {
        this.workerIdentifier = workerIdentifier;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads)
    {
        this.workerThreads = workerThreads;
    }
}
