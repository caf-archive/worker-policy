/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.github.cafdataprocessing.worker.policy.version.tagging;

/**
 * Holds information about a Worker involved in Processing.
 */
public final class WorkerProcessingInfo {
    /**
     * A worker version.
     */
    private final String workerVersion;
    /**
     * A classifier for a worker.
     */
    private final String workerClassifier;

    /**
     * Holds information about a Worker involved in Processing.
     * @param workerVersion The version of a worker.
     * @param workerClassifier The classifier for a worker.
     */
    public WorkerProcessingInfo(String workerVersion, String workerClassifier){
        this.workerVersion = workerVersion;
        this.workerClassifier = workerClassifier;
    }

    /**
     * Returns the Worker Version.
     * @return
     */
    public String getWorkerVersion(){
        return workerVersion;
    }

    /**
     * Returns the Worker Classifier.
     * @return
     */
    public String getWorkerClassifier(){
        return workerClassifier;
    }
}
