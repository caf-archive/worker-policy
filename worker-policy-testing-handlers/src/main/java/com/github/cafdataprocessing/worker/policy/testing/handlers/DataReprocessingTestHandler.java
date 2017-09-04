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
package com.github.cafdataprocessing.worker.policy.testing.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import com.github.cafdataprocessing.entity.fields.DocumentProcessingRecord;
import com.github.cafdataprocessing.worker.policy.WorkerTaskResponsePolicyHandler;
import com.github.cafdataprocessing.worker.policy.converters.DocumentConverter;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.builder.PolicyReprocessingDocumentBuilder;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.builder.exceptions.PolicyReprocessingReconstructMessageException;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.github.cafdataprocessing.worker.policy.handlers.shared.document.SharedDocument;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskStatus;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Testing handler for document reconstruction
 */
public class DataReprocessingTestHandler extends WorkerTaskResponsePolicyHandler {


    @Override
    protected WorkerHandlerResponse handleTaskPolicy(Document document, Policy policy, Long aLong, TaskData taskData) throws InvalidTaskException {

        DataReproTask task = new DataReproTask();

        com.github.cafdataprocessing.worker.policy.shared.Document currentPolicyWorkerDoc = taskData.getDocument();

        com.github.cafdataprocessing.worker.policy.shared.Document finalPolicyWorkerOutput = new DocumentConverter().convert(document);
        if (currentPolicyWorkerDoc != null){
            //need to record processing record on this new Policy Worker Shared Document
            DocumentProcessingRecord taskDataProcessingRecord = currentPolicyWorkerDoc.getPolicyDataProcessingRecord();
            if(taskDataProcessingRecord!=null) {
                DocumentProcessingRecord newProcessingRecord = finalPolicyWorkerOutput.createPolicyDataProcessingRecord();
                newProcessingRecord.metadataChanges = taskDataProcessingRecord.metadataChanges;
                newProcessingRecord.referenceChange = taskDataProcessingRecord.referenceChange;
            }
        }

        //Forces new object to avoid reconstituting the workflow output
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new GuavaModule());
        byte[] documentBytes = null;

        try {
            documentBytes = objectMapper.writeValueAsBytes(finalPolicyWorkerOutput);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        com.github.cafdataprocessing.worker.policy.shared.Document documentToReconstitute =
                new com.github.cafdataprocessing.worker.policy.shared.Document();
        try {
            documentToReconstitute = objectMapper.readValue(documentBytes,
                    com.github.cafdataprocessing.worker.policy.shared.Document.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            PolicyReprocessingDocumentBuilder.reconstituteOriginalDocument(documentToReconstitute);
        } catch (PolicyReprocessingReconstructMessageException e) {
            e.printStackTrace();
        }

        task.reconstitutedDocument = documentToReconstitute;

        task.workflowDocument = currentPolicyWorkerDoc;
        task.sharedDocument = new SharedDocumentConverter().convert(finalPolicyWorkerOutput);

        DataReproPolicyDefinition policyDefinition = new ObjectMapper().convertValue(policy.details, DataReproPolicyDefinition.class);

        return new WorkerHandlerResponse(policyDefinition.queueName, TaskStatus.NEW_TASK, task, "Test",1);
    }

    @Override
    public PolicyType getPolicyType() {
        PolicyType policyType = new PolicyType();
        policyType.name = "Data Reprocessing Test PolicyType";
        policyType.shortName = "DataReproPolicyType";
        try {
            policyType.definition = new ObjectMapper().readTree(this.getClass().getResource("/data-repro-policy-definition.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return policyType;
    }

    public Collection<Policy> resolve(Document document, Collection<Policy> collection) {
        return null;
    }

    /**
     * Duplication of code here as there is already a converter in GenericQueueHandler but this avoids
     * PolicyWorkerTestingHandlers having a dependency on Handlers project.
     */
    class SharedDocumentConverter{
        public SharedDocument convert(com.github.cafdataprocessing.worker.policy.shared.Document policyDocument) {
            SharedDocument newDocument = new SharedDocument();

            newDocument.setReference(policyDocument.getReference());
            if (policyDocument.getMetadata() != null) {
                newDocument.getMetadata().addAll(policyDocument.getMetadata().entries());
            }
            if (policyDocument.getMetadataReferences() != null) {
                newDocument.getMetadataReference().addAll(policyDocument.getMetadataReferences().entries());
            }
            if (policyDocument.getDocuments() != null) {
                newDocument.getChildDocuments().addAll(policyDocument.getDocuments().stream().map(this::convert).collect(Collectors.toList()));
            }
            newDocument.setDocumentProcessingRecord(policyDocument.getPolicyDataProcessingRecord());

            return newDocument;
        }
    }
}
