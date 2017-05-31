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
package com.github.cafdataprocessing.worker.policy.converters.classification;

import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterException;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterInterface;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterRuntime;
import com.github.cafdataprocessing.worker.policy.converters.ConverterUtils;
import com.github.cafdataprocessing.worker.policy.shared.*;
import com.google.common.collect.Multimap;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.InvalidTaskException;

import java.util.Collection;

/**
 * Converts a message result from the Classification Worker (AgentStore or Elastic Search version) and applies result as fields on  the document that was evaluated.
 */
public class ClassificationWorkerConverter implements PolicyWorkerConverterInterface
{
    @Override
    public void updateSupportedClassifierVersions(Multimap<String, Integer> multimap) {
        multimap.put("PolicyClassificationWorker", PolicyWorkerConstants.API_VERSION);
        multimap.put("PolicyClassificationWorkerElasticsearch", PolicyWorkerConstants.API_VERSION);
    }

    @Override
    public void convert(PolicyWorkerConverterRuntime runtime) throws PolicyWorkerConverterException, CodecException, InvalidTaskException
    {
        TaskResponse taskResponse = runtime.deserialiseData(TaskResponse.class);

        if(taskResponse == null){
            runtime.recordError("NULL_PTR", "taskResponse is null");
            return;
        }

        if (taskResponse.getClassifiedDocuments() == null) {
            runtime.recordError("NULL_PTR", "getClassifiedDocuments returned null");
            return;
        }

        for (ClassifyDocumentResult docResult : taskResponse.getClassifiedDocuments()) {

            Collection<MatchedCollection> matchedCollections = docResult.getMatchedCollections();

            if(matchedCollections == null){
                runtime.recordError("NULL_PTR", "matchedCollections is null");
            } else {
                for (MatchedCollection matchedCollection : matchedCollections) {
                    ConverterUtils.addMetadataToDocument(ClassificationWorkerConverterFields.CLASSIFICATION_MATCHED_COLLECTION, matchedCollection.getId().toString(), runtime.getDocument());
                    Collection<CollectionPolicy> executedPolicies = matchedCollection.getPolicies();
                    for (CollectionPolicy policy : executedPolicies) {
                        ConverterUtils.addMetadataToDocument(ClassificationWorkerConverterFields.CLASSIFICATION_POLICYNAME_FIELD, policy.getName(), runtime.getDocument());
                        ConverterUtils.addMetadataToDocument(ClassificationWorkerConverterFields.CLASSIFICATION_POLICYID_FIELD, String.valueOf(policy.getId()), runtime.getDocument());
                    }
                    for (MatchedCondition matchedCondition : matchedCollection.getMatchedConditions()) {
                        ConverterUtils.addMetadataToDocument(
                                ClassificationWorkerConverterFields.getMatchedConditionField(matchedCollection.getId()),
                                String.valueOf(matchedCondition.getId()),
                                runtime.getDocument()
                        );
                    }
                }
            }
        }
    }
}
