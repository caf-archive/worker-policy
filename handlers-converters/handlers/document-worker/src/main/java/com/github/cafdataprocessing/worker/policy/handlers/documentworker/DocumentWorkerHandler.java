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
package com.github.cafdataprocessing.worker.policy.handlers.documentworker;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.document.DocumentWorkerConstants;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.document.DocumentWorkerTask;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentWorkerHandler extends WorkerTaskResponsePolicyHandler {

    // Logger for logging purposes
    private static final Logger LOG = LoggerFactory.getLogger(DocumentWorkerHandler.class);

    public DocumentWorkerHandler(){
    }

    @Override
    protected WorkerHandlerResponse handleTaskPolicy(Document document, Policy policy, Long aLong, TaskData taskData) throws InvalidTaskException {
        final DocumentWorkerHandlerProperties properties = loadWorkerHandlerProperties(DocumentWorkerHandlerProperties.class);
        DocumentWorkerTask task = new DocumentWorkerTask();
        ObjectMapper mapper = new ObjectMapper();

        LOG.info("Retrieving policy definition...");
        DocumentPolicyDefinition policyDef = mapper.convertValue(policy.details, DocumentPolicyDefinition.class);

        final Map<String, String> targetMap = new HashMap<>();

        properties.setWorkerName(policyDef.workerName);

        // Loop through policyDef.customData
        policyDef.customData.entrySet().stream().forEach(customDataEntry -> {
            final String key = customDataEntry.getKey();
            final Object value = customDataEntry.getValue();

            final String parsedValue = parseCustomDataValue(value, taskData, mapper);

            // If the value passed into parseCustomDataValue is not a supported datatype or is incorrect, then null will be returned.
            // This ensures that incomplete key-value pairs are not added to customData.
            if (parsedValue != null) {
                targetMap.put(key, parsedValue);
            }
        });

        // Add the results of targetMap into customData
        if (!targetMap.isEmpty()) {
            task.customData = targetMap;
        }

        //  Gather fields for Document Worker task.
        Map<String, List<DocumentWorkerFieldValue>> fields = getFieldsData(policyDef.fields, document);
        task.fields = fields;

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
                DocumentWorkerConstants.WORKER_NAME,
                DocumentWorkerConstants.WORKER_API_VER);
    }

    @Override
    public PolicyType getPolicyType()
    {
        PolicyType policyType = new PolicyType();
        policyType.conflictResolutionMode = ConflictResolutionMode.PRIORITY;
        ObjectMapper m = new ObjectMapper();
        JsonNode definition = null;
        try {
            definition = m.readTree(this.getClass().getResource("/document-policy-definition.json"));
        } catch (IOException e) {
            logger.error("Could not deserialize DocumentPolicyType definition", e);
        }
        policyType.definition = definition;
        policyType.description = "Enriches a document";
        policyType.name = "Document Policy Type";
        policyType.shortName = "DocumentWorkerHandler";
        return policyType;
    }

    @Override
    public Collection<Policy> resolve(Document document, Collection<Policy> collection) {
        return null;
    }

    private Map<String, List<DocumentWorkerFieldValue>> getFieldsData(Set<String> policyDefFields, Document document) {
        Map<String, List<DocumentWorkerFieldValue>> fieldsMap = new HashMap<>();

        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        com.github.cafdataprocessing.worker.policy.shared.Document taskDataDocumentToClassify = workerResponseHolder.getTaskData().getDocument();

        //  Add metadata fields to Document Worker Task fields. If 'fields' is specified in the policy definition then only add those fields to the 'fieldsMap' otherwise add all fields.
        // This method is looping through a Metadata map of String to String, and a Metadata map of String to ReferenceData which is used for binary data and storage references.
        if (policyDefFields == null || policyDefFields.isEmpty()) {
            document.getMetadata().entries().stream()
                    .forEach(metadata -> addToWorkerTaskFields(fieldsMap, metadata.getKey(), createWorkerData(metadata.getValue())));
            if (taskDataDocumentToClassify != null) {
                taskDataDocumentToClassify.getMetadataReferences().entries().stream()
                        .forEach(metadataReference -> addToWorkerTaskFields(fieldsMap, metadataReference.getKey(), createWorkerData(metadataReference.getValue())));
            }
            return fieldsMap;
        }

        for (String fieldName : policyDefFields) {
            // Get the predicate for metadata key matching from the filter field name
            final Predicate<String> doesFieldSpecMatch = getKeyToFilterFieldNameMatchPredicate(fieldName);
            // If the filter field name matches with the metadata key add the metadata entry to the worker task fields
            document.getMetadata().entries().stream()
                    .filter(metadata -> doesFieldSpecMatch.test(metadata.getKey()))
                    .forEach(metadata -> addToWorkerTaskFields(fieldsMap, metadata.getKey(), createWorkerData(metadata.getValue())));
            if (taskDataDocumentToClassify != null) {
                taskDataDocumentToClassify.getMetadataReferences().entries().stream()
                        .filter(metadataRef -> doesFieldSpecMatch.test(metadataRef.getKey()))
                        .forEach(metadataReference -> addToWorkerTaskFields(fieldsMap, metadataReference.getKey(), createWorkerData(metadataReference.getValue())));
            }
        }

        return fieldsMap;
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

    private static void addToWorkerTaskFields(Map<String, List<DocumentWorkerFieldValue>> fieldsMap, String name, DocumentWorkerFieldValue workerData){
        List<DocumentWorkerFieldValue> data = fieldsMap.get(name);

        if(data == null){
            data = new ArrayList<>();
            fieldsMap.put(name, data);
        }
        data.add(workerData);
    }

    private static DocumentWorkerFieldValue createWorkerData(String value){
        DocumentWorkerFieldValue workerData = new DocumentWorkerFieldValue();
        workerData.data = value;
        return workerData;
    }

    private static DocumentWorkerFieldValue createWorkerData(ReferencedData value){
        DocumentWorkerFieldValue workerData = new DocumentWorkerFieldValue();

        if(value.getData() != null){
            workerData.data = Base64.getEncoder().encodeToString(value.getData());
            workerData.encoding = DocumentWorkerFieldEncoding.base64;
        }
        else if (value.getReference() != null){
            workerData.data = value.getReference();
            workerData.encoding = DocumentWorkerFieldEncoding.storage_ref;
        } else {
            workerData.encoding = DocumentWorkerFieldEncoding.base64;
        }

        return workerData;
    }

    /**
     * Parses the value from an entry of the policy's customData map, and returns the value to be passed to the worker, or null if no
     * value should be passed.
     *
     * @param value the value from the customData map
     * @param taskData the PolicyWorker object which provides the context the handler is running in
     * @param mapper the object mapper to use for JSON value conversions
     * @return the value to be added to the customData to be sent to the worker, or null if the value should be ignored
     */
    private static String parseCustomDataValue(final Object value, final TaskData taskData, final ObjectMapper mapper)
    {
        // If value is null then it can be ignored
        if (value == null) {
            return null;
        }

        // If value is a string literal then it can be directly returned
        if (value instanceof String) {
            return (String) value;
        }

        // Apart from string literals, JSON objects (i.e. Maps) are the only other value type currently supported
        if (!(value instanceof Map)) {
            LOG.warn("The value in the customData map is not of datatype 'String' or 'Map'."
                + " Other datatypes within the values are not supported. Skipping...");
            return null;
        }

        // Get the source from the Map and check that it is a string
        final Object sourceValue = ((Map) value).get("source");

        if (!(sourceValue instanceof String)) {
            LOG.warn("The customData object 'source' is not specified or is not a string. Skipping...");
            return null;
        }

        // Call the appropriate function based on the source
        if (sourceValue.equals("dataStorePartialReference")) {
            return taskData.getOutputPartialReference();
        }

        if (sourceValue.equals("inlineJson")) {
            return encodeInlineJson((Map)value, mapper);
        }

        if (sourceValue.equals("projectId")) {
            return taskData.getProjectId();
        }

        if (sourceValue.equals("workflowId")) {
            return taskData.getWorkflowId();
        }

        // The source value is not recognised
        LOG.warn("The customData object 'source' is not recognised. Skipping...");
        return null;
    }

    private static String encodeInlineJson(final Map value, final ObjectMapper mapper)
    {
        // Get the 'data' object from the supplied value and check that it is a Map.
        final Object data = value.get("data");
        if (!(data instanceof Map)) {
            LOG.warn("The customData inlineJson object 'data' is not specified or is not a map. Skipping...");
            return null;
        }

        // Encode the 'data' object as a JSON-escaped string.
        try {
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            LOG.warn("The customData inlineJson object 'data' could not be JSON-encoded. Skipping...");
            return null;
        }
    }
}
