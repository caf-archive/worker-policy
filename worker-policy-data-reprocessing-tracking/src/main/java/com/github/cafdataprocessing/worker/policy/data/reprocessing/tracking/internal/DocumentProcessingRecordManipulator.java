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
package com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.internal;

import com.github.cafdataprocessing.worker.policy.shared.DocumentProcessingFieldType;
import com.github.cafdataprocessing.entity.fields.DocumentProcessingRecord;
import com.github.cafdataprocessing.entity.fields.FieldChangeRecord;
import com.github.cafdataprocessing.entity.fields.FieldChangeType;
import com.github.cafdataprocessing.entity.fields.printer.FieldChangeRecordPrinter;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.exceptions.TrackedChangeException;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.exceptions.TrackedChangeTypeException;

import java.util.HashMap;
import java.util.Map;


/**
 * Internal Wrapping Class purely to add/update/delete information from the DataProcessingRecord class, it does not
 * understand when to do each of these, only accessing classes to manipulate the actual record itself.
 * This is here to prevent us placing logic into the actual contract class itself, or into the DataProcessingRecordImpl
 *
 * @author trevor.getty@hpe.com
 */
class DocumentProcessingRecordManipulator {

    private final DocumentProcessingRecord internalDocumentProcessingRecord;

    public DocumentProcessingRecordManipulator( DocumentProcessingRecord existingRecord ) throws TrackedChangeException {
        if ( existingRecord == null ) {
            throw new TrackedChangeException( 
                    "Attempt to create a DocumentProcessingRecordManipulator without any existing DocumentProcessingRecord." );
        }

        internalDocumentProcessingRecord = existingRecord;
    }

    /**
     * Get existing field change record information from the requested processing record. 
     * @param fieldType
     * @param fieldName
     * @return
     * @throws TrackedChangeException
     * @throws TrackedChangeTypeException 
     */
    public FieldChangeRecord getFieldChangeRecord( DocumentProcessingFieldType fieldType,
                                                   String fieldName ) throws TrackedChangeException, TrackedChangeTypeException {

        // get the existing record, if present, or null
        switch ( fieldType ) {
            case REFERENCE:
                return internalDocumentProcessingRecord.referenceChange;

            case METADATA_REFERENCE:
            case METADATA:
                return ( internalDocumentProcessingRecord.metadataChanges == null ) ? 
                       null : internalDocumentProcessingRecord.metadataChanges.get( fieldName );
            default:
                throw new TrackedChangeTypeException( 
                        "removeDataProcessingRecordFieldChange - Invalid document processing field type specified: " + fieldType );

        }
    }

    /**
     *  Add a new field change record to the data processing record. 
     * @param fieldType
     * @param newFieldChange
     * @param fieldName
     * @throws TrackedChangeException
     * @throws TrackedChangeTypeException 
     */
    public void addFieldChangeRecord( DocumentProcessingFieldType fieldType,
                                      FieldChangeRecord newFieldChange,
                                      String fieldName ) throws TrackedChangeException, TrackedChangeTypeException {

        // strip off the originalValues from the added fieldChangeValues, they were needed for internal tracking, but noone wants them
        // externally - if this changes - just make this method do nothing.
        stripAddedFieldChangeValues( newFieldChange );
        
        switch ( fieldType ) {
            case REFERENCE:
                // set the reference change record to new value, whether its add or update we can only ever have one
                // reference, it would be better to call update though if it already has a value.
                internalDocumentProcessingRecord.referenceChange = newFieldChange;
                return;

            case METADATA_REFERENCE:
            case METADATA:
                getOrCreateMetadataChanges().put( fieldName, newFieldChange );
                return;
            default:
                throw new TrackedChangeTypeException( 
                        "removeDataProcessingRecordFieldChange - Invalid document processing field type specified: " + fieldType );

        }
    }

    /**
     * strip off the originalValues from the added fieldChangeValues, they were needed for internal tracking, but noone wants them
     * externally - if this changes - just make this method do nothing.
     * @param newFieldChange 
     */
    private void stripAddedFieldChangeValues( FieldChangeRecord newFieldChange ) {
        // For added field records we strip off the original values, as they dont need them.
        if ( newFieldChange.changeType == FieldChangeType.added )
        {
            newFieldChange.changeValues = null;
        }
    }

    /**
     * Delete existing field change record information from the requested processing record. 
     * @param fieldType
     * @param existingFieldChange
     * @throws TrackedChangeException
     * @throws TrackedChangeTypeException 
     */
    public void removeFieldChangeRecord( DocumentProcessingFieldType fieldType, 
                                         String fieldName,
                                         FieldChangeRecord existingFieldChange ) throws TrackedChangeException, TrackedChangeTypeException {

        switch ( fieldType ) {
            case REFERENCE:
                // set the reference change record to null as there is only one fieldchangerecord.
                internalDocumentProcessingRecord.referenceChange = null;
                return;

            case METADATA_REFERENCE:
            case METADATA:
                
                if ( internalDocumentProcessingRecord.metadataChanges == null ) {
                    // for some reason the record we have been given isn't the correct record to be removed.
                    throw new TrackedChangeException( 
                            "Attempt to remove metadataChanges existing fieldChangeRecord failed as no metadatachanges exist fieldChangeRecord Info: "
                            + FieldChangeRecordPrinter.toFormattedString(existingFieldChange) );
                }
                
                // As we only have a single entry per field name, its safe to remove by fieldName, if not use object to try to find it also.
                FieldChangeRecord removedFieldChange = internalDocumentProcessingRecord.metadataChanges.remove( fieldName );

                if ( removedFieldChange == null ) {
                    // for some reason the record we have been given isn't the correct record to be removed.
                    throw new TrackedChangeException( 
                            "Attempt to remove metadataChanges existing fieldChangeRecord failed for record: "
                            + FieldChangeRecordPrinter.toFormattedString( existingFieldChange ) );
                }

                // we have removed our record successfully.
                return;
            default:
                throw new TrackedChangeTypeException( 
                        "removeDataProcessingRecordFieldChange - Invalid document processing field type specified: " + fieldType );

        }
    }
    
    
    
    /**
     * Utility method to get or create the metadataChanges map, to ensure that access to the map is not done when
     * its null.
     */
    private Map<String, FieldChangeRecord> getOrCreateMetadataChanges() {
        
        // add our new fieldChangeRecords map.
        if ( internalDocumentProcessingRecord.metadataChanges == null )
        {
            internalDocumentProcessingRecord.metadataChanges = new HashMap<String, FieldChangeRecord>();
        }
        
        return internalDocumentProcessingRecord.metadataChanges;
    }
}
