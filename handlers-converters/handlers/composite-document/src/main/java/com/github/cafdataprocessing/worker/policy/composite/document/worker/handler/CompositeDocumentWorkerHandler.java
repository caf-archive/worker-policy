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
package com.github.cafdataprocessing.worker.policy.composite.document.worker.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.dto.ConflictResolutionMode;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import com.github.cafdataprocessing.worker.policy.WorkerResponseHolder;
import com.github.cafdataprocessing.worker.policy.WorkerTaskResponsePolicyHandler;
import com.github.cafdataprocessing.worker.policy.common.DataStoreAwareInputStream;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.google.common.base.Strings;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.document.DocumentWorkerConstants;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerDocumentTask;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeDocumentWorkerHandler extends WorkerTaskResponsePolicyHandler
{

    // Logger for logging purposes
    private static final Logger LOG = LoggerFactory.getLogger(CompositeDocumentWorkerHandler.class);

    @Override
    protected WorkerHandlerResponse handleTaskPolicy(final Document document, final Policy policy, final Long aLong, final TaskData taskData)
        throws InvalidTaskException
    {
        final CompositeDocumentWorkerHandlerProperties properties = loadWorkerHandlerProperties(CompositeDocumentWorkerHandlerProperties.class);
        final DocumentWorkerDocumentTask task = new DocumentWorkerDocumentTask();
        task.document = new DocumentWorkerDocument();
        task.document.reference = document.getReference();
        final ObjectMapper mapper = new ObjectMapper();

        LOG.info("Retrieving policy definition...");
        CompositeDocumentPolicyDefinition policyDef = mapper.convertValue(policy.details, CompositeDocumentPolicyDefinition.class);

        final Map<String, String> targetMap = new HashMap<>();

        properties.setWorkerName(policyDef.workerName);

        // Loop through policyDef.customData
        policyDef.customData.entrySet().stream().forEach(customDataEntry -> {
            final String key = customDataEntry.getKey();
            final Object value = customDataEntry.getValue();

            final String parsedValue = parseCustomDataValue(value, taskData, mapper);

            /**
             * If the value passed into parseCustomDataValue is not a supported datatype or is incorrect, then null will be returned. This
             * ensures that incomplete key-value pairs are not added to customData.
             */
            if (parsedValue != null) {
                targetMap.put(key, parsedValue);
            }
        });

        // Add the results of targetMap into customData
        if (!targetMap.isEmpty()) {
            task.customData = targetMap;
        }

        //  Gather fields for Document Worker task.
        task.document.fields = getFieldsData(document);

        //  Gather sub-files for document worker document to be provided on task.
        copySubFiles(document.getDocuments(), task.document);

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
            DocumentWorkerConstants.DOCUMENT_TASK_NAME,
            DocumentWorkerConstants.DOCUMENT_TASK_API_VER);
    }

    @Override
    public PolicyType getPolicyType()
    {
        final PolicyType policyType = new PolicyType();
        policyType.conflictResolutionMode = ConflictResolutionMode.PRIORITY;
        final ObjectMapper m = new ObjectMapper();
        JsonNode definition = null;
        try {
            definition = m.readTree(this.getClass().getResource("/composite-document-policy-definition.json"));
        } catch (IOException e) {
            logger.error("Could not deserialize DocumentPolicyType definition", e);
        }
        policyType.definition = definition;
        policyType.description = "Enriches a family of documents";
        policyType.name = "Composite Document Policy Type";
        policyType.shortName = "CompositeDocumentWorkerHandler";
        return policyType;
    }

    @Override
    public Collection<Policy> resolve(final Document document, final Collection<Policy> collection)
    {
        return null;
    }

    private Map<String, List<DocumentWorkerFieldValue>> getFieldsData(final Document document)
    {
        final Map<String, List<DocumentWorkerFieldValue>> fieldsMap = new HashMap<>();

        final WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        final com.github.cafdataprocessing.worker.policy.shared.Document taskDataDocumentToClassify = workerResponseHolder.getTaskData().getDocument();

        /**
         * Add metadata fields to Document Worker Task fields. This method is looping through a Metadata map of String to String, and a
         * Metadata map of String to ReferenceData which is used for binary data and storage references.
         */
        document.getMetadata().entries().stream()
            .forEach(metadata -> addToWorkerTaskFields(fieldsMap, metadata.getKey(), createWorkerData(metadata.getValue())));

        //copy the ReferenceData of any DataStoreAwareInputStreams as metadata references on new document
        if(document.getStreams() != null) {
            for (Map.Entry<String, InputStream> streamEntry : document.getStreams().entries()){
                if(streamEntry.getValue() == null || !(streamEntry.getValue() instanceof DataStoreAwareInputStream)){
                    continue;
                }
                DataStoreAwareInputStream asDataStoreStreamEntry = (DataStoreAwareInputStream) streamEntry.getValue();
                addToWorkerTaskFields(fieldsMap, streamEntry.getKey(),
                        createWorkerData(asDataStoreStreamEntry.getReferencedData()));
            }
        }

        return fieldsMap;
    }

    private static void addToWorkerTaskFields(final Map<String, List<DocumentWorkerFieldValue>> fieldsMap, final String name,
                                              final DocumentWorkerFieldValue workerData)
    {
        List<DocumentWorkerFieldValue> data = fieldsMap.get(name);

        if (data == null) {
            data = new ArrayList<>();
            fieldsMap.put(name, data);
        }
        data.add(workerData);
    }

    private static DocumentWorkerFieldValue createWorkerData(final String value)
    {
        final DocumentWorkerFieldValue workerData = new DocumentWorkerFieldValue();
        workerData.data = value;
        return workerData;
    }

    private static DocumentWorkerFieldValue createWorkerData(final ReferencedData value)
    {
        final DocumentWorkerFieldValue workerData = new DocumentWorkerFieldValue();

        if (value.getData() != null) {
            workerData.data = Base64.getEncoder().encodeToString(value.getData());
            workerData.encoding = DocumentWorkerFieldEncoding.base64;
        } else if (value.getReference() != null) {
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

        return resolveSourceValue((String) sourceValue, (Map) value, taskData, mapper);
    }

    private static String resolveSourceValue(final String sourceValue, final Map value, final TaskData taskData, final ObjectMapper mapper)
    {
        // Call the appropriate function based on the source
        if ("dataStorePartialReference".equals(sourceValue)) {
            return taskData.getOutputPartialReference();
        }

        if ("inlineJson".equals(sourceValue)) {
            return encodeInlineJson(value, mapper);
        }

        if ("projectId".equals(sourceValue)) {
            return taskData.getProjectId();
        }

        if ("workflowId".equals(sourceValue)) {
            return taskData.getWorkflowId();
        }

        // The source value is not recognised
        LOG.warn("The customData object 'source' is not recognised. Skipping...");
        return null;
    }

    private static String encodeInlineJson(final Map value, final ObjectMapper mapper)
    {
        final Object data = value.get("data");

        // Encode the 'data' object as a JSON-escaped string.
        try {
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            LOG.warn("The customData inlineJson object 'data' could not be JSON-encoded. Skipping...");
            return null;
        }
    }

    private void copySubFiles(final Collection<Document> documents, final DocumentWorkerDocument document)
    {
        if (document.subdocuments == null) {
            document.subdocuments = new ArrayList();
        }
        for (Document doc : documents) {
            DocumentWorkerDocument subDoc = new DocumentWorkerDocument();
            subDoc.reference = doc.getReference();
            subDoc.fields = getFieldsData(doc);
            if (doc.getDocuments().size() > 0) {
                copySubFiles(doc.getDocuments(), subDoc);
            }
            document.subdocuments.add(subDoc);
        }
    }
}
