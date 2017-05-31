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
package com.github.cafdataprocessing.worker.policy.handlers.boilerplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.UserContext;
import com.github.cafdataprocessing.corepolicy.common.dto.ConflictResolutionMode;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import com.github.cafdataprocessing.worker.policy.WorkerResponseHolder;
import com.github.cafdataprocessing.worker.policy.WorkerTaskResponsePolicyHandler;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.policyworker.policyboilerplatefields.PolicyBoilerplateFields;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.boilerplateshared.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Contain logic on how to handle a Boilerplate Policy and its definition.
 */
public class BoilerplateWorkerHandler extends WorkerTaskResponsePolicyHandler {
    private final BoilerplateHandlerProperties properties;

    public BoilerplateWorkerHandler() {
        this.properties = loadWorkerHandlerProperties(BoilerplateHandlerProperties.class);
    }

    @Override
    protected WorkerHandlerResponse handleTaskPolicy(Document document, Policy policy, Long collectionSequenceID, TaskData currentTaskData) throws InvalidTaskException {
        if (currentTaskData.getOutputPartialReference() == null) {
            throw new InvalidTaskException("partialOutputReference was not set");
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new GuavaModule());
        BoilerplatePolicyDefinition policyDef = mapper.convertValue(policy.details, BoilerplatePolicyDefinition.class);
        Collection<String> fieldsToSend;
        BoilerplateWorkerTask task = new BoilerplateWorkerTask();
        task.setDataStorePartialReference(currentTaskData.getOutputPartialReference());
        task.setRedactionType(policyDef.redactionType);
        task.setTenantId(applicationContext.getBean(UserContext.class).getProjectId());
        task.setReturnMatches(policyDef.returnMatches);

        boolean useDefaultFields = policyDef.fields == null || policyDef.fields.size() == 0;
        fieldsToSend = useDefaultFields ? properties.getDefaultFields() : policyDef.fields;

        Multimap<String, ReferencedData> sourceData = getRequestedFieldsData(fieldsToSend, document);
        task.setSourceData(sourceData);
        SelectedItems selectedItems = null;
        if (policyDef.expressionIds != null && policyDef.expressionIds.size() > 0) {
            SelectedExpressions expression = new SelectedExpressions();
            expression.setExpressionIds(policyDef.expressionIds);
            selectedItems = expression;
        } else if (policyDef.tagId != null) {
            SelectedTag tag = new SelectedTag();
            tag.setTagId(policyDef.tagId);
            selectedItems = tag;
        } else if (policyDef.emailSegregationRules != null) {
            SelectedEmail selectedEmail = new SelectedEmail();
            selectedEmail.primaryContent = policyDef.emailSegregationRules.primaryExpression;
            selectedEmail.secondaryContent = policyDef.emailSegregationRules.secondaryExpression;
            selectedEmail.tertiaryContent = policyDef.emailSegregationRules.tertiaryExpression;
            if (policyDef.emailSegregationRules.primaryFieldName != null) {
                document.getMetadata().put(PolicyBoilerplateFields.POLICY_BOILERPLATE_PRIMARY_FIELDNAME, policyDef.emailSegregationRules.primaryFieldName);
            }
            if (policyDef.emailSegregationRules.secondaryFieldName != null) {
                document.getMetadata().put(PolicyBoilerplateFields.POLICY_BOILERPLATE_SECONDARY_FIELDNAME, policyDef.emailSegregationRules.secondaryFieldName);
            }
            if (policyDef.emailSegregationRules.tertiaryFieldName != null) {
                document.getMetadata().put(PolicyBoilerplateFields.POLICY_BOILERPLATE_TERTIARY_FIELDNAME, policyDef.emailSegregationRules.tertiaryFieldName);
            }
            selectedItems = selectedEmail;
        } else if (policyDef.emailSignatureDetection != null) {
            SelectedEmailSignature selectedEmailSignature = new SelectedEmailSignature();
            if (policyDef.emailSignatureDetection.extractedEmailSignaturesFieldName != null) {
                document.getMetadata().put(PolicyBoilerplateFields.POLICY_BOILERPLATE_EMAIL_SIGNATURES_FIELDNAME, policyDef.emailSignatureDetection.extractedEmailSignaturesFieldName);
            }
            if (policyDef.emailSignatureDetection.sender != null) {
                selectedEmailSignature.sender = policyDef.emailSignatureDetection.sender;
            } else {
                selectedEmailSignature.sender = "";
            }
            selectedItems = selectedEmailSignature;
        } else {
            logger.warn("No tagId, expressionIds, emailSegregationRules or emailSignatureDetection provided on Boilerplate Policy Definition for Policy with id " + policy.id);
        }
        task.setExpressions(selectedItems);

        // get the appropriate queue name based on whether a failure has happened or not.
        String queueName = getQueueName(policyDef, properties);

        // If a failure happened and the queue is not specified return null back to the engine for normal resuming
        if ( Strings.isNullOrEmpty(queueName) && hasFailureHappened() ) {
            return null;
        }

        return new WorkerTaskResponsePolicyHandler.WorkerHandlerResponse(
                queueName,
                TaskStatus.NEW_TASK, task,
                BoilerplateWorkerConstants.WORKER_NAME, BoilerplateWorkerConstants.WORKER_API_VERSION);
    }

    private Multimap<String, ReferencedData> getRequestedFieldsData(Collection<String> fieldsRequested, Document document) {
        //gather source data fields for Extract task
        Multimap<String, ReferencedData> sourceData = ArrayListMultimap.create();
        //add metadata fields to source data (filter to list provided on policy definition if required)
        for (Map.Entry<String, String> metadata : document.getMetadata().entries()) {
            String fieldName = metadata.getKey();
            if (fieldsRequested.contains(fieldName)) {
                sourceData.put(fieldName, ReferencedData.getWrappedData(metadata.getValue().getBytes()));
            }
        }

        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        com.github.cafdataprocessing.worker.policy.shared.Document taskDataDocumentToClassify = workerResponseHolder.getTaskData().getDocument();

        //add metadata reference fields to source data (filter to list provided on policy definition if required)
        for (Map.Entry<String, ReferencedData> metadataReference : taskDataDocumentToClassify.getMetadataReferences().entries()) {
            String fieldName = metadataReference.getKey();
            if (fieldsRequested.contains(fieldName)) {
                sourceData.put(metadataReference.getKey(), metadataReference.getValue());
            }
        }
        return sourceData;
    }

    @Override
    public PolicyType getPolicyType() {
        PolicyType policyType = new PolicyType();
        policyType.conflictResolutionMode = ConflictResolutionMode.PRIORITY;
        ObjectMapper m = new ObjectMapper();
        JsonNode definition = null;
        try {
            definition = m.readTree(BoilerplateWorkerHandler.class.getResource("/boilerplate-policy-definition.json"));
        } catch (IOException e) {
            logger.error("Could not deserialize BoilerplatePolicyDefinition definition", e);
        }
        policyType.definition = definition;
        policyType.description = "Detects and optionally removes or replaces boilerplate text on a document.";
        policyType.name = "Boilerplate Policy Type";
        policyType.shortName = "BoilerplatePolicyType";
        return policyType;
    }

    public Collection<Policy> resolve(Document document, Collection<Policy> policies) {
        return null;
    }
}
