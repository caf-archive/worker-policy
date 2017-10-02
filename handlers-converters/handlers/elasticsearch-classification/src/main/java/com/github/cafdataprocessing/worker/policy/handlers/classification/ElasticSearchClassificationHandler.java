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
package com.github.cafdataprocessing.worker.policy.handlers.classification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cafdataprocessing.corepolicy.common.dto.ConflictResolutionMode;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import com.github.cafdataprocessing.worker.policy.handlers.shared.HandlerProperties;

import java.io.IOException;

/**
 * Handler to construct a task message from a Policy and Document to send to the ElasticSearch implementation of Classification Worker.
 */
public class ElasticSearchClassificationHandler extends ClassificationHandlerBase {
    @Override
    protected HandlerProperties getProperties(){
        return loadWorkerHandlerProperties(ElasticSearchClassificationWorkerProperties.class);
    }

    @Override
    public PolicyType getPolicyType() {
        PolicyType policyType = new PolicyType();

        policyType.conflictResolutionMode = ConflictResolutionMode.PRIORITY;

        ObjectMapper m = new ObjectMapper();
        JsonNode definition = null;
        try {
            definition = m.readTree(this.getClass().getResource("/elastic-search-classification-policy-definition.json"));
        } catch (IOException e) {
            logger.error("Could not deserialize classification definition", e);
        }
        policyType.definition = definition;
        policyType.name = "Elastic Search Classification Policy Type";
        policyType.description = "Classifies a document into predefined collections using the Elastic Search Classification Worker.";
        policyType.shortName = "ElasticSearchClassificationPolicyType";

        return policyType;
    }
}
