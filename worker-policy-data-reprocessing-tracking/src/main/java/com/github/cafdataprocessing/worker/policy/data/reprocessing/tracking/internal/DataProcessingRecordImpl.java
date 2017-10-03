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
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.github.cafdataprocessing.entity.fields.DocumentProcessingRecord;
import com.github.cafdataprocessing.entity.fields.FieldChangeRecord;
import com.github.cafdataprocessing.entity.fields.FieldChangeType;
import com.github.cafdataprocessing.entity.fields.FieldValue;
import com.github.cafdataprocessing.entity.fields.accessor.FieldValueAccessor;
import com.github.cafdataprocessing.entity.fields.factory.FieldValueFactory;
import com.github.cafdataprocessing.entity.fields.printer.FieldChangeRecordPrinter;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.exceptions.TrackedChangeException;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.exceptions.TrackedChangeTypeException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * DataProcessingRecordImpl, is the core implementation which decided based on a change which has happened to the
 * document,
 * the previous changes which have happened, and the current document values, what the Data Processing Record should be.
 *
 * It is the core engine, which keeps track of the delta information to return the document back to its original state,
 * and
 * is responsible for merging, and converting FieldChangeRecords or merging them with other records, depending upon the
 * change.
 *
 * @author trevor.getty@hpe.com
 */
public final class DataProcessingRecordImpl {

    private final static Logger logger = LoggerFactory.getLogger( DataProcessingRecordImpl.class );

    private DataProcessingRecordImpl() {
    }

    static void updateProcessingRecord( DocumentProcessingFieldType fieldType,
                                        DocumentInterface internalDocument,
                                        String fieldName,
                                        FieldChangeRecord fieldChangeRecordWithOriginalValues,
                                        Collection<?> actualChangedValues ) throws TrackedChangeTypeException, TrackedChangeException {

        logger.debug( "updateProcessingRecord for new change: " + FieldChangeRecordPrinter.toFormattedString( 
                fieldChangeRecordWithOriginalValues ) );

        // get the existing record if any and wrap it with a DocumentProcessingManipulator just incase we need to make 
        // any changes to it.
        DocumentProcessingRecordManipulator dataProcessingRecord
                = new DocumentProcessingRecordManipulator( getOrCreateDataProcessingRecord( internalDocument ) );

        // based on the field change type update our records.
        FieldChangeRecord existingFieldChange = dataProcessingRecord.getFieldChangeRecord( fieldType,
                                                                                           fieldName );

        if ( existingFieldChange == null ) {
            // we have no existing record.

            /**
             * ADDED - a newly added field, just add the field name, we dont need original values.
             * UPDATED - an updated field, as such we need to record an updated record, will ALL original values, so we
             * can revert back.
             * the document back to this value.
             * DELETED - deleted, record only the actually deleted values here, not all of them!
             */
            FieldChangeRecord finalRecord
                    = createFirstFieldChangeRecord( fieldChangeRecordWithOriginalValues.changeType,
                                                    fieldChangeRecordWithOriginalValues,
                                                    fieldName,
                                                    actualChangedValues );

            dataProcessingRecord.addFieldChangeRecord( fieldType, finalRecord, fieldName );
            return;
        }

        /**
         * ***************************************************************************************
         * CRITICAL TO REPROCESSING, REVIEW, REVIEW, REVIEW.
         *****************************************************************************************
         * ADDING - if we are adding a value:
         *
         * 1) we have originally added as a new field. As such adding another value doesn't change the fact
         * we are to delete them all at the end.
         *
         * 2) we have updated the field originally, as such adding another value,
         * isnt' to change our updated record which hold the initial state of the document.
         *
         * 3) we have originally deleted the field, as such we have recorded its original value(s) so
         * this addition can do one or both of these 2 things:
         * Either: 1) Cancel out all or part of the deletion record, as we are adding back in what was deleted.
         * 2) Change the deleted record to an update record, where we take and deleted records which are left after
         * applying stage 1 above, and merge this with the original values on the field prior to these new ones which
         * are being added.
         *
         * DELETING:
         *
         * 1) we have originally added this as a new field. In which case we check to see if this deletion is in fact
         * removing
         * all values i.e. the key itself. If so then the added field record, and disappear.
         *
         * 2) we have originally updated this field and recorded all original values.
         * In which case we still leave the update record untouched with original document state recorded on it.
         *
         * 3) we have deleted vaules already. In which case we can add some more deleted values to this record.
         *
         * UPDATING:
         *
         * 1) we have originally added this as a new field. In which case we can ignore any update, it still a newly
         * added field.
         *
         * 2) we have originally updated this field, in which case we leave the original update record untouched with
         * the original values
         * on it.
         *
         * 3) we have originally delete value(s) from this field. In which case we are now changing its state to this, a
         * merge should take
         * place to ensure all deleted value, and the original values, prior to the update are recorded as an update
         * record.
         *
         * Where example are used the format is:
         * (x) Document state with value of a key in brackets. i.e. this document has initial value of x. key no
         * mentioned.
         * or with key. i.e. (key1: [x, y, z]) is document with key1 values x, y, z.
         *
         * 'y+' Changes made to document in quotes, can be +added -deletd ?updated.
         * ie. 'y+' is added field y
         *
         * finally document change record value is in square brackets
         * [ x+ ] like the change value, the change record is a value, and change type.
         *
         * Full example:
         * // (key1:[ a, b ]) -> 'a-' ( b ) [ - a ] -> b+ ( b, b ) = ( [? a, b] update record )
         */
        // otherwise we have to compare what we have, and whether to update it or leave it.
        switch ( existingFieldChange.changeType ) {
            case added: {
                updateRecordFromPreviousState_Added( fieldChangeRecordWithOriginalValues, fieldType, internalDocument,
                                                     fieldName, dataProcessingRecord, existingFieldChange );
                break;
            }

            case updated: {

                // If we have an updated record, we do not do anything to it.  It is our super
                // state which will end causing all values for this fieldname to be deleted and the 
                // document sets to this value(s).  As such it doesn't
                // matter if you update, add, delete again to the value. 
                // The originally recorded update field is still how we want this document to appear!
                logger.debug( "Leaving existing update record alone for change." );
                break;
            }

            case deleted: {
                updateRecordFromPreviousState_Deleted( fieldChangeRecordWithOriginalValues, existingFieldChange,
                                                       dataProcessingRecord, fieldType, fieldName, actualChangedValues );

                break;
            }

            default:
                throw new TrackedChangeTypeException(
                        "Invalid existing field changeType: " + fieldChangeRecordWithOriginalValues.changeType );
        }

    }

    private static void updateRecordFromPreviousState_Deleted( FieldChangeRecord fieldChangeRecordWithOriginalValues,
                                                               FieldChangeRecord existingFieldChange,
                                                               DocumentProcessingRecordManipulator dataProcessingRecord,
                                                               DocumentProcessingFieldType fieldType, String fieldName,
                                                               Collection<?> actualChangedValues ) throws TrackedChangeException, TrackedChangeTypeException {

        // Examples to bear in mind.
        // key1=( a, b ) ->  a-  ( b ) [ - a ]  -> b+ ( b, b ) = ( record should be [? a, b] )
        // key1=( a, b ) -> a- ( b ) -> a- ( b ) = ( ensure we dont have 2 delete a records, record should be [a-] ! )
        // key1=( a, b ) -> a- ( b ) -> b- ( ) = ( ensure we dont have 2 delete a records! [a-, b-] )
        // key1=( a, b ) -> a- ( b ) -> a+ ( b, a ) = ( ensure we have no record as we are back to original values? [null] )
        switch ( fieldChangeRecordWithOriginalValues.changeType ) {
            case added: {
                // if we have added another field value, to a deleted field, we should instead mark it as an updated record,
                // as we want to get back to that state.
                // Merge our deleted record value x, with whats now in the record minus what has just been added ).

                // key1: ["x", "y", "z"] -> deleted x ["y", "z"] -> added ["x"]  = Should have no record
                // key1: ["x", "y", "z"] -> deleted x ["y", "z"] -> added ["a"]  = Should have an updated record with (x,y,z)
                Collection<FieldValue> originalValuesPreAdd = fieldChangeRecordWithOriginalValues.changeValues;

                Collection<FieldValue> previouslyDeletedValues = existingFieldChange.changeValues;

                // remove the existing field chane record, as we are going to change it somehow and add another.
                dataProcessingRecord.removeFieldChangeRecord( fieldType, fieldName, existingFieldChange );

                // first off run through our previously deleted values, and check if any are exact matches for our new additions
                // if so we can delete them as they cancel each other out.
                Collection<?> addedValuesAfterMerge = cancelOutPreviousChanges( actualChangedValues, previouslyDeletedValues );

                // ok so we have no deleted values left, and we have no added values left after the merge, so in fact
                // the entire record should now disappear and leave nothing.
                if ( ( previouslyDeletedValues == null || previouslyDeletedValues.isEmpty() ) && 
                     ( addedValuesAfterMerge == null || addedValuesAfterMerge.isEmpty() ) )
                {
                    logger.debug(
                            "Existing deleted items record is entirely replaced as additions cancelled them all out, so no record remains." );
                    return;
                }
                
                FieldChangeRecord newUpdateRecord = new FieldChangeRecord();
                newUpdateRecord.changeType = FieldChangeType.updated;
                newUpdateRecord.changeValues = new ArrayList<>();

                // add all previous deletions which are left.
                addFieldChangesToValuesList( previouslyDeletedValues, newUpdateRecord );
                // add any other original values which aren't additions from the original state.
                addFieldChangesToValuesList( originalValuesPreAdd, newUpdateRecord );

                // these 2 together should now be how the document was, and its now an update record
                // which are NEVER changed again....
                // if the field change record values, is empty by this point do not add it there is no
                // point.
                if ( newUpdateRecord.changeValues == null || newUpdateRecord.changeValues.isEmpty() ) {
                    logger.debug(
                            "Existing deleted items record is being removed, as additions cancelled them all out." );
                    return;
                }

                dataProcessingRecord.addFieldChangeRecord( fieldType, newUpdateRecord, fieldName );

                logger.debug(
                        "Leaving existing added deleted record changed to an update record due to new additions." );
                return;
            }
            case updated: {
                /**
                 * we originally deleted a value(s) from this field so:
                 * if we have now updated the field from value a,x, to value b. we need to merge this original
                 * information
                 * along with the deleted record, to get a true state of what the values where before the change.
                 * take:
                 * ( x, y, z ) -> z- ( x, y ) [ -z ] -> b? ( b ) with final change record [ - x, y, z ]
                 */
                // Merge the newly deleted value, with the previous delete record.
                FieldChangeRecord newUpdateRecord = new FieldChangeRecord();
                newUpdateRecord.changeType = FieldChangeType.updated;
                newUpdateRecord.changeValues = new ArrayList<>();

                // Use the original values which were there before, the replaceAll call.
                // The actual changed values should also match, as replaceAll will return the deleted fields also.
                Collection<FieldValue> originalValuesPreUpdate = fieldChangeRecordWithOriginalValues.changeValues;
                Collection<FieldValue> previouslyDeletedValues = existingFieldChange.changeValues;

                // add all previous deletions which are left.
                addFieldChangesToValuesList( previouslyDeletedValues, newUpdateRecord );
                // add any other original values which aren't additions from the original state.
                addFieldChangesToValuesList( originalValuesPreUpdate, newUpdateRecord );

                // these 2 together should now be how the document was, and its now a merged delete record
                logger.debug( 
                        "adding a merged deleted field change record, with previous deleted values, and the newly deleted values." );

                // remove old change
                dataProcessingRecord.removeFieldChangeRecord( fieldType, fieldName, existingFieldChange );

                // add new change record.
                dataProcessingRecord.addFieldChangeRecord( fieldType, newUpdateRecord, fieldName );
                return;
            }
            case deleted: {
                // Merge the newly deleted value, with the previous delete record.
                FieldChangeRecord newUpdateRecord = new FieldChangeRecord();
                newUpdateRecord.changeType = FieldChangeType.deleted;
                newUpdateRecord.changeValues = new ArrayList<>();

                Collection<FieldValue> justDeletedValues = FieldValueFactory.create( actualChangedValues );
                Collection<FieldValue> previouslyDeletedValues = existingFieldChange.changeValues;

                // add all previous deletions which are left.
                addFieldChangesToValuesList( previouslyDeletedValues, newUpdateRecord );
                // add any other original values which aren't additions from the original state.
                addFieldChangesToValuesList( justDeletedValues, newUpdateRecord );

                // these 2 together should now be how the document was, and its now a merged delete record
                logger.debug( 
                        "adding a merged deleted field change record, with previous deleted values, and the newly deleted values." );

                // remove old change
                dataProcessingRecord.removeFieldChangeRecord( fieldType, fieldName, existingFieldChange );

                // add new change record.
                dataProcessingRecord.addFieldChangeRecord( fieldType, newUpdateRecord, fieldName );

                return;
            }
            default:
                throw new TrackedChangeTypeException(
                        "Invalid newFieldChangeType: " + fieldChangeRecordWithOriginalValues.changeType );
        }
    }

    /**
     * 
     * @param actualChangedValues
     * @param previouslyDeletedValues
     * @return 
     */
    private static Collection<?> cancelOutPreviousChanges(
            Collection<?> actualChangedValues,
            Collection<FieldValue> previouslyDeletedValues ) {
        
        if  (actualChangedValues == null || actualChangedValues.isEmpty())
        {
            return actualChangedValues;
        }
        
        // create a copy, to cancel out values on...
        Collection<Object> changedValuesNotCancelledOut = new ArrayList<>();
        changedValuesNotCancelledOut.addAll( actualChangedValues );
        
        for ( Object addedItem : actualChangedValues ) {
            if ( addedItem == null ) {
                continue;
            }

            // create a field value from this and attempt to remove it.
            FieldValue tmpAddedFieldValue = FieldValueFactory.create( addedItem );
            String tmpAddedValue = FieldValueAccessor.createStringFromFieldValue( tmpAddedFieldValue );
            
            for ( FieldValue tmpItem : previouslyDeletedValues )
            {
                String tmpValue = FieldValueAccessor.createStringFromFieldValue( tmpItem );
                
                if ( StringCompare.equals( tmpValue, tmpAddedValue ))
                {
                    // we have a match here, as such they cancel each other out.
                    previouslyDeletedValues.remove(  tmpItem );
                    changedValuesNotCancelledOut.remove( addedItem );
                    logger.debug( "Newly added value, cancelled out previously deleted value: " + tmpValue );
                    break;
                }
            }
        }
        
        return changedValuesNotCancelledOut;
    }

    private static void updateRecordFromPreviousState_Added( FieldChangeRecord fieldChangeRecordWithOriginalValues,
                                                             DocumentProcessingFieldType fieldType,
                                                             DocumentInterface internalDocument, String fieldName,
                                                             DocumentProcessingRecordManipulator dataProcessingRecord,
                                                             FieldChangeRecord existingFieldChange ) throws TrackedChangeTypeException, TrackedChangeException {
        switch ( fieldChangeRecordWithOriginalValues.changeType ) {
            case added:
                // if we have added another field, we do not really care how many values this has
                // because to get back to original document we delete this field name completely.
                logger.debug(
                        "Leaving existing added record alone due to this being another addition to an added field." );
                break;
            case updated:
                // if we have now updated the field from value a, to value b this isn't important as
                // we originally added the field, so any field will be removed.
                logger.debug(
                        "Leaving existing added record alone due to this being an update to an added field." );

                break;
            case deleted:
                // ok 2 things could happen here, either we have deleted the added field completely or we have deleted
                // a part of it.
                // So we need to know can we successfully delete the original deleted record, and this is only
                // possible if no key field remains to be deleted on the original document.
                if ( doesKeyExistOnDocument( fieldType, internalDocument, fieldName ) ) {
                    // key still exists, so keep the added record, and just exit.
                    logger.debug(
                            "Leaving existing added fieldChangeRecord alone due to this being an update to an deleted field but it hasn't deleted the entire field, only some values." );
                    break;
                }

                logger.debug(
                        "Removing added fieldChangeRecord alone due to all fields for this key, now having been removed." );

                // key no longer exists, so we dont need the deleted record, remove it.
                dataProcessingRecord.removeFieldChangeRecord( fieldType, fieldName, existingFieldChange );

                break;
            default:
                throw new TrackedChangeTypeException(
                        "Invalid newFieldChangeType: " + fieldChangeRecordWithOriginalValues.changeType );
        }
    }

    private static void addFieldChangesToValuesList( Collection<FieldValue> valuesToAdd,
                                                     FieldChangeRecord newUpdateRecord ) {
        // go through and add each previously deleted item, and each arationalizedAdditions item
        // to our new update values.
        if ( valuesToAdd == null || valuesToAdd.isEmpty() ) {
            return;
        }
        for ( FieldValue fieldChange : valuesToAdd ) {
            newUpdateRecord.changeValues.add( fieldChange );
        }
    }

    private static boolean doesKeyExistOnDocument( DocumentProcessingFieldType fieldType,
                                                   DocumentInterface internalDocument, String fieldName ) throws TrackedChangeTypeException {

        switch ( fieldType ) {
            case REFERENCE:
                // pretty much we always say a reference field exists, as its always there, even when set to null.
                // we cant actually delete the key.
                return true;

            case METADATA:
                return internalDocument.getMetadata().containsKey( fieldName );

            case METADATA_REFERENCE:
                return internalDocument.getMetadataReferences().containsKey( fieldName );

            default:
                throw new TrackedChangeTypeException( "Invalid document processing field type requested: " + fieldType );
        }
    }

    /**
     * Creates the first field change record, to ever be held about a field. this does nothing about merging records
     * together, only the simple case, when a record isnt' already here for this field.
     *
     * ADDED - a newly added field, only if the field didnt' already exist.  Just add the field name, we dont need original values.
     * UPDATED - an updated field which already existed, as such we need to record an updated record, will ALL original values, so we can
     * revert back.
     * the document back to this value.
     * DELETED - deleted values from an existing field, record only the actually changed fields here, not all of them!
     *
     * @param fieldType
     * @param fieldChangeRecordWithOriginalValues
     * @param fieldName
     * @param actualChangedValues
     * @return
     */
    private static FieldChangeRecord createFirstFieldChangeRecord( FieldChangeType fieldType,
                                                                   FieldChangeRecord fieldChangeRecordWithOriginalValues,
                                                                   String fieldName,
                                                                   Collection<?> actualChangedValues ) {
        FieldChangeRecord finalChangeRecord = new FieldChangeRecord();
        finalChangeRecord.changeType = fieldType;

        switch ( fieldType ) {
            case added:
                // Ok so we are adding some new fields, did the key already exist?
                // if not it is in fact an addition of some more values as such its actually an update record!
                if ( fieldChangeRecordWithOriginalValues.changeValues == null || fieldChangeRecordWithOriginalValues.changeValues.isEmpty() )
                {
                    // no values originally on the doucment - so record as an ADDED change type 
                    // Added changes, do not have changeValues recorded - so return early without any.
                    return finalChangeRecord;    
                }
                
                // we have a list of fields originally on the document, as such ignore the new values, and record
                // the originals as an UPDATE record.
                finalChangeRecord.changeType = FieldChangeType.updated;
                finalChangeRecord.changeValues = fieldChangeRecordWithOriginalValues.changeValues;
                return finalChangeRecord;

            case deleted:

                // set only the really deleted field on this record.
                finalChangeRecord.changeValues = FieldValueFactory.create( actualChangedValues );
                return finalChangeRecord;

            case updated:

                // finally the updated record, always holds all original values, although I would 
                // expect that the changedvalues and originalValues are the same in this case...
                finalChangeRecord.changeValues = fieldChangeRecordWithOriginalValues.changeValues;
                return finalChangeRecord;

            default:
                throw new InvalidParameterException( "Unexpected field change record type." + fieldType );
        }

    }

    private static DocumentProcessingRecord getOrCreateDataProcessingRecord( DocumentInterface internalDocument ) {
        DocumentProcessingRecord dataProcessingRecord = internalDocument.getPolicyDataProcessingRecord();
        if ( dataProcessingRecord == null ) {
            dataProcessingRecord = internalDocument.createPolicyDataProcessingRecord();
        }
        return dataProcessingRecord;
    }
}
