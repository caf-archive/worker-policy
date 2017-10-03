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
package com.github.cafdataprocessing.worker.policy.converter.qa;

import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterRuntime;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.TrackedDocument;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.github.cafdataprocessing.worker.policy.shared.PolicyWorkerConstants;
import com.google.common.collect.Multimap;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.DecodeMethod;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.WorkerTaskData;

public final class TestWorkerConverterRuntimeImpl implements PolicyWorkerConverterRuntime
{
    private final TrackedDocument trackedDocument;
    private final Codec codec;
    private final DataStore dataStore;
    private final WorkerTaskData workerTask;

    public TestWorkerConverterRuntimeImpl(
        final TrackedDocument trackedDocument,
        final Codec codec,
        final DataStore dataStore,
        final WorkerTaskData workerTask
    )
    {
        this.trackedDocument = trackedDocument;
        this.codec = codec;
        this.dataStore = dataStore;
        this.workerTask = workerTask;
    }

    @Override
    public TaskStatus getTaskStatus()
    {
        return workerTask.getStatus();
    }

    @Override
    public DocumentInterface getDocument()
    {
        return trackedDocument;
    }

    @Override
    public <T> T deserialiseData(Class<T> clazz) throws CodecException
    {
        final byte[] data = workerTask.getData();

        return codec.deserialise(data, clazz);
    }

    @Override
    public <T> T deserialiseData(Class<T> clazz, DecodeMethod decodeMethod) throws CodecException
    {
        final byte[] data = workerTask.getData();

        return codec.deserialise(data, clazz, decodeMethod);
    }

    @Override
    public DataStore getDataStore()
    {
        return dataStore;
    }

    @Override
    public byte[] getData()
    {
        return workerTask.getData();
    }

    @Override
    public void recordError(String errorCodeMessage)
    {
        // Get the document metadata
        final Multimap<String, String> documentMetadata = trackedDocument.getMetadata();

        // If there are previous failure details, then remove them
        documentMetadata.removeAll(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_CODE);
        documentMetadata.removeAll(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_MESSAGE);

        // Add the new failure error code to the document
        documentMetadata.put(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_CODE, errorCodeMessage);

        // Record the failure in a second (multi-value) field which will not get removed by subsequent logic
        documentMetadata.put(PolicyWorkerConstants.POLICYWORKER_ERROR_CODE, errorCodeMessage);
    }

    @Override
    public void recordError(String errorCode, String errorMessage)
    {
        // Get the document metadata
        final Multimap<String, String> documentMetadata = trackedDocument.getMetadata();

        // If there are previous failure details, then remove them
        documentMetadata.removeAll(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_CODE);
        documentMetadata.removeAll(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_MESSAGE);

        // Add the new failure details to the document
        documentMetadata.put(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_CODE, errorCode);
        documentMetadata.put(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_MESSAGE, errorMessage);

        // Record the failure in a second set of (multi-value) fields which will not get removed by subsequent logic
        documentMetadata.put(PolicyWorkerConstants.POLICYWORKER_ERROR_CODE, errorCode);
        documentMetadata.put(PolicyWorkerConstants.POLICYWORKER_ERROR_MESSAGE, errorMessage);
    }
}
