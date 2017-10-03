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
package com.github.cafdataprocessing.worker.policy.converters;

import com.github.cafdataprocessing.worker.policy.shared.ClassifyDocumentResult;

import java.util.stream.Collectors;

/**
 * Converts a Core Policy ClassifyDocumentResult to the ClassifyDocumentResult from worker-policy-shared
 */
public class ClassifyDocumentResultConverter {
    public ClassifyDocumentResult convert (com.github.cafdataprocessing.corepolicy.common.dto.ClassifyDocumentResult classifyDocumentResult) {
        ClassifyDocumentResult newClassifyDocumentResult = new ClassifyDocumentResult();

        newClassifyDocumentResult.setCollectionIdAssignedByDefault(classifyDocumentResult.collectionIdAssignedByDefault);
        newClassifyDocumentResult.setIncompleteCollections(classifyDocumentResult.incompleteCollections);
        MatchedCollectionConverter matchedCollectionConverter = new MatchedCollectionConverter();
        newClassifyDocumentResult.setMatchedCollections(
                classifyDocumentResult.matchedCollections.stream().map(matchedCollectionConverter::convert).collect(Collectors.toList())
        );
        newClassifyDocumentResult.setReference(classifyDocumentResult.reference);
        newClassifyDocumentResult.setResolvedPolicies(classifyDocumentResult.resolvedPolicies);

        return newClassifyDocumentResult;
    }
}
