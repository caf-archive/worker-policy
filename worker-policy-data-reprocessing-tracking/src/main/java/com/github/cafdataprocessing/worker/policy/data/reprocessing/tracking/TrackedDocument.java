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
package com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking;

import com.github.cafdataprocessing.worker.policy.shared.DocumentProcessingFieldType;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.internal.TrackedDocumentInternal;
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.github.cafdataprocessing.entity.fields.*;
import com.hpe.caf.util.ref.ReferencedData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An Implementation of the policyworker.shared.documentinterface, which allows a shared.document object to be wrapped,
 * for the purpose of tracking changes which are made to it, and keeping up to date a record of the changed fields.
 * @author trevor.getty@hpe.com
 */
public class TrackedDocument extends TrackedDocumentInternal implements DocumentInterface  {
        
    private static final Logger logger = LoggerFactory.getLogger( TrackedDocument.class );
    
    // now we have a wrapped document, we also need to wrap any objects that we can return, in order to track calls
    // made on them, like getMetadata().put(key,value);
    private TrackedMultiMap<String, String> trackedMetadata;
    private TrackedMultiMap<String, ReferencedData> trackedMetadataRefs;
    private Collection<DocumentInterface> trackedSubDocuments;
            
    /**
     * Constructor to create a blank TrackedDocument.
     */
    public TrackedDocument() {
        // if you create a blank TrackedDocument, allow this to create its own internal implementation
        // using a shared.document instead of using an existing one.        
        super();
    }
    
    /**
     * Constructor to create a TrackedDocument monitoring all changes to an existing document.
     * 
     * @param existingDocument The existing document object, that you wish to create the TrackedDocument wrapper on.
     */
    public TrackedDocument( DocumentInterface existingDocument )
    {
        super( existingDocument );
    }

    /**
     * returns a tracked version of the existing metadata multimap.
     * 
     * Changes made to this multimap are recorded in the DataProcessingRecord.
     * @return a tracked multimap containing all the existing field names / values, but additionally tracks new changes.
     */
    @Override
    public Multimap<String, String> getMetadata() {
        
        // have we got a wrapper for the metadata map, if not create one now
        return ( trackedMetadata == null ) ? new TrackedMultiMap( trackedDocumentChanger.getInternalDocument(), DocumentProcessingFieldType.METADATA ) : trackedMetadata;
    }

    /**
     * returns a tracked version of the existing metadataReferences multimap.
     * 
     * Changes made to this multimap are recorded in the DataProcessingRecord.
     * @return a tracked multimap containing all the existing field names / values, but additionally tracks new changes.
     */
    @Override
    public Multimap<String, ReferencedData> getMetadataReferences() {
        return ( trackedMetadataRefs == null ) ? new TrackedMultiMap( trackedDocumentChanger.getInternalDocument(), DocumentProcessingFieldType.METADATA_REFERENCE ) : trackedMetadataRefs;
    }

    /**
     * Returns the existing reference field from the document.
     * @return the existing reference as a string.
     */
    @Override
    public String getReference() {
        
        return trackedDocumentChanger.getInternalDocument().getReference();
    }

    /**
     * Changes made to the reference field are recorded in the DataProcessingRecord.
     * @param newReference set the document reference to the given value, and track the change.
     */
    @Override
    public void setReference( String newReference ) {

        // we have a base implementation to ensure that we only create / update
        // the policy data processing record information after we change
        // the underlying document.
        setFieldValue( null, newReference, DocumentProcessingFieldType.REFERENCE );
    }
    
    /**
     * Contains information about changes made to the original document.
     * This will only return whatever DataProcessingRecord is present (if any).
     * @return the existing policyDataProcessingRecord field.  This will not create the field, only return it.
     */
    @Override
    public DocumentProcessingRecord getPolicyDataProcessingRecord() {
        return trackedDocumentChanger.getInternalDocument().getPolicyDataProcessingRecord();
    }
 
    /**
     * Create a Policy Data Processing Record object, inside a sharedDocument, only if it does not already exist.
     * 
     * @return the created policyDataProcessingRecord.
     */
    @Override
    public DocumentProcessingRecord createPolicyDataProcessingRecord() {
        return trackedDocumentChanger.getInternalDocument().createPolicyDataProcessingRecord();
    }
    
    /**
     * Used at the end of a reconstituteOriginalDocument call, as this removes the actual
     * dataProcessingRecord so that a new one can be created going forward from scratch. 
     * Note this is not a tracked change.
     */
    @Override
    public void deletePolicyDataProcessingRecord() {
        trackedDocumentChanger.getInternalDocument().deletePolicyDataProcessingRecord();
    }

    @Override
    public Collection<DocumentInterface> getSubDocuments()
    {
        if (trackedSubDocuments == null) {
            Collection<DocumentInterface> trackedsubDocumentsCollection = new ArrayList<>();
            trackedDocumentChanger.getInternalDocument().getSubDocuments().forEach((document) -> {
                trackedsubDocumentsCollection.add(new TrackedDocument(document));
            });
            this.trackedSubDocuments = trackedsubDocumentsCollection;
        }
        return Collections.unmodifiableCollection(trackedSubDocuments);
    }

    @Override
    public void removeSubdocument(final String reference)
    {
        trackedDocumentChanger.getInternalDocument().getSubDocuments().stream().forEach(doc -> {
            if (doc.getReference().equals(reference)) {
                trackedDocumentChanger.getInternalDocument().removeSubdocument(reference);
                return;
            }
        });
        trackedSubDocuments.stream().forEach(doc -> {
            if (doc.getReference().equals(reference)) {
                trackedSubDocuments.remove(doc);
                return;
            }
        });
    }

    @Override
    public DocumentInterface addSubDocument(final String reference)
    {
        DocumentInterface document = trackedDocumentChanger.getInternalDocument().addSubDocument(reference);
        TrackedDocument trackedDocument = new TrackedDocument(document);
        if(trackedSubDocuments==null){
            trackedSubDocuments = new ArrayList<>();
        }
        trackedSubDocuments.add(trackedDocument);
        return trackedDocument;
    }
}
