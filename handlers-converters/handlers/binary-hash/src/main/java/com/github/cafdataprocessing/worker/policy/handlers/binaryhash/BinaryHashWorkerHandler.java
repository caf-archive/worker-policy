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
package com.github.cafdataprocessing.worker.policy.handlers.binaryhash;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.base.Strings;

import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.dto.ConflictResolutionMode;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import com.github.cafdataprocessing.worker.policy.WorkerTaskResponsePolicyHandler;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;

import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.binaryhash.BinaryHashWorkerConstants;
import com.hpe.caf.worker.binaryhash.BinaryHashWorkerTask;

import java.io.IOException;

import java.util.Collection;

public class BinaryHashWorkerHandler extends WorkerTaskResponsePolicyHandler
{
    private final BinaryHashWorkerHandlerProperties properties;

    public BinaryHashWorkerHandler()
    {
        this.properties = loadWorkerHandlerProperties(BinaryHashWorkerHandlerProperties.class);
    }

    @Override
    protected WorkerHandlerResponse handleTaskPolicy(Document document, Policy policy, Long collectionSequenceID, TaskData taskData)
    {
        BinaryHashWorkerTask task = new BinaryHashWorkerTask();
        ObjectMapper mapper = new ObjectMapper();

        BinaryHashPolicyDefinition policyDef = mapper.convertValue(policy.details, BinaryHashPolicyDefinition.class);

        task.sourceData = ReferencedData.getReferencedData(getStorageReference(document));

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
            BinaryHashWorkerConstants.WORKER_NAME,
            BinaryHashWorkerConstants.WORKER_API_VER);
    }

    @Override
    public PolicyType getPolicyType()
    {
        PolicyType policyType = new PolicyType();
        policyType.conflictResolutionMode = ConflictResolutionMode.PRIORITY;
        ObjectMapper m = new ObjectMapper();
        JsonNode definition = null;
        try {
            definition = m.readTree(this.getClass().getResource("/binaryhash-policy-definition.json"));
        } catch (IOException e) {
            logger.error("Could not deserialize BinaryHashPolicyType definition", e);
        }
        policyType.definition = definition;
        policyType.description = "Returns a SHA-1 digest of the input file.";
        policyType.name = "BinaryHash Policy Type";
        policyType.shortName = "BinaryHashPolicyType";
        return policyType;
    }

    @Override
    public Collection<Policy> resolve(Document document, Collection<Policy> collection)
    {
        return null;
    }

    private String getStorageReference(Document document)
    {
        Collection<String> storageReferences = document.getMetadata().get("storageReference");
        if (storageReferences.size() != 1) {
            throw new RuntimeException("No storageReference set");
        }
        return storageReferences.stream().findAny().get();
    }
}
