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
package com.github.cafdataprocessing.worker.policy.handlers.markup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.dto.ConflictResolutionMode;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import com.github.cafdataprocessing.worker.policy.WorkerResponseHolder;
import com.github.cafdataprocessing.worker.policy.WorkerTaskResponsePolicyHandler;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.google.common.base.Strings;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.markup.MarkupWorkerConstants;
import com.hpe.caf.worker.markup.MarkupWorkerTask;

import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class MarkupWorkerHandler extends WorkerTaskResponsePolicyHandler
{
    private final MarkupWorkerHandlerProperties properties;

    public MarkupWorkerHandler()
    {
        this.properties = loadWorkerHandlerProperties(MarkupWorkerHandlerProperties.class);
    }

    @Override
    protected WorkerHandlerResponse handleTaskPolicy(Document document, Policy policy, Long collectionSequenceID,
                                                     TaskData taskData)
    {
        MarkupWorkerTask task = new MarkupWorkerTask();
        ObjectMapper mapper = new ObjectMapper();

        MarkupPolicyDefinition policyDef = mapper.convertValue(policy.details, MarkupPolicyDefinition.class);

        //  Gather source data fields for Markup task.
        Multimap<String, ReferencedData> sourceData = getFieldsData(policyDef.fields, document);
        task.sourceData = sourceData;
        task.hashConfiguration = policyDef.hashConfiguration;
        task.outputFields = policyDef.outputFields;
        task.isEmail = policyDef.isEmail;

        //  Get the appropriate queue name based on whether a failure has happened or not.
        String queueName = getQueueName(policyDef, properties);

        // If a failure happened and the queue is not specified return null back to the engine for normal resuming
        if (Strings.isNullOrEmpty(queueName) && hasFailureHappened()) {
            return null;
        }

        return new WorkerHandlerResponse(
            queueName,
            TaskStatus.NEW_TASK,
            task,
            MarkupWorkerConstants.WORKER_NAME,
            MarkupWorkerConstants.WORKER_API_VER);
    }

    @Override
    public PolicyType getPolicyType()
    {
        PolicyType policyType = new PolicyType();
        policyType.conflictResolutionMode = ConflictResolutionMode.PRIORITY;
        ObjectMapper m = new ObjectMapper();
        JsonNode definition = null;
        try {
            definition = m.readTree(this.getClass().getResource("/markup-policy-definition.json"));
        } catch (IOException e) {
            logger.error("Could not deserialize MarkupPolicyType definition", e);
        }
        policyType.definition = definition;
        policyType.description = "Segments and marks up email message content.";
        policyType.name = "Markup Policy Type";
        policyType.shortName = "MarkupPolicyType";
        return policyType;
    }

    @Override
    public Collection<Policy> resolve(Document document, Collection<Policy> collection)
    {
        return null;
    }

    private Multimap<String, ReferencedData> getFieldsData(Set<String> policyDefFields, Document document)
    {
        Multimap<String, ReferencedData> sourceData = ArrayListMultimap.create();
        //  Add metadata fields to source data (filter to list provided on policy definition if required)
        if (policyDefFields == null || policyDefFields.isEmpty()) {
            document.getMetadata().entries().stream().forEach((metadata) -> {
                sourceData.put(metadata.getKey(), ReferencedData.getWrappedData(metadata.getValue().getBytes(StandardCharsets.UTF_8)));
            });
        } else {
            for (final String filterFieldName : policyDefFields) {
                // Get the predicate for metadata key matching from the filter field name
                final Predicate<String> doesFieldSpecMatch = getKeyToFilterFieldNameMatchPredicate(filterFieldName);
                for (Map.Entry<String, String> metadata : document.getMetadata().entries()) {
                    // If the filter field name matches with the metadata key add the metadata entry to the sourceData
                    if (doesFieldSpecMatch.test(metadata.getKey())) {
                        sourceData.put(metadata.getKey(), ReferencedData.getWrappedData(metadata.getValue().getBytes(StandardCharsets.UTF_8)));
                    }
                }
            }
        }
        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        com.github.cafdataprocessing.worker.policy.shared.Document taskDataDocumentToClassify = workerResponseHolder.getTaskData().getDocument();
        if (taskDataDocumentToClassify != null) {
            if (policyDefFields == null || policyDefFields.isEmpty()) {
                taskDataDocumentToClassify.getMetadataReferences().entries().stream().forEach((metadataReference) -> {
                    sourceData.put(metadataReference.getKey(), metadataReference.getValue());
                });
            } else {
            for (final String filterFieldName : policyDefFields) {
                // Get the predicate for metadata key matching from the filter field name
                final Predicate<String> doesFieldSpecMatch = getKeyToFilterFieldNameMatchPredicate(filterFieldName);
                    //  Add metadata reference fields to source data.
                    for (Map.Entry<String, ReferencedData> metadataReference : taskDataDocumentToClassify.getMetadataReferences().entries()) {
                        if (doesFieldSpecMatch.test(metadataReference.getKey())) {
                            sourceData.put(metadataReference.getKey(), metadataReference.getValue());
                        }
                    }
                }
            }
        }
        return sourceData;
    }

    private Predicate<String> getKeyToFilterFieldNameMatchPredicate(String filterFieldName) {
        // If the filter field name contains an asterisk for wildcard matching transform it for regex matching and
        // return the predicate for matching a key against it.
        // Else return the predicate for matching a key against the filter field name.
        if (filterFieldName.contains("*")) {
            String[] splitFieldFieldName = filterFieldName.split("\\*", -1);
            for (int i = 0; i < splitFieldFieldName.length; i++) {
                splitFieldFieldName[i] = Pattern.quote(splitFieldFieldName[i].toUpperCase());
            }
            final String finalRegEx = String.join(".*", splitFieldFieldName);
            return key -> key.toUpperCase().matches(finalRegEx);
        } else {
            return key -> key.equalsIgnoreCase(filterFieldName);
        }
    }
}
