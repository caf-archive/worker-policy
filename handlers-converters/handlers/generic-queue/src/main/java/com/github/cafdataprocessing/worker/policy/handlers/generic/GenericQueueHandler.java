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
package com.github.cafdataprocessing.worker.policy.handlers.generic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.dto.ConflictResolutionMode;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import com.github.cafdataprocessing.entity.fields.DocumentProcessingRecord;
import com.github.cafdataprocessing.worker.policy.WorkerResponseHolder;
import com.github.cafdataprocessing.worker.policy.WorkerTaskResponsePolicyHandler;
import com.github.cafdataprocessing.worker.policy.converters.DocumentConverter;
import com.github.cafdataprocessing.worker.policy.handlers.shared.document.SharedDocument;
import com.github.cafdataprocessing.worker.policy.handlers.shared.document.converter.SharedDocumentConverter;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.hpe.caf.api.worker.TaskStatus;

import java.io.IOException;
import java.util.Collection;

/**
 * Returns a response that contains the document metadata, outputting it to a configured queue.
 */
public class GenericQueueHandler extends WorkerTaskResponsePolicyHandler {
    private final GenericQueueHandlerProperties properties;

    public GenericQueueHandler() {
        this.properties = loadWorkerHandlerProperties(GenericQueueHandlerProperties.class);
    }

    @Override
    protected WorkerTaskResponsePolicyHandler.WorkerHandlerResponse handleTaskPolicy(Document document, Policy policy, Long collectionSequenceID, TaskData currentTaskData) {
        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        com.github.cafdataprocessing.worker.policy.shared.Document taskDataDocumentToClassify = workerResponseHolder.getTaskData().getDocument();

        com.github.cafdataprocessing.worker.policy.shared.Document documentToClassify = new DocumentConverter().convert(document);
        if (taskDataDocumentToClassify != null){
            //need to record processing record on this new Policy Worker Shared Document
            DocumentProcessingRecord taskDataProcessingRecord = taskDataDocumentToClassify.getPolicyDataProcessingRecord();
            if(taskDataProcessingRecord!=null) {
                DocumentProcessingRecord newProcessingRecord = documentToClassify.createPolicyDataProcessingRecord();
                newProcessingRecord.metadataChanges = taskDataProcessingRecord.metadataChanges;
                newProcessingRecord.referenceChange = taskDataProcessingRecord.referenceChange;
            }
        }

        SharedDocumentConverter sharedDocumentConverter = new SharedDocumentConverter();
        SharedDocument sharedDocument = sharedDocumentConverter.convert(documentToClassify);

        ObjectMapper objectMapper = new ObjectMapper();
        GenericQueuePolicyDefinition policyDefinition = objectMapper.convertValue(policy.details, GenericQueuePolicyDefinition.class);

        //  Get the appropriate queue name based on whether a failure has happened or not.
        String queueName = getQueueName(policyDefinition, properties);
        return new WorkerHandlerResponse(queueName, TaskStatus.NEW_TASK, sharedDocument, policyDefinition.messageType, policyDefinition.apiVersion);
    }

    @Override
    public PolicyType getPolicyType() {

        PolicyType policyType = new PolicyType();
        policyType.conflictResolutionMode = ConflictResolutionMode.PRIORITY;
        ObjectMapper m = new ObjectMapper();
        JsonNode definition = null;
        try {
            definition = m.readTree(this.getClass().getResource("/generic-queue-policy-definition.json"));
        } catch (IOException e) {
            logger.error("Could not deserialize TextExtractPolicyType definition", e);
        }
        policyType.description = "Used to create policies which send a document to a queue";
        policyType.definition = definition;
        policyType.name = "Generic Queue Policy Type";
        policyType.shortName = "GenericQueueHandler";
        return policyType;

    }

    public Collection<Policy> resolve(Document document, Collection<Policy> policies) {
        return null;
    }
}

