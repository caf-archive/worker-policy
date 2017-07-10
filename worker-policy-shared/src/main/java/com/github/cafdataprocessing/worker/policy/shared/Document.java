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
package com.github.cafdataprocessing.worker.policy.shared;

import com.github.cafdataprocessing.corepolicy.multimap.utils.CaseInsensitiveMultimap;
import com.github.cafdataprocessing.entity.fields.DocumentProcessingRecord;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.hpe.caf.util.ref.ReferencedData;

import java.util.Collection;
import java.util.Collections;

/**
 * A document to be collated against a collection sequence and it's contained collections and conditions.
 */
public class Document implements DocumentInterface {

    /**
     * metadata - The metadata for the document.
     *
     */
    private CaseInsensitiveMultimap<String> metadata = CaseInsensitiveMultimap.create();

    /**
     * metadataReferences - The ReferencedData Object is used in preference to String to allow metadata values to be
     * references to data stored in the object store.
     */
    private HashMultimap<String, ReferencedData> metadataReferences = HashMultimap.create();

    /**
     * policyDataProcessingRecord - The policyDataProcessingRecord contains information about how an item has been
     * changed by policy workflow, so that is can be used to revert the document to its#
     * original state.
     */
    private DocumentProcessingRecord policyDataProcessingRecord = null;

    /**
     * An identifier for the document
     */
    private String reference;

    /**
     * Sub documents
     */
    private Collection<Document> documents;

    @Override
    public Multimap<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata( Multimap<String, String> metadata ) {
        this.metadata = metadata == null ? null : CaseInsensitiveMultimap.create( metadata );
    }

    @Override
    public Multimap<String, ReferencedData> getMetadataReferences() {
        return metadataReferences;
    }

    public void setMetadataReferences( Multimap<String, ReferencedData> metadataReferences ) {
        this.metadataReferences = metadataReferences == null ? null : HashMultimap.create( metadataReferences );
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public void setReference( String reference ) {
        this.reference = reference;
    }

    @Override
    public Collection<DocumentInterface> getSubDocuments() {
        return Collections.unmodifiableCollection(documents);
    }

    public Collection<Document> getDocuments() {
        return documents;
    }

    public void setDocuments( Collection<Document> documents ) {
        this.documents = documents;
    }

    /**
     * @return the policyDataProcessingRecord
     */
    @Override
    public DocumentProcessingRecord getPolicyDataProcessingRecord() {

        return policyDataProcessingRecord;
    }

    @Override
    public DocumentProcessingRecord createPolicyDataProcessingRecord() {

        // allow for someone to transmit no policyDataProcessingRecord, and instead create one now.
        // Remember this is mostly a contract for serialization, and not just a Java Class.
        if ( policyDataProcessingRecord == null ) {
            policyDataProcessingRecord = new DocumentProcessingRecord();
        }

        return policyDataProcessingRecord;
    }

    /**
     * Remove the existing record, this happens at the end of a reconstituteOriginalDocument when the old
     * reprocessing record should no longer be present.
     */
    @Override
    public void deletePolicyDataProcessingRecord() {
        this.policyDataProcessingRecord = null;
    }

    /**
     * @param policyDataProcessingRecord the policyDataProcessingRecord to set.
     * Only use this to copy across an existing data processing record onto the document.
     */
    public void setPolicyDataProcessingRecord(
            DocumentProcessingRecord policyDataProcessingRecord ) {
        this.policyDataProcessingRecord = policyDataProcessingRecord;
    }

    @Override
    public void removeSubdocument(String reference)
    {
        documents.stream().forEach(doc -> {
            if (doc.getReference().equals(reference)) {
                documents.remove(doc);
                return;
            }
        });
    }

    @Override
    public DocumentInterface addSubDocument(String reference)
    {
        Document document = new Document();
        document.setReference(reference);
        documents.add(document);
        return document;
    }
}
