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
package com.github.cafdataprocessing.worker.policy.handlers.stop;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.cafdataprocessing.corepolicy.ProcessingAction;
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.dto.ConflictResolutionMode;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import com.github.cafdataprocessing.worker.policy.WorkerPolicyHandler;

import java.util.Collection;

/**
 * A handler that when executed prevents further policy execution on a document in a collection sequence.
 */
public class StopPolicyHandler extends WorkerPolicyHandler {
    @Override
    public PolicyType getPolicyType() {
        PolicyType policyType = new PolicyType();

        policyType.conflictResolutionMode = ConflictResolutionMode.PRIORITY;

        JsonNodeFactory nodeFactory = new JsonNodeFactory(false);
        policyType.definition = nodeFactory.objectNode();
        policyType.name = "Stop Execution Policy Type";
        policyType.description = "Once encountered, prevents subsequence policy execution on this document.";
        policyType.shortName = "StopPolicyType";

        return policyType;
    }

    @Override
    protected ProcessingAction handlePolicy(Document document, Policy policy, Long collectionSequenceID) {
        return ProcessingAction.STOP_PROCESSING;
    }

    public Collection<Policy> resolve(Document document, Collection<Policy> collection) {
        return collection;
    }
}
