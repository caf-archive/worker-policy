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
package com.github.cafdataprocessing.worker.policy.testing.shared;

import com.github.cafdataprocessing.corepolicy.common.ClassificationApi;
import com.github.cafdataprocessing.corepolicy.common.PolicyApi;
import com.github.cafdataprocessing.corepolicy.common.WorkflowApi;
import com.github.cafdataprocessing.corepolicy.common.dto.*;
import com.github.cafdataprocessing.corepolicy.common.dto.conditions.*;
import com.github.cafdataprocessing.corepolicy.common.shared.CorePolicyObjectMapper;
import com.github.cafdataprocessing.corepolicy.domainModels.FieldAction;
import com.github.cafdataprocessing.corepolicy.policy.MetadataPolicy.MetadataPolicy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

/**
 * Convenience methods for writing tests using Policy
 */
public class PolicyTestHelper {
    protected static final CorePolicyObjectMapper mapper = new CorePolicyObjectMapper();

    public static Policy createGenericQueuePolicy(PolicyApi policyApi, String queueOutput) throws IOException {
        return createPolicy(policyApi,"GenericQueueHandler", "Generic Queue Policy", "{\"queueName\":\"" + queueOutput + "\"}");
    }

    public static Policy createElasticSearchClassificationPolicy(PolicyApi policyApi, Long classificationSeqeuenceId) throws IOException {
        return createPolicy(policyApi, "ElasticSearchClassificationPolicyType", "Sent to Elastic Classification", "{\"classificationSequenceId\":" + classificationSeqeuenceId + "}");
    }

    public static Policy createElasticSearchClassificationWorkflowIdPolicy(PolicyApi policyApi, Long workflowId) throws IOException {
        return createPolicy(policyApi, "ElasticSearchClassificationPolicyType", "Sent to Elastic Classification", "{\"workflowId\":" + workflowId + "}");
    }

    public static Policy createPolicy(PolicyApi policyApi, String policyType, String policyNameBase, String policyDetails) throws IOException {
        Policy policy = new Policy();
        policy.name = getUniqueString(policyNameBase);
        policy.details = mapper.readTree(policyDetails);
        policy.typeId = policyApi.retrievePolicyTypeByName(policyType).id;
        policy = policyApi.create(policy);
        return policy;
    }

    public static CollectionSequenceEntry createCollectionSequenceEntry(Long externalCollection) {
        return createCollectionSequenceEntry(null, externalCollection);
    }

    public static CollectionSequenceEntry createCollectionSequenceEntry(Short order, Long externalCollection){
        CollectionSequenceEntry sequenceEntry = new CollectionSequenceEntry();
        sequenceEntry.collectionIds = new HashSet<>();
        if(order!=null) {
            sequenceEntry.order = order;
        }
        sequenceEntry.collectionIds.add(externalCollection);
        return sequenceEntry;
    }

    public static DocumentCollection createDocumentCollection(ClassificationApi classificationApi, String nameBasis, Condition condition,
                                                              Long policyId){
        return createDocumentCollection(classificationApi, nameBasis, condition, policyId, null);
    }

    public static DocumentCollection createDocumentCollection(ClassificationApi classificationApi, String nameBasis, Condition condition,
                                                              Long policyId, String description){
        DocumentCollection documentCollection = new DocumentCollection();
        documentCollection.name = getUniqueString(nameBasis);
        if(description!=null){
            documentCollection.description = description;
        }
        if(condition != null) {
            documentCollection.condition = condition;
        }
        documentCollection.policyIds = new HashSet<>();
        documentCollection.policyIds.add(policyId);
        documentCollection = classificationApi.create(documentCollection);
        return documentCollection;
    }

    public static ExistsCondition createExistsCondition(String field, String nameBasis){
        ExistsCondition condition = new ExistsCondition();
        condition.conditionType = ConditionType.EXISTS;
        condition.field = field;
        condition.name = getUniqueString(nameBasis);
        return condition;
    }

    public static StringCondition createStringCondition(String field, StringOperatorType operatorType, String value){
        StringCondition stringCondition = new StringCondition();
        stringCondition.value = value;
        stringCondition.operator = operatorType;
        stringCondition.field = field;
        stringCondition.conditionType = ConditionType.STRING;
        return stringCondition;
    }
    
    public static NotCondition createNotCondition(Condition notElementCondition, String nameBasis){
        NotCondition condition = new NotCondition();
        condition.conditionType = ConditionType.NOT;
        condition.condition = notElementCondition;
        condition.name = getUniqueString(nameBasis);
        return condition;
    }

    public static String getUniqueString(String strPrefix) {
        return strPrefix + UUID.randomUUID().toString();
    }

    public static Policy createMetadataPolicy(PolicyApi policyApi) throws IOException {
        MetadataPolicy metadataPolicy = new MetadataPolicy();
        metadataPolicy.setFieldActions(new ArrayList<>());
        FieldAction fieldAction = new FieldAction();
        fieldAction.setAction(FieldAction.Action.ADD_FIELD_VALUE);
        fieldAction.setFieldName("EXTERNAL_TEST");
        fieldAction.setFieldValue("1");
        metadataPolicy.getFieldActions().add(fieldAction);

        return createPolicy(policyApi, "MetadataPolicy", "Tag the document",
                mapper.writeValueAsString(metadataPolicy));
    }

    public static CollectionSequence createCollectionSequenceWithOneEntry(ClassificationApi classificationApi,
                                                                          Long collectionId, String name,
                                                                          String description)
    {
        CollectionSequence classificationSequence = new CollectionSequence();
        classificationSequence.name = name;
        classificationSequence.description = description;
        classificationSequence.collectionSequenceEntries = new ArrayList<>();

        CollectionSequenceEntry entry = PolicyTestHelper.createCollectionSequenceEntry(collectionId);

        classificationSequence.collectionSequenceEntries.add(entry);
        return classificationApi.create(classificationSequence);
    }

    public static SequenceWorkflow createWorkflowWithOneEntry(WorkflowApi workflowApi, Long sequenceId,
                                                              String name)
    {
        SequenceWorkflow externalWorkflow = new SequenceWorkflow();
        externalWorkflow.name = getUniqueString("External_workflow");

        SequenceWorkflowEntry externalWorkflowEntry = new SequenceWorkflowEntry();
        externalWorkflowEntry.collectionSequenceId = sequenceId;

        externalWorkflow.sequenceWorkflowEntries = new ArrayList<>();
        externalWorkflow.sequenceWorkflowEntries.add(externalWorkflowEntry);
        return workflowApi.create(externalWorkflow);
    }
}