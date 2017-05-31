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

import com.github.cafdataprocessing.worker.policy.converterinterface.*;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.worker.policy.shared.PolicyWorkerConstants;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.*;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.TrackedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterRuntime;
import com.github.cafdataprocessing.worker.policy.version.tagging.PolicyReprocessingVersionTagging;
import com.github.cafdataprocessing.worker.policy.version.tagging.PolicyReprocessingVersionTaggingException;
import com.github.cafdataprocessing.worker.policy.version.tagging.WorkerProcessingInfo;
import java.util.Optional;

/**
 * Handles converting a worker-policy task data from a task returned by a different type of worker.
 */
public class TaskDataConverter {
    private final Map<String, PolicyWorkerConverterInterface> workerInputConverters = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(TaskDataConverter.class);

    public TaskDataConverter() {
        ServiceLoader<PolicyWorkerConverterInterface> loader = ServiceLoader.load(PolicyWorkerConverterInterface.class);

        for (PolicyWorkerConverterInterface converter : loader) {
            Set<String> supportedClassifiers = getSupportedClassifierVersion(converter).keySet();
            for (String supportedClassifier : supportedClassifiers) {
                workerInputConverters.put(supportedClassifier, converter);
            }
        }
    }

    /**
     * Gets the supported classifiers
     *
     * @return Returns a map of the supported classifiers with the versions supported. Can not be null.
     */
    private static Multimap<String, Integer> getSupportedClassifierVersion(final PolicyWorkerConverterInterface converter) {
        Multimap<String, Integer> supportedMap = ArrayListMultimap.create();
        converter.updateSupportedClassifierVersions(supportedMap);
        return supportedMap;
    }

    /**
     * Converts a Task Message from a Worker to a Policy TaskData, updating the TaskData with information from the passed in
     * WorkerTaskData
     *
     * @param converter Converter to use
     * @param codec Codec to use when deserializing.
     * @param dataStore DataStore to use in retrieving or storing data.
     * @param workerTask Information about the Task that is to be converted.
     * @return A Policy Worker Task Data updated with any relevant information added from the Task that was converted.
     * @throws CodecException
     * @throws InvalidTaskException
     */
    public static TaskData convert(PolicyWorkerConverterInterface converter, Codec codec, DataStore dataStore, WorkerTaskData workerTask)
        throws CodecException, InvalidTaskException
    {
        // Get the initial task data from the context (or the converter)
        final TaskData taskData = getTaskData(converter, codec, dataStore, workerTask);

        // Throw an error if the task data could not br retrieved
        if (taskData == null) {
            throw new InvalidTaskException("Context not present, could not be deserialised, or could not be replaced");
        }

        // Verify the document obtained from the taskData is not null.
        final Document document = taskData.getDocument();
        if (document == null) {
            throw new InvalidTaskException("Context did not contain a document");
        }

        // Create a tracked document so changes are recorded on the processing record of the document
        final TrackedDocument trackedDocument = new TrackedDocument(document);

        // Create the services object to be passed to the converter
        final PolicyWorkerConverterRuntime converterRuntime
            = new PolicyWorkerConverterRuntimeImpl(trackedDocument, codec, dataStore, workerTask);

        try {
            final TaskStatus taskStatus = workerTask.getStatus();
            
            if (null != taskStatus) {
                switch (taskStatus) {
                    case NEW_TASK:
                    case RESULT_SUCCESS:
                        converter.convert(converterRuntime);

                        final Optional<String> policyWorkerFailureErrorCodeMetadataField
                            = trackedDocument.getMetadata().get(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_CODE).stream().findFirst();

                        // In the instance of a task status success AND a policyworker_failure_error_code field exists on the document,
                        // add policy_worker_failure field for diagnostics processing of the message.
                        if (policyWorkerFailureErrorCodeMetadataField.isPresent()) {
                            addPolicyWorkerFailureFieldToDocument(
                                trackedDocument,
                                getWorkerName(workerTask),
                                null);
                        }

                        break;

                    case RESULT_FAILURE:
                        logger.warn("Handling RESULT_FAILURE" + taskStatus);
                        converter.convert(converterRuntime);

                        addPolicyWorkerFailureFieldToDocument(
                            trackedDocument,
                            getWorkerName(workerTask),
                            taskStatus.toString());

                        break;

                    default:
                        logger.error("Handling INVALID_TASK or RESULT_EXCEPTION" + taskStatus);
                        assert (taskStatus == TaskStatus.INVALID_TASK || taskStatus == TaskStatus.RESULT_EXCEPTION);

                        final String errorMessage = getDataAsString(workerTask.getData());

                        addPolicyWorkerFailureFieldToDocument(
                            trackedDocument,
                            getWorkerName(workerTask),
                            taskStatus.toString() + ": " + errorMessage);

                        break;
                }
            } else {
                addPolicyWorkerFailureFieldToDocument(
                    trackedDocument,
                    getWorkerName(workerTask),
                    "<status_missing>");
            }

            addProcessingWorkerInformation(trackedDocument, workerTask.getSourceInfo());
        } catch (PolicyWorkerConverterException pwce) {
            addPolicyWorkerFailureFieldToDocument(
                trackedDocument,
                getWorkerName(workerTask),
                pwce.toString());
        }

        return taskData;
    }

    private static TaskData getTaskData(
        final PolicyWorkerConverterInterface converter,
        final Codec codec,
        final DataStore dataStore,
        final WorkerTaskData workerTask
    )
        throws InvalidTaskException, CodecException
    {
        // Just deserialise the context if it is available
        final byte[] context = workerTask.getContext();

        if (context != null) {
            return codec.deserialise(context, TaskData.class);
        }

        // Check that the task status is available
        final TaskStatus taskStatus = workerTask.getStatus();

        if (null == taskStatus) {
            throw new InvalidTaskException("Task status not returned and no document to record error");
        }

        // We only want to call the converter for specified statuses
        switch (taskStatus) {
            case NEW_TASK:
            case RESULT_SUCCESS:
            case RESULT_FAILURE:
                // Base runtime used in sub document converter method, providing a codec to deserialize the response
                PolicyWorkerConverterRuntimeBase baseRuntime = new PolicyWorkerConverterRuntimeBaseImpl(codec, dataStore, workerTask);

                try {
                    return converter.convert(baseRuntime);
                } catch (PolicyWorkerConverterException pwce) {
                    throw new InvalidTaskException("Failed to retrieve taskdata", pwce);
                }
        }

        assert (taskStatus == TaskStatus.INVALID_TASK || taskStatus == TaskStatus.RESULT_EXCEPTION);

        final String errorMessage = getDataAsString(workerTask.getData());

        throw new InvalidTaskException("Invalid Task returned and no document to record error: " + errorMessage);
    }

    private static String getDataAsString(final byte[] data)
    {
        return (data == null)
            ? ""
            : new String(data, StandardCharsets.UTF_8);
    }

    private static void addPolicyWorkerFailureFieldToDocument(
        final TrackedDocument document,
        final String workerName,
        final String errorMessage
    )
    {
        logger.error("Adding failure to document metadata %s, %s", workerName, errorMessage);

        // Get the document metadata
        final Multimap<String, String> documentMetadata = document.getMetadata();

        // If there is a policy worker failure field present then remove it
        documentMetadata.removeAll(PolicyWorkerConstants.POLICYWORKER_FAILURE);

        // Add the new failure to the document
        final String value = (errorMessage == null)
            ? workerName
            : workerName + ": " + errorMessage;

        documentMetadata.put(PolicyWorkerConstants.POLICYWORKER_FAILURE, value);

        // Record the failure in a second (multi-value) field which will not get removed by subsequent logic
        documentMetadata.put(PolicyWorkerConstants.POLICYWORKER_ERROR, value);
    }

    /**
     * Updates the passed in Document metadata with details of the passed in TaskSourceInfo.
     *
     * @param document Document to update metadata on.
     * @param sourceInfo The TaskSourceInfo to use in updating the metadata.
     */
    public static void addProcessingWorkerInformation(DocumentInterface document, TaskSourceInfo sourceInfo)
    {
        if (sourceInfo == null) {
            logger.warn("No TaskSourceInfo was provided, unable to add Processing Worker Version to document.");
            return;
        }
        try {
            PolicyReprocessingVersionTagging.addProcessingWorkerVersion(
                document, new WorkerProcessingInfo(sourceInfo.getVersion(), sourceInfo.getName()));
        } catch (PolicyReprocessingVersionTaggingException e) {
            logger.warn("Failed to add Processing Worker Version to document.", e);
        }
    }

    /**
     * Obtain the name of the worker, that performed the operation.
     *
     * @param workerTaskData
     * @return
     */
    private static String getWorkerName(WorkerTaskData workerTaskData)
    {
        final TaskSourceInfo sourceInfo = workerTaskData.getSourceInfo();

        if (sourceInfo == null || Strings.isNullOrEmpty(sourceInfo.getName())) {
            return "Unknown";
        }

        return sourceInfo.getName();
    }

    /**
     * Convenience method that builds up a String of information about the classifier, taskData and context passed
     * when a CodecException was thrown.
     * @param classifier The classifier that was passed to the method that threw the CodecException.
     * @param taskData The taskData that was passed to the method that threw the CodecException.
     * @param context The context that was passed to the method that threw the CodecException.
     * @return Constructed message with String representations of the parameters.
     */
    public static String getCodecExceptionVariablesInfoMessage(final String classifier, byte[] taskData, byte[] context){
        StringBuilder codecExceptionVariablesInfo = new StringBuilder();
        codecExceptionVariablesInfo.append("CodecException was thrown with;");
        codecExceptionVariablesInfo.append(System.lineSeparator());
        codecExceptionVariablesInfo.append(taskData != null ? "taskData: " +
                new String(taskData, StandardCharsets.UTF_8) : "taskData passed was null.");
        codecExceptionVariablesInfo.append(System.lineSeparator());
        codecExceptionVariablesInfo.append(context != null ? "context: " +
                new String(context, StandardCharsets.UTF_8) : "context passed was null.");
        codecExceptionVariablesInfo.append(System.lineSeparator());
        codecExceptionVariablesInfo.append(classifier != null ? "classifier: " + classifier :
                "classifier passed was null.");
        return codecExceptionVariablesInfo.toString();
    }

    public void updateObjectWithTaskData(String classifierToSendTo, int classifierVersion, Object object, TaskData taskData)
    {
        PolicyWorkerConverterInterface converter = null;
        try {
            converter = getConverter(classifierToSendTo, classifierVersion);
        } catch (Exception e) {
            logger.warn("Could not find converter with the name " + classifierToSendTo);
        }

        if (converter instanceof PolicyWorkerConverterRequestInterface) {
            ((PolicyWorkerConverterRequestInterface) converter).mergeTaskDataOntoInfoToBeSent(object, taskData);
        }
    }

    public PolicyWorkerConverterInterface getConverter(String classifier, int version) throws InvalidTaskException {
        PolicyWorkerConverterInterface converter = workerInputConverters.get(classifier);

        if (converter == null) {
            throw new InvalidTaskException("No WorkerInputConverter found for classifier with the name " + classifier);
        }
        if (!isSupported(converter, classifier, version)) {
            throw new InvalidTaskException("No WorkerInputConverter found for classifier " + classifier + " version " + version);
        }

        return converter;
    }

    /**
     * Check if a classifier name and version is supported
     */
    public static boolean isSupported(
        final PolicyWorkerConverterInterface converter,
        final String classifierName,
        final Integer classifierVersion
    )
    {
        if (converter == null || classifierName == null || classifierVersion == null) {
            throw new IllegalArgumentException("Object should not be null");
        }

        final Multimap<String, Integer> supportMap = getSupportedClassifierVersion(converter);
        if (supportMap == null) {
            return false;
        }

        return supportMap.containsEntry(classifierName, classifierVersion);
    }
}
