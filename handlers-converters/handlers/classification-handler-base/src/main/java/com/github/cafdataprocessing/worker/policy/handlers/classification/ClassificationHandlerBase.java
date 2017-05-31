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
package com.github.cafdataprocessing.worker.policy.handlers.classification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.UserContext;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.worker.policy.WorkerResponseHolder;
import com.github.cafdataprocessing.worker.policy.WorkerTaskResponsePolicyHandler;
import com.github.cafdataprocessing.worker.policy.converters.DocumentConverter;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.hpe.caf.api.worker.TaskStatus;
import com.github.cafdataprocessing.worker.policy.handlers.shared.HandlerProperties;

import java.util.Collection;
import java.util.Collections;

/**
 * Defines common logic to build a task for the Classification Worker that can be used with handler implementations.
 * Properties used in constructing task may be provided by implementations.
 */
public abstract class ClassificationHandlerBase extends WorkerTaskResponsePolicyHandler {
    @Override
    protected WorkerTaskResponsePolicyHandler.WorkerHandlerResponse handleTaskPolicy(Document document, Policy policy, Long collectionSequenceID, TaskData currentTaskData) {

        if(currentTaskData.getOutputPartialReference() == null){
            throw new RuntimeException("partialOutputReference was not set");
        }
        TaskData taskData = new TaskData();

        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        com.github.cafdataprocessing.worker.policy.shared.Document taskDataDocumentToClassify =
                workerResponseHolder.getTaskData().getDocument();

        taskData.setProjectId(applicationContext.getBean(UserContext.class).getProjectId());
        com.github.cafdataprocessing.worker.policy.shared.Document documentToClassify = new DocumentConverter().convert(document);
        documentToClassify.setMetadataReferences(taskDataDocumentToClassify.getMetadataReferences());
        taskData.setDocument(documentToClassify);
        taskData.setExecutePolicyOnClassifiedDocuments(false);
        taskData.setOutputPartialReference(currentTaskData.getOutputPartialReference());

        ObjectMapper objectMapper = new ObjectMapper();
        ClassificationPolicyDefinition policyDefinition = objectMapper.convertValue(policy.details, ClassificationPolicyDefinition.class);

        //if workflow ID is provided it will be used, otherwise use collection sequence ID
        Long workflowId = policyDefinition.workflowId;
        if(workflowId!=null){
            taskData.setWorkflowId(Long.toString(workflowId));
        }
        else{
            Long classificationSequenceId = policyDefinition.classificationSequenceId;
            if(classificationSequenceId==null){
                throw new IllegalArgumentException(String.format("Policy definition provided on Policy ID {0} does not " +
                        "have workflowId or collectionSequenceId set.", policy.id));
            }
            taskData.setCollectionSequence(Collections.singletonList(Long.toString(classificationSequenceId)));
        }

        // get the appropriate queue name based on whether a failure has happened or not.
        String queueName = getQueueName(policyDefinition, getProperties());

        // If a failure happened and the queue is not specified return null back to the engine for normal resuming
        if ( Strings.isNullOrEmpty(queueName) && hasFailureHappened() ) {
            return null;
        }

        return new WorkerHandlerResponse(
                queueName,
                TaskStatus.NEW_TASK, taskData, "PolicyWorker", 1);
    }

    public Collection<Policy> resolve(Document document, Collection<Policy> collection) {
        return null;
    }

    /**
     * Should be overridden by implementing class and define the properties for the handler.
     * @return
     */
    protected abstract HandlerProperties getProperties();
}
