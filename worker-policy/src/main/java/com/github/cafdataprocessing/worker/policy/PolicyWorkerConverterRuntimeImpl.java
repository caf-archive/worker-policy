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

import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterRuntime;
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.github.cafdataprocessing.worker.policy.shared.PolicyWorkerConstants;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.WorkerTaskData;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.TrackedDocument;

public final class PolicyWorkerConverterRuntimeImpl
    extends PolicyWorkerConverterRuntimeBaseImpl
    implements PolicyWorkerConverterRuntime
{
    private final TrackedDocument trackedDocument;

    public PolicyWorkerConverterRuntimeImpl(
        final TrackedDocument trackedDocument,
        final Codec codec,
        final DataStore dataStore,
        final WorkerTaskData workerTask
    )
    {
        super(codec, dataStore, workerTask);
        this.trackedDocument = trackedDocument;
    }

    @Override
    public DocumentInterface getDocument()
    {
        return trackedDocument;
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
