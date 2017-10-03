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
package com.github.cafdataprocessing.worker.policy.testing.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.corepolicy.ProcessingAction;
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.dto.ConflictResolutionMode;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import com.github.cafdataprocessing.corepolicy.policy.TagPolicy.TagPolicy;
import com.github.cafdataprocessing.worker.policy.TagPolicyHandlerBase;
import com.github.cafdataprocessing.worker.policy.common.DocumentFields;

import java.io.IOException;
import java.util.Collection;

/**
 * A handler for test purposes that checks for temporary metadata on documents.
 */
public class CheckTempFieldsWorkerHandler extends TagPolicyHandlerBase {

    private final static String policyTypeJson = "{\n" +
            "    \"properties\": {\n" +
            "        \"fieldActions\": {\n" +
            "            \"type\": \"array\",\n" +
            "            \"items\": {\n" +
            "                \"title\": \"Field Action\",\n" +
            "                \"type\": \"object\",\n" +
            "                \"properties\": {\n" +
            "                    \"name\": {\n" +
            "                        \"description\": \"The name of the field to perform the action on.\",\n" +
            "                        \"type\": \"string\",\n" +
            "                        \"minLength\": 1\n" +
            "                    },\n" +
            "                    \"action\": {\n" +
            "                        \"description\": \"The type of action to perform on the field.\",\n" +
            "                        \"type\": \"string\",\n" +
            "                        \"enum\": [\n" +
            "                            \"ADD_FIELD_VALUE\"\n" +
            "                        ]\n" +
            "                    },\n" +
            "                    \"value\": {\n" +
            "                        \"description\": \"The value to use for the field action.\",\n" +
            "                        \"type\": \"string\"\n" +
            "                    }\n" +
            "                },\n" +
            "                \"required\": [\"name\", \"action\"]\n" +
            "            }\n" +
            "        }\n" +
            "    } \n" +
            "}";

    @Override
    public PolicyType getPolicyType() {
        PolicyType policyType = new PolicyType();

        policyType.conflictResolutionMode = ConflictResolutionMode.PRIORITY;

        ObjectMapper m = new ObjectMapper();
        JsonNode definition = null;
        try {
            definition = m.readTree( policyTypeJson );
        } catch (IOException e) {
            logger.error("Could not deserialize policy type definition", e);
        }
        policyType.definition = definition;
        policyType.name = "Test CheckForTempContextInfo Type";
        policyType.description = "Used to create test policies of type CheckForTempContextInfo";
        policyType.shortName = "TestHandlers-CheckForTempContextInfo";

        return policyType;
    }

    @Override
    protected ProcessingAction handlePolicy(Document document, Policy policy, Long collectionSequenceId ) {

        // Apply any tagging information setup by this policy.
        TagPolicy policy1 = getTagPolicy(policy);

        // apply any fields to the document. setup by this policy.
        applyFieldActions(document, policy1.getFieldActions());

        Multimap<String, String> metadata = document.getMetadata();

        final boolean[] foundTmpFields = {false};
        // Now we also need to a tag a field depending on whether or not we can
        // find any of the temp metadata fields present on the metadata supplied.
        DocumentFields.getListOfKnownTemporaryData(metadata).stream().filter(propName -> metadata.containsKey(propName)).forEach(propName -> {
            foundTmpFields[0] = true;
        });

        /* add a field we know about to the document, based on the collection sequence id... */
        metadata.put("CheckTempFieldsWorkerFoundMetadata_" + collectionSequenceId, Boolean.toString(foundTmpFields[0]));

        // we can use the same engine, and don't need to requeue this to happen..
        return ProcessingAction.CONTINUE_PROCESSING;
    }

    @Override
    public Collection<Policy> resolve(Document document, Collection<Policy> policies) {
        return null;
    }
}
