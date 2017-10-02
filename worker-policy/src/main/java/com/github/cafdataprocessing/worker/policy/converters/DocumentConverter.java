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

import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.DocumentImpl;
import com.github.cafdataprocessing.corepolicy.multimap.utils.CaseInsensitiveMultimap;
import com.github.cafdataprocessing.worker.policy.common.DataStoreAwareInputStream;
import com.hpe.caf.api.worker.DataStoreSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Logic for converting between Core Policy Document and worker-policy-shared Document types
 */
public class DocumentConverter {
    private static Logger logger = LoggerFactory.getLogger(DocumentConverter.class);

    public Document convert(com.github.cafdataprocessing.worker.policy.shared.Document document, DataStoreSource dataStoreSource) {
        DocumentImpl newDocument = new DocumentImpl();

        CaseInsensitiveMultimap<String> metadata = newDocument.getMetadata();

        // add all metadata.
        if (document.getMetadata() != null) {
            document.getMetadata().entries().stream()
                    .filter(entry -> entry.getValue() != null)
                    .forEach(entry -> metadata.put(entry.getKey(), entry.getValue()));
        }
        // add all doc references, as inputStreams to the target document.
        if (document.getMetadataReferences() != null) {
            document.getMetadataReferences().entries().stream()
                    .filter(entry -> entry.getValue() != null)
                    .forEach(entry -> {
                        // create the lazy initialized stream using the unique reference for the DataStore.
                        DataStoreAwareInputStream smartStream = new DataStoreAwareInputStream(entry.getValue(), dataStoreSource);
                        newDocument.getStreams().put(entry.getKey(), smartStream);
                    });
        }

        newDocument.setFullMetadata(true);

        if (document.getDocuments() != null) {
            newDocument.getDocuments().addAll(document.getDocuments().stream().map(u -> convert(u, dataStoreSource)).collect(Collectors.toList()));
        }

        newDocument.setReference(document.getReference());

        return newDocument;
    }

    /**
     * Converts the core policy document provided to a worker policy representation, copying metadata and streams to
     * metadata and metadata reference fields and converting any sub-documents also.
     * @param document Core policy document to convert
     * @return Converted version of passed document.
     */
    public com.github.cafdataprocessing.worker.policy.shared.Document convert(Document document) {
        com.github.cafdataprocessing.worker.policy.shared.Document newDocument =
                new com.github.cafdataprocessing.worker.policy.shared.Document();
        newDocument.setReference(document.getReference());

        if (document.getMetadata() != null) {
            for (Map.Entry<String, String> entry : document.getMetadata().entries()) {
                newDocument.getMetadata().put(entry.getKey(), entry.getValue());
            }
        }

        //copy the ReferenceData of any DataStoreAwareInputStreams as metadata references on new document
        if(document.getStreams() != null) {
            for (Map.Entry<String, InputStream> streamEntry : document.getStreams().entries()){
                if(streamEntry.getValue() == null || !(streamEntry.getValue() instanceof DataStoreAwareInputStream)){
                    continue;
                }
                DataStoreAwareInputStream asDataStoreStreamEntry = (DataStoreAwareInputStream) streamEntry.getValue();
                newDocument.getMetadataReferences().put(streamEntry.getKey(), asDataStoreStreamEntry.getReferencedData());
            }
        }

        if (document.getDocuments() != null) {
            newDocument.setDocuments(document.getDocuments().stream().map(this::convert).collect(Collectors.toList()));
        }

        return newDocument;
    }
}
