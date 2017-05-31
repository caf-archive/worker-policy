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
package com.github.cafdataprocessing.worker.policy.handlers.fieldmapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.github.cafdataprocessing.corepolicy.ProcessingAction;
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.dto.ConflictResolutionMode;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import com.github.cafdataprocessing.corepolicy.multimap.utils.CaseInsensitiveMultimap;
import com.github.cafdataprocessing.worker.policy.WorkerPolicyHandler;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * The Field Mapping Handler renames the metadata fields of a document, according to a configurable mapping of field names.
 */
public class FieldMappingHandler extends WorkerPolicyHandler
{
    private static Logger logger = LoggerFactory.getLogger(FieldMappingHandler.class);

    @Override
    public PolicyType getPolicyType()
    {
        final PolicyType policyType = new PolicyType();
        policyType.conflictResolutionMode = ConflictResolutionMode.PRIORITY;
        policyType.definition = new JsonNodeFactory(false).objectNode();
        policyType.name = "Field Mapping Policy Type";
        policyType.description = "Once encountered, renames the fields of this document according to a configurable field name mapping.";
        policyType.shortName = "FieldMappingPolicyType";
        return policyType;
    }

    @Override
    protected ProcessingAction handlePolicy(Document document, Policy policy, Long collectionSequenceID)
    {
        mapFields(getFieldNameMappings(policy), document.getMetadata());
        return ProcessingAction.CONTINUE_PROCESSING;
    }

    @Override
    public Collection<Policy> resolve(Document document, Collection<Policy> collection)
    {
        return collection;
    }

    private void mapFields(final Map<String, String> fieldNameMappings,
                           final Multimap<String, String> documentMetadata)
    {
        final Multimap<String, String> fieldsToMap = getFieldsToBeMapped(fieldNameMappings, documentMetadata);
        fieldsToMap.keys().forEach(documentMetadata::removeAll);
        fieldsToMap.keys().forEach((currentFieldName) -> {
            final String newFieldName = fieldNameMappings.get(currentFieldName);
            final Collection<String> currentFieldValues = fieldsToMap.get(currentFieldName);
            documentMetadata.putAll(newFieldName, currentFieldValues);
        });
    }

    private Multimap<String, String> getFieldsToBeMapped(final Map<String, String> fieldNameMappings,
                                                         final Multimap<String, String> documentMetadata)
    {
        final Multimap<String, String> fieldsToMap = new CaseInsensitiveMultimap<>();
        fieldNameMappings.keySet()
                .forEach(fieldNameToMap ->
                        fieldsToMap.putAll(fieldNameToMap, documentMetadata.get(fieldNameToMap)));
        return fieldsToMap;
    }

    private Map<String, String> getFieldNameMappings(final Policy policy)
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new GuavaModule());
        FieldMappingPolicyDefinition policyDef = mapper.convertValue(policy.details, FieldMappingPolicyDefinition.class);
        return policyDef.mappings;
    }
}
