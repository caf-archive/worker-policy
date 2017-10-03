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
package com.github.cafdataprocessing.worker.policy.testing.shared;

import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.codec.JsonCodec;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Helper methods for working with TaskMessage class during testing.
 */
public class TestTaskMessageHelper {
    public static JsonCodec codec = new JsonCodec();
    private static final int TASK_API_VERSION = 1;
    
    /**
     * Create a new task message to classify a document, given a set of collectionSequences identifiers.
     * @param taskId
     * @param taskClassifier
     * @param document
     * @param collectionSequenceIds
     * @param executePolicy
     * @param projectId
     * @param datastoreRef
     * @param to
     * @param trackingInfo
     * @return
     * @throws CodecException 
     */
    public static TaskMessage getClassifyTaskMessage(final String taskId, final String taskClassifier, final Document document, final List<String> collectionSequenceIds,
                                                     final Boolean executePolicy, final String projectId, final String datastoreRef, final String to, final TrackingInfo trackingInfo)
            throws CodecException {
        return getClassifyTaskMessage(taskId, taskClassifier, document, collectionSequenceIds, executePolicy, projectId, datastoreRef, to, trackingInfo, TaskStatus.NEW_TASK);
    }
    
    /**
     * Create a new task message to classify a document, given a set of collectionSequences identifiers.
     * @param taskId
     * @param taskClassifier
     * @param document
     * @param collectionSequenceIds
     * @param executePolicy
     * @param projectId
     * @param datastoreRef
     * @param to
     * @param trackingInfo
     * @param taskStatus
     * @return
     * @throws CodecException 
     */
    public static TaskMessage getClassifyTaskMessage(final String taskId, final String taskClassifier, final Document document, final List<String> collectionSequenceIds,
                                                     final Boolean executePolicy, final String projectId, final String datastoreRef, final String to, final TrackingInfo trackingInfo, 
                                                     final TaskStatus taskStatus)
            throws CodecException {
        TaskData taskData = TestTaskDataHelper.getClassifyTaskData(document, collectionSequenceIds, executePolicy, projectId, datastoreRef);
        return new TaskMessage(taskId, taskClassifier, TASK_API_VERSION, codec.serialise(taskData), taskStatus, new HashMap<>(), to, trackingInfo);
    }

    /**
     * Create a new task message to classify a document, given a Workflow identifier.
     * @param taskId
     * @param taskClassifier
     * @param document
     * @param workflowId
     * @param executePolicy
     * @param projectId
     * @param datastoreRef
     * @param to
     * @param trackingInfo
     * @return
     * @throws CodecException 
     */
    public static TaskMessage getClassifyTaskMessage(final String taskId, final String taskClassifier,
                                                     final Document document, final String workflowId,
                                                     final Boolean executePolicy, final String projectId,
                                                     final String datastoreRef, final String to,
                                                     final TrackingInfo trackingInfo)
            throws CodecException {
        return getClassifyTaskMessage(taskId, taskClassifier, document, workflowId, executePolicy,
                projectId, datastoreRef, to, trackingInfo, TaskStatus.NEW_TASK);
    }
    
    /**
     * Create a new task message to classify a document, given a Workflow identifier.
     * @param taskId
     * @param taskClassifier
     * @param document
     * @param workflowId
     * @param executePolicy
     * @param projectId
     * @param datastoreRef
     * @param to
     * @param trackingInfo
     * @param taskStatus
     * @return
     * @throws CodecException 
     */
    public static TaskMessage getClassifyTaskMessage(final String taskId, final String taskClassifier,
                                                     final Document document, final String workflowId,
                                                     final Boolean executePolicy, final String projectId,
                                                     final String datastoreRef, final String to,
                                                     final TrackingInfo trackingInfo,
                                                     final TaskStatus taskStatus )
            throws CodecException {
        TaskData taskData = TestTaskDataHelper.getClassifyTaskData(document, workflowId, executePolicy,
                projectId,datastoreRef);
        return new TaskMessage(taskId, taskClassifier, TASK_API_VERSION, codec.serialise(taskData),
                taskStatus, new HashMap<>(), to, trackingInfo);
    }

    /**
     * Create a new task message to execute policies directly on a document, given a set of policy identifiers and collection sequence identifiers.
     * @param taskId
     * @param taskClassifier
     * @param documentToExecutePolicyOn
     * @param policyIDs
     * @param projectId
     * @param collectionSequenceIds
     * @param to
     * @param trackingInfo
     * @return
     * @throws CodecException 
     */
     public static TaskMessage getExecuteTaskMessage(String taskId, String taskClassifier, Document documentToExecutePolicyOn, Collection<Long> policyIDs,
                                                    String projectId, List<String> collectionSequenceIds, final String to, final TrackingInfo trackingInfo) throws CodecException {
        TaskData taskData = TestTaskDataHelper.getExecuteTaskData(documentToExecutePolicyOn, policyIDs, projectId, collectionSequenceIds);
        return new TaskMessage(taskId, taskClassifier, TASK_API_VERSION, codec.serialise(taskData), TaskStatus.NEW_TASK, new HashMap<>(), to, trackingInfo);
    }
}
