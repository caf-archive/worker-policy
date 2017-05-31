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

import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;

import java.util.Collection;
import java.util.List;

/**
 * Helper methods for working with TaskData during testing.
 */
public class TestTaskDataHelper {
    public static TaskData getClassifyTaskData(Document document, List<String> collectionSequenceIds,
                                               Boolean executePolicy, String projectId, String datastoreRef)
    {
        TaskData taskData = new TaskData();
        taskData.setCollectionSequence(collectionSequenceIds);
        taskData.setDocument(document);
        taskData.setExecutePolicyOnClassifiedDocuments(executePolicy);
        taskData.setProjectId(projectId);
        taskData.setOutputPartialReference(datastoreRef);
        return taskData;
    }

    public static TaskData getClassifyTaskData(Document document, String workflowId, Boolean executePolicy,
                                               String projectId, String datastoreRef)
    {
        TaskData taskData = new TaskData();
        taskData.setWorkflowId(workflowId);
        taskData.setDocument(document);
        taskData.setExecutePolicyOnClassifiedDocuments(executePolicy);
        taskData.setProjectId(projectId);
        taskData.setOutputPartialReference(datastoreRef);
        return taskData;
    }

    public static TaskData getExecuteTaskData(Document documentToExecutePolicyOn, Collection<Long> policyIDs,
                                              String projectId, List<String> collectionSequenceIds)
    {
        TaskData taskData = new TaskData();
        taskData.setProjectId(projectId);
        taskData.setDocument(documentToExecutePolicyOn);
        taskData.setPoliciesToExecute(policyIDs);
        taskData.setCollectionSequence(collectionSequenceIds);
        return taskData;
    }
}
