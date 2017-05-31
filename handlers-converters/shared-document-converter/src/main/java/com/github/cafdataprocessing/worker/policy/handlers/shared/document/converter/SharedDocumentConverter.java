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
package com.github.cafdataprocessing.worker.policy.handlers.shared.document.converter;

import com.github.cafdataprocessing.worker.policy.handlers.shared.document.SharedDocument;

import java.util.stream.Collectors;

/**
 * Shared Document Converter
 */
public class SharedDocumentConverter {
    public SharedDocument convert(com.github.cafdataprocessing.worker.policy.shared.Document policyDocument) {
        SharedDocument newDocument = new SharedDocument();

        newDocument.setReference(policyDocument.getReference());
        if (policyDocument.getMetadata() != null) {
            newDocument.getMetadata().addAll(policyDocument.getMetadata().entries());
        }
        if (policyDocument.getMetadataReferences() != null) {
            newDocument.getMetadataReference().addAll(policyDocument.getMetadataReferences().entries());
        }
        if (policyDocument.getDocuments() != null) {
            newDocument.getChildDocuments().addAll(policyDocument.getDocuments().stream().map(this::convert).collect(Collectors.toList()));
        }
        newDocument.setDocumentProcessingRecord(policyDocument.getPolicyDataProcessingRecord());

        return newDocument;
    }
}
