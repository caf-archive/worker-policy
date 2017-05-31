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

import com.github.cafdataprocessing.corepolicy.ProcessingAction;
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskStatus;

import java.util.Objects;

/**
 * Base class for Policy Handlers that send a task to an external worker.
 */
public abstract class WorkerTaskResponsePolicyHandler extends WorkerPolicyQueueInfoHandler {

    protected ProcessingAction handlePolicy(Document document, Policy policy, Long collectionSequenceID) throws InvalidTaskException {
        WorkerResponseHolder workerResponseHolder = this.applicationContext.getBean(WorkerResponseHolder.class);

        TaskData currentTaskData = workerResponseHolder.getTaskData();

        WorkerHandlerResponse workerHandlerResponse = handleTaskPolicy(document, policy, collectionSequenceID, currentTaskData);

        if (workerHandlerResponse == null) {
            return ProcessingAction.CONTINUE_PROCESSING;
        }

        // indicate to first stop processing.
        // but in addition, note that we have setup a chain from this handler, to prevent us creating our normal
        // response code executing.
        workerResponseHolder.setChainWorkerResponse(workerHandlerResponse);
        return ProcessingAction.STOP_PROCESSING;
    }

    protected abstract WorkerHandlerResponse handleTaskPolicy(Document document, Policy policy, Long collectionSequenceID, TaskData currentTaskData) throws InvalidTaskException;


    public class WorkerHandlerResponse {
        private final String queueReference;
        private final TaskStatus taskStatus;
        private final Object data;
        private final String messageType;
        private final int apiVersion;

        public WorkerHandlerResponse(String queue, TaskStatus status, Object data, String msgType, int version) {
            this.queueReference = Objects.requireNonNull(queue);
            this.taskStatus = Objects.requireNonNull(status);
            this.data = data;
            this.messageType = Objects.requireNonNull(msgType);
            this.apiVersion = version;
        }

        public TaskStatus getTaskStatus() {
            return this.taskStatus;
        }

        public String getQueueReference() {
            return this.queueReference;
        }

        public Object getData() {
            return this.data;
        }

        public String getMessageType() {
            return this.messageType;
        }

        public int getApiVersion() {
            return this.apiVersion;
        }
    }
}
