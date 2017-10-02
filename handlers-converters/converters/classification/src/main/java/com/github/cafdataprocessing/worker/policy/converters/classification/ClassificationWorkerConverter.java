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
package com.github.cafdataprocessing.worker.policy.converters.classification;

import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterException;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterInterface;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterRuntime;
import com.github.cafdataprocessing.worker.policy.converters.ConverterUtils;
import com.github.cafdataprocessing.worker.policy.shared.*;
import com.google.common.collect.Multimap;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.InvalidTaskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Converts a message result from the Classification Worker (AgentStore or Elastic Search version) and applies result as fields on  the document that was evaluated.
 */
public class ClassificationWorkerConverter implements PolicyWorkerConverterInterface
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassificationWorkerConverter.class);

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
                continue;
            }
            //keeping track of sub-documents to avoid having to iterate through the tree of children per matched collection
            //or condition if we already encountered a document previously
            HashMap<String, DocumentInterface> refsToDocumentMap = new HashMap<>();
            DocumentInterface documentToApplyCollectionTo = runtime.getDocument();

            for (MatchedCollection matchedCollection : matchedCollections) {
                addMatchedCollectionFields(matchedCollection, documentToApplyCollectionTo);
                //check the conditions on the matched collection to identify those docs that were responsible for the match
                //so the collection fields can be applied to any sub-documents that caused the match also
                applyMatchToSubDocs(matchedCollection, documentToApplyCollectionTo.getReference(),
                        documentToApplyCollectionTo.getSubDocuments(), refsToDocumentMap);
            }
        }
    }

    private void addMatchedCollectionFields(MatchedCollection matchedCollection, DocumentInterface document){
        ConverterUtils.addMetadataToDocument(ClassificationWorkerConverterFields.CLASSIFICATION_MATCHED_COLLECTION,
                matchedCollection.getId().toString(), document);
        addMatchedPolicyFields(matchedCollection.getPolicies(), document);
    }

    private void addMatchedPolicyFields(Collection<CollectionPolicy> executedPolicies, DocumentInterface document){
        for (CollectionPolicy policy : executedPolicies) {
            String policyName = policy.getName();
            Long policyId = policy.getId();
            // the policy_fields are deprecated and may be removed in later releases
            ConverterUtils.addMetadataToDocument(ClassificationWorkerConverterFields.CLASSIFICATION_POLICYNAME_FIELD,
                    policyName, document);
            ConverterUtils.addMetadataToDocument(ClassificationWorkerConverterFields.CLASSIFICATION_POLICYID_FIELD,
                    String.valueOf(policyId), document);
            ConverterUtils.addMetadataToDocument(ClassificationWorkerConverterFields.CLASSIFICATION_NAME_FIELD,
                    policyName, document);
            ConverterUtils.addMetadataToDocument(ClassificationWorkerConverterFields.CLASSIFICATION_ID_FIELD,
                    String.valueOf(policyId), document);
        }
    }

    private void applyMatchToSubDocs(MatchedCollection matchedCollection, String rootReference,
                                                              Collection<DocumentInterface> subDocs,
                                                              HashMap<String, DocumentInterface> refsToDocumentMap){
        if(subDocs==null || subDocs.isEmpty()){
            return;
        }
        Collection<DocumentInterface> subDocsToApplyMatchFieldsTo = new ArrayList<>();
        Collection<MatchedCondition> matchedConditions = matchedCollection.getMatchedConditions();
        for(MatchedCondition matchedCondition: matchedConditions){
            String matchedDocumentReference = matchedCondition.getReference();
            if(matchedDocumentReference==null || matchedDocumentReference.equals(rootReference)){
                continue;
            }
            //check if we've already found this sub-document in the doc map from a previous findSubDoc search
            if(refsToDocumentMap.containsKey(matchedDocumentReference)){
                //don't add the document if it is already in the collection due to matching another condition
                DocumentInterface matchedSubDocument = refsToDocumentMap.get(matchedDocumentReference);
                if(!subDocsToApplyMatchFieldsTo.contains(matchedSubDocument)){
                    subDocsToApplyMatchFieldsTo.add(matchedSubDocument);
                }
                continue;
            }
            DocumentInterface matchedSubDoc = findSubDoc(subDocs, matchedDocumentReference);
            if(matchedSubDoc==null){
                LOGGER.warn("Condition matched on sub-document \""+matchedDocumentReference+
                        "\" but cannot find sub-document to apply matched fields to.");
                continue;
            }
            refsToDocumentMap.put(matchedDocumentReference, matchedSubDoc);
            subDocsToApplyMatchFieldsTo.add(matchedSubDoc);
        }
        for(DocumentInterface subDocToApplyMatchFieldsTo: subDocsToApplyMatchFieldsTo){
            addMatchedCollectionFields(matchedCollection, subDocToApplyMatchFieldsTo);
        }
    }

    @Nullable
    private DocumentInterface findSubDoc(Collection<DocumentInterface> subDocs, String docReferenceToFind){
        for(DocumentInterface currentSubDoc: subDocs){
            String subDocReference = currentSubDoc.getReference();
            if(subDocReference!=null && subDocReference.equals(docReferenceToFind)){
                return currentSubDoc;
            }
            Collection<DocumentInterface> nextLevelSubDocs = currentSubDoc.getSubDocuments();
            if(nextLevelSubDocs != null && !nextLevelSubDocs.isEmpty()){
                DocumentInterface foundDocument = findSubDoc(nextLevelSubDocs, docReferenceToFind);
                if(foundDocument!=null){
                    return foundDocument;
                }
            }
        }
        return null;
    }
}
