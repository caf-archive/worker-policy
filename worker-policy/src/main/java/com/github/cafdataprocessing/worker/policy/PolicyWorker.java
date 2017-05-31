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

import com.github.cafdataprocessing.corepolicy.api.ApiProvider;
import com.github.cafdataprocessing.corepolicy.common.CorePolicyApplicationContext;
import com.github.cafdataprocessing.corepolicy.common.dto.SequenceWorkflow;
import com.github.cafdataprocessing.corepolicy.common.exceptions.TransitoryBackEndFailureCpeException;
import com.github.cafdataprocessing.worker.policy.shared.ClassifyDocumentResult;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.github.cafdataprocessing.worker.policy.shared.TaskResponse;
import com.google.common.cache.LoadingCache;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.worker.AbstractWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Receives tasks containing information about documents to perform Policy actions on and performs work on these documents such as classification and execution of policy.
 */
public class PolicyWorker extends AbstractWorker<TaskData, Object> {

    private static final int API_VERSION = 1;

    private final String workerIdentifier;
    private TaskDataConverter taskDataConverter;
    private byte[] data;
    private TaskStatus taskStatus;
    private byte[] context;
    private static Logger logger = LoggerFactory.getLogger(PolicyWorker.class);
    private ExecuteTaskData executeTaskData;

    public PolicyWorker(String workerIdentifier, CorePolicyApplicationContext applicationContext,
                        LoadingCache<Long, SequenceWorkflow> workflowCache, ApiProvider apiProvider,
                        TaskDataConverter taskDataConverter, TaskStatus taskStatus,
                        final TaskData task, final Codec codec,
                        final byte[] data, final byte[] context, String resultQueue, DataStore dataStore) throws InvalidTaskException {
        super(task, resultQueue, codec);
        this.workerIdentifier = workerIdentifier;
        this.taskDataConverter = taskDataConverter;
        this.data = data;

        this.executeTaskData = new ExecuteTaskData(applicationContext, this, new DataStoreSource(dataStore, codec),
                workflowCache, apiProvider);
        this.taskStatus = taskStatus;
        this.context = context;
    }

    public static TaskResponse createResultObject(Collection<ClassifyDocumentResult> classifyDocumentsResult) {
        TaskResponse res = new TaskResponse();
        res.setClassifiedDocuments(classifyDocumentsResult);
        return res;
    }


    private WorkerResponse Execute(TaskData task) throws InvalidTaskException, TaskRejectedException {
        return executeTaskData.execute(task);
    }

    // Expose access to protected member inside Worker Handler code.
    public WorkerResponse createSuccessResultCallback(TaskResponse taskResponse) {
        return createSuccessResult(taskResponse);
    }

    @Override
    public WorkerResponse doWork() throws InterruptedException, TaskRejectedException, InvalidTaskException {
        if (taskStatus == TaskStatus.RESULT_EXCEPTION || taskStatus == TaskStatus.RESULT_FAILURE) {
            return new WorkerResponse(this.getResultQueue(), TaskStatus.RESULT_FAILURE, data, this.getWorkerIdentifier(), this.getWorkerApiVersion(), context);
        }

        try {
            return Execute(getTask());
        } catch (InvalidTaskException e) {
            logger.error("Invalid task item detected", e);
            throw e;
        } catch (TaskRejectedException e) {
            logger.error("Transitory error occurred", e);
            throw e;
        } catch (TransitoryBackEndFailureCpeException e) {
            logger.error("Transitory error occurred", e);
            throw new TaskRejectedException("Transitory error encountered trying to execute task", e);
        }
    }

    @Override
    public String getWorkerIdentifier() {
        return this.workerIdentifier;
    }

    @Override
    public int getWorkerApiVersion() {
        return API_VERSION;
    }

    public TaskDataConverter getTaskDataConverter() {
        return taskDataConverter;
    }
}
