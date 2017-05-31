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
package com.github.cafdataprocessing.worker.policy;

import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.hpe.caf.api.worker.WorkerResponse;

/**
 * Holds details of the response from the policy worker to a task.
 */

public class WorkerResponseHolder {
    private WorkerResponse workerResponse;
    private TaskData taskData;
    private WorkerTaskResponsePolicyHandler.WorkerHandlerResponse chainWorkerResponse;
    private boolean shouldStop = false;

    public WorkerResponse getWorkerResponse() {
        return workerResponse;
    }

    public void setWorkerResponse(WorkerResponse workerResponse) {
        this.workerResponse = workerResponse;
        shouldStop = true;
    }

    public WorkerTaskResponsePolicyHandler.WorkerHandlerResponse getChainWorkerResponse() {
        return chainWorkerResponse;
    }

    public void setChainWorkerResponse( WorkerTaskResponsePolicyHandler.WorkerHandlerResponse value ) {
        chainWorkerResponse = value;
        shouldStop = true;
    }

    public TaskData getTaskData() {
        return taskData;
    }

    public void setTaskData(TaskData taskData) {
        this.taskData = taskData;
    }

    public void clear(){
        this.workerResponse = null;
        this.chainWorkerResponse = null;
        shouldStop = false;
    }

    public boolean isShouldStop() {
        return shouldStop;
    }

    public void setShouldStop(boolean shouldStop) {
        this.shouldStop = shouldStop;
    }
}
