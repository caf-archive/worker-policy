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

import com.google.common.base.Strings;
import com.github.cafdataprocessing.worker.policy.handlers.shared.HandlerProperties;
import com.github.cafdataprocessing.worker.policy.handlers.shared.PolicyQueueDefinition;

import java.util.Objects;

/**
 * Extension of WorkerPolicyHandler adding common worker property methods.
 */
public abstract class WorkerPolicyQueueInfoHandler extends WorkerPolicyHandler {

    public String getQueueName(PolicyQueueDefinition policyQueueDefinition, HandlerProperties properties) {
        return hasFailureHappened() ?
                getDiagnosticsQueue(policyQueueDefinition, properties) :
                getTaskQueue(policyQueueDefinition, properties);
    }

    private String getTaskQueue(PolicyQueueDefinition policyQueueDefinition, HandlerProperties properties) {
        return !Strings.isNullOrEmpty(policyQueueDefinition.queueName) ? policyQueueDefinition.queueName : properties.getTaskQueueName();
    }

    private String getDiagnosticsQueue(PolicyQueueDefinition policyQueueDefinition, HandlerProperties properties) {
        return !Strings.isNullOrEmpty(policyQueueDefinition.diagnosticsQueueName) ? policyQueueDefinition.diagnosticsQueueName : properties.getDiagnosticsQueueName();
    }

    public boolean hasFailureHappened() {
        WorkerRequestHolder workerRequestHolder = this.applicationContext.getBean(WorkerRequestHolder.class);
        if (Objects.isNull(workerRequestHolder)) {
            return false;
        }
        if (Objects.isNull(workerRequestHolder.getWorkerRequestInfo())) {
            return false;
        }
        return workerRequestHolder.getWorkerRequestInfo().useDiagnostics;
    }
}
