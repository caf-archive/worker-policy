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
package com.github.cafdataprocessing.worker.policy.handlers.shared.document;

import com.github.cafdataprocessing.entity.fields.DocumentProcessingRecord;
import com.hpe.caf.util.ref.ReferencedData;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Shared document for use within the handlers and workers which don't want to define there own message interface
 */
public class SharedDocument {

    private String reference;

    private Collection<SharedDocument> childDocuments = new HashSet<SharedDocument>();

    private Collection<Map.Entry<String, String>> metadata = new HashSet<Map.Entry<String, String>>();

    private Collection<Map.Entry<String, ReferencedData>> metadataReference = new HashSet<Map.Entry<String, ReferencedData>>();

    /**
     * A record of processing that has occurred on this document, such as metadata added, updated and deleted.
     */
    private DocumentProcessingRecord documentProcessingRecord;

    public Collection<Map.Entry<String, ReferencedData>> getMetadataReference() {
        return metadataReference;
    }

    public Collection<Map.Entry<String, String>> getMetadata() {
        return metadata;
    }

    public Collection<SharedDocument> getChildDocuments() {
        return childDocuments;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * Get the processing record for this SharedDocument.
     * @return
     */
    public DocumentProcessingRecord getDocumentProcessingRecord(){
        return documentProcessingRecord;
    }

    /**
     * Set the processing record for this Shared Document.
     * @param documentProcessingRecord
     */
    public void setDocumentProcessingRecord(DocumentProcessingRecord documentProcessingRecord){
        this.documentProcessingRecord = documentProcessingRecord;
    }
}
