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
package com.github.cafdataprocessing.worker.policy.data.reprocessing.builder;

import com.github.cafdataprocessing.worker.policy.data.reprocessing.builder.exceptions.PolicyReprocessingReconstructMessageException;
import com.google.common.base.Strings;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.github.cafdataprocessing.worker.policy.shared.DocumentProcessingFieldCategory;
import com.github.cafdataprocessing.worker.policy.shared.DocumentProcessingFieldType;
import com.github.cafdataprocessing.worker.policy.shared.utils.DocumentFieldChanger;
import com.github.cafdataprocessing.entity.fields.DocumentProcessingRecord;
import com.github.cafdataprocessing.entity.fields.FieldChangeRecord;

import java.security.InvalidParameterException;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author getty
 */
public final class PolicyReprocessingDocumentBuilder {

    private static final Logger logger = LoggerFactory.getLogger( PolicyReprocessingDocumentBuilder.class );

    private PolicyReprocessingDocumentBuilder(){

    }

    /**
     * Augments the document, to return it to its original state.
     * @param document The document, to be reconstituted to its original state.
     * @throws PolicyReprocessingReconstructMessageException exception thrown when a problem happens during reconstruction of the document.
     */
    public static void reconstituteOriginalDocument( DocumentInterface document ) throws PolicyReprocessingReconstructMessageException
    {
        logger.trace("reconstituteOriginalDocument entered for document with reference: " +
                ( Strings.isNullOrEmpty( document.getReference() ) ? "null" : document.getReference() ) );

        /**
         * Obtain the Policy Data Processing Record, and use this to reconstitute the original document.
         */
        if ( document == null )
        {
            logger.error( "reconstituteOriginalDocument - null document supplied");

            throw new InvalidParameterException("No valid document object was supplied.");
        }

        DocumentProcessingRecord processingRecord = document.getPolicyDataProcessingRecord();

        if (processingRecord == null)
        {
            // there is no processing record, so we have no manipulation to perform to the document
            // supplied, just exit.
            logger.debug("reconstituteOriginalDocument has no work to perform due to no data processing record.");
            return;
        }

        logger.debug("reconstituteOriginalDocument is about to reset document using the data processing record.");

        /** We have a record, so loop over each of these and perform:
         * - Added fields - remove fields with each name from the document.
         * - Deleted fields - add these fields with each name and values(s) back onto the document.
         * - Updated fields - remove any fields with each name, and add these value(s) to the document instead.
         */
        DocumentFieldChanger documentChanger = new DocumentFieldChanger( document );

        // make any changes based on the reference field change record.
        applyFieldChangeRecordChanges( documentChanger, DocumentProcessingFieldCategory.REFERENCE, processingRecord.referenceChange, null );

        // now make any changes based on whatever lies within the metadataChanges records.
        applyFieldChangeRecordChanges( documentChanger, DocumentProcessingFieldCategory.ALLMETADATA, processingRecord.metadataChanges );

        // finally clear out the data processing record, so we can begin again!
        document.deletePolicyDataProcessingRecord();
    }

    //change documentFieldType to a true or false flag to indicate reference or not
    private static void applyFieldChangeRecordChanges( DocumentFieldChanger documentChanger,
                                                       DocumentProcessingFieldCategory documentProcessingFieldCategory,
                                                       Map<String, FieldChangeRecord> fieldChangeRecords ) {

        // if we are given no changes, we have nothing to do.
        if ( fieldChangeRecords == null || fieldChangeRecords.isEmpty() )
        {
            return;
        }

        Set<Map.Entry<String, FieldChangeRecord>> entries = fieldChangeRecords.entrySet();
        for ( Map.Entry<String, FieldChangeRecord> entry : entries )
        {
            applyFieldChangeRecordChanges( documentChanger, documentProcessingFieldCategory, entry.getValue(),
                    entry.getKey() );
        }
    }

    private static void applyFieldChangeRecordChanges( DocumentFieldChanger documentChanger,
                                                       DocumentProcessingFieldCategory documentFieldCategory,
                                                       FieldChangeRecord fieldChange, String fieldName ) {

        // get the values associated with this field type and field name.
        if ( fieldChange == null )
        {
            return;
        }

        switch( fieldChange.changeType )
        {
            case added:
                // a field has been added so delete it.
                fieldDeleteByDocumentProcessingFieldCategory(documentChanger, documentFieldCategory, fieldName);
            case deleted:
                // Turn the fieldValues into a friendly collection we can set on the document.
                documentChanger.addFieldValues(documentFieldCategory, fieldName, fieldChange.changeValues);
                break;
            case updated:
                fieldDeleteByDocumentProcessingFieldCategory(documentChanger, documentFieldCategory, fieldName);
                documentChanger.addFieldValues(documentFieldCategory, fieldName, fieldChange.changeValues);
                break;
            default:
                throw new RuntimeException("Unknown FieldChangeRecord changeType: " + fieldChange.changeType );
        }
    }

    private static void fieldDeleteByDocumentProcessingFieldCategory(DocumentFieldChanger documentChanger,
                                                                     DocumentProcessingFieldCategory documentFieldCategory,
                                                                     String fieldName){
        switch (documentFieldCategory){
            case REFERENCE:
                documentChanger.deleteFieldValues(DocumentProcessingFieldType.REFERENCE, fieldName);
                break;
            case ALLMETADATA:
                documentChanger.deleteFieldValues(DocumentProcessingFieldType.METADATA, fieldName);
                documentChanger.deleteFieldValues(DocumentProcessingFieldType.METADATA_REFERENCE, fieldName);
                break;
            default:
                throw new RuntimeException("Unknown DocumentProcessingFieldCategory category: " + documentFieldCategory );
        }
    }
}