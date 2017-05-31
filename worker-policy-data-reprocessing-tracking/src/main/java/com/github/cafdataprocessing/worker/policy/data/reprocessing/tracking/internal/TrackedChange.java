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
package com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.internal;

import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.worker.policy.shared.DocumentProcessingFieldType;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.github.cafdataprocessing.worker.policy.shared.utils.CollectionConversion;
import com.github.cafdataprocessing.worker.policy.shared.utils.DocumentFieldChanger;
import com.github.cafdataprocessing.entity.fields.FieldChangeRecord;
import com.github.cafdataprocessing.entity.fields.FieldChangeType;
import com.github.cafdataprocessing.entity.fields.exceptions.FieldChangeTypeException;
import com.github.cafdataprocessing.entity.fields.factory.FieldChangeRecordFactory;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.exceptions.TrackedChangeException;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.exceptions.TrackedChangeTypeException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author trevor.getty@hpe.com
 */
class TrackedChange {

    protected final DocumentFieldChanger trackedDocumentChanger;

    // ensure noones creates an object of this type.
    private TrackedChange() {
        // maybe for unit testing.
        trackedDocumentChanger = null;
    }

    public TrackedChange( DocumentInterface document ) {
        trackedDocumentChanger = new DocumentFieldChanger( document );
    }

    public TrackedChange( DocumentFieldChanger document ) {
        trackedDocumentChanger = document;
    }

    /******************************************************************************************
     *
     * Public methods, add/set/delete value(s) are all Tracking methods, so come first
     * Then the get value(s) methods as after as they are just pass through methods.
     *
     ******************************************************************************************/
    
    /**
     * Add field value is used to update an existing field, with the supplied newValue.
     *
     * @param fieldName The name of the field, to add the value to.
     * @param newValue The new value to be added to the field specified.
     * @param fieldType The type of field the value is to be added as.
     * @return The Collection represented the changes which have been made, in this case the added field(s).
     */
    public Collection<?> addFieldValue( String fieldName,
                                  Object newValue,
                                        DocumentProcessingFieldType fieldType ) {

        // Check if this setFieldValue is going to actually make a change
        FieldChangeRecord fieldChangeRecord = shouldAddValue( fieldType, fieldName, newValue );

        if ( fieldChangeRecord == null ) {

            // no change required, just exit with false, so caller
            // knows we made no change.
            return null;
        }

        /**
         * Now what we have to do is 2 things.
         * 1) Make the actual change to the document metadata.
         * 2) Update our policyDataProcessingRecord with the FieldChangeRecord we recorded earlier, so dont
         * perform this update until we make the real change to the document JIC.
         */
        Collection<?> addedValues = trackedDocumentChanger.addFieldValue( fieldType, fieldName, newValue );

        
        // finally update the Data Processing Record with our fieldChangeRecord information.
        DataProcessingRecordImpl.updateProcessingRecord( fieldType, 
                                                         trackedDocumentChanger.getInternalDocument(),
                                                         fieldName, 
                                                         fieldChangeRecord, 
                                                         addedValues );

        return addedValues;

    }

    /**
     * addFieldValues is used to add map of field names / values to the document.
     * 
     * @param newValuesMap is a map of fields/values to be added to the document
     * @param fieldType is the DocumentProcessingFieldType used to add the values to the correct map e.g. metadata or reference
     * @return The Collection represented the changes which have been made, in this case the added fields.
     */
    public Collection<?> addFieldValues( Multimap newValuesMap,
                                   DocumentProcessingFieldType fieldType ) {

        // as this is a map, which can update several key / value pairs, we take the update
        // true status across all fields.  If any update happens, we return true.
        Collection<Object> addedValues = new ArrayList<>();
        
        Collection< Map.Entry<String, ?>> entries = newValuesMap.entries();

        // anything which changes the state of the map, needs to actually perform tracking on the field change!
        for ( Map.Entry<String, ?> entry : entries ) {
            String fieldName = entry.getKey();
            // this object may be a single object, or a collection or something, dont assume!
            Object newvalue = entry.getValue();

            // Check if this setFieldValue is going to actually make a change
            FieldChangeRecord fieldChangeRecord = shouldAddValue( fieldType, fieldName, newvalue );

            if ( fieldChangeRecord == null ) {

                // no change required, just exit
                continue;
            }

            /**
             * Now what we have to do is 2 things.
             * 1) Make the actual change to the document metadata.
             * 2) Update our policyDataProcessingRecord with the FieldChangeRecord we recorded earlier, so dont
             * perform this update until we make the real change to the document JIC.
             */
            Collection<?> tmpAddedValues = trackedDocumentChanger.addFieldValue( fieldType, fieldName, newvalue );

            // finally update the Data Processing Record with our fieldChangeRecord information.
            DataProcessingRecordImpl.updateProcessingRecord( fieldType, trackedDocumentChanger.getInternalDocument(),
                                                             fieldName, fieldChangeRecord, tmpAddedValues );

            // if we got any added values back, put them on our return map.
            if  (tmpAddedValues != null)
            {
                addedValues.addAll( tmpAddedValues );
            }
        }

        return addedValues;
    }

    /**
     * adds a collection of values, to the document using the fieldName specified.
     * 
     * @param fieldName name of the field on the document, the values should be added to.
     * @param newValues the values, to be added to the field specified.
     * @param fieldType the type of the document processing field, this value is to be added as: i.e. metadata or reference.
     * @return The Collection represented the changes which have been made, in this case the added fields.
     */
    public Collection<?> addFieldValues( String fieldName,
                                   Collection<?> newValues,
                                   DocumentProcessingFieldType fieldType ) {

        // Check if this setFieldValue is going to actually make a change
        FieldChangeRecord fieldChangeRecord = shouldAddValue( fieldType, fieldName, newValues );

        if ( fieldChangeRecord == null ) {

            // no change required, just exit
            return null;
        }

        /**
         * Now what we have to do is 2 things.
         * 1) Make the actual change to the document metadata.
         * 2) Update our policyDataProcessingRecord with the FieldChangeRecord we recorded earlier, so dont
         * perform this update until we make the real change to the document JIC.
         */
        Collection<?> addedValues = trackedDocumentChanger.addFieldValues( fieldType, fieldName, newValues );

        // finally update the Data Processing Record with our fieldChangeRecord information.
        DataProcessingRecordImpl.updateProcessingRecord( fieldType, trackedDocumentChanger.getInternalDocument(),
                                                         fieldName, fieldChangeRecord, addedValues );

        return addedValues;

    }

    /**
     * Set field value is used to update an existing value, with the given one.
     *
     * @param fieldName the name of the field to be set to the values specified.
     * @param newValue the value as an object, of the value to be set.
     * @param fieldType the type of the field being represented.
     * @return The collection of fields changed on the document, in this case its the original values on the field, 
     * before they were replaced with the new field values.
     */
    public Collection<?> setFieldValue( String fieldName,
                                  Object newValue,
                                  DocumentProcessingFieldType fieldType ) {

        // Check if this setFieldValue is going to actually make a change
        FieldChangeRecord fieldChangeRecord = shouldSetValue( fieldType, fieldName, newValue );

        if ( fieldChangeRecord == null ) {

            // no change required, just exit
            return null;
        }

        /**
         * Now what we have to do is 2 things.
         * 1) Make the actual change to the document metadata.
         * 2) Update our policyDataProcessingRecord with the FieldChangeRecord we recorded earlier, so dont
         * perform this update until we make the real change to the document JIC.
         */
        Collection updatedValues = trackedDocumentChanger.setFieldValue( fieldType, fieldName, newValue );

        // finally update the Data Processing Record with our fieldChangeRecord information.
        DataProcessingRecordImpl.updateProcessingRecord( fieldType, trackedDocumentChanger.getInternalDocument(),
                                                         fieldName, fieldChangeRecord, updatedValues );

        return updatedValues;
    }

     /**
     * Set field value is used to update an existing value, with the given value(s).
     *
     * @param fieldName the name of the field to be set to the values specified.
     * @param newValues A collection of values to be set on the field specified.
     * @param fieldType the type of the field being represented.
     * @return The collection of fields changed on the document, in this case its the original values on the field, 
     * before they were replaced with the new field values.
     */
    public Collection<?> setFieldValues( String fieldName,
                                   Collection<?> newValues,
                                   DocumentProcessingFieldType fieldType ) throws TrackedChangeTypeException, TrackedChangeException, FieldChangeTypeException {

        // Check if this setFieldValue is going to actually make a change
        FieldChangeRecord fieldChangeRecord = shouldSetValue( fieldType, fieldName, newValues );

        if ( fieldChangeRecord == null ) {

            // no change required, just exit
            return null;
        }

        /**
         * Now what we have to do is 2 things.
         * 1) Make the actual change to the document metadata.
         * 2) Update our policyDataProcessingRecord with the FieldChangeRecord we recorded earlier, so dont
         * perform this update until we make the real change to the document JIC.
         */
        Collection<?> updatedValues = trackedDocumentChanger.setFieldValue( fieldType, fieldName,
                                                                         newValues );

        // finally update the Data Processing Record with our fieldChangeRecord information.
        DataProcessingRecordImpl.updateProcessingRecord( fieldType, trackedDocumentChanger.getInternalDocument(),
                                                         fieldName, fieldChangeRecord, updatedValues );
        return updatedValues;

    }
    
    /**
     * Set field value is used to update an existing value, with the given value(s).
     *
     * @param fieldName the name of the field to be set to the values specified.
     * @param newValues the values as an Iterable, of the values to be set.
     * @param fieldType the type of the field being represented.
     * @return The collection of fields changed on the document, in this case its the original values on the field, 
     * before they were replaced with the new field values.
     */
    public Collection<?> setFieldValues( String fieldName,
                                   Iterable newValues,
                                   DocumentProcessingFieldType fieldType ) throws TrackedChangeTypeException, TrackedChangeException, FieldChangeTypeException {

        // Check if this setFieldValue is going to actually make a change
        FieldChangeRecord fieldChangeRecord = shouldSetValue( fieldType, fieldName, newValues );

        if ( fieldChangeRecord == null ) {

            // no change required, just exit
            return null;
        }

        /**
         * Now what we have to do is 2 things.
         * 1) Make the actual change to the document metadata.
         * 2) Update our policyDataProcessingRecord with the FieldChangeRecord we recorded earlier, so dont
         * perform this update until we make the real change to the document JIC.
         */
        Collection<?> updatedValues = trackedDocumentChanger.setFieldValues( fieldType, fieldName, newValues );

        // finally update the Data Processing Record with our fieldChangeRecord information.
        DataProcessingRecordImpl.updateProcessingRecord( fieldType, trackedDocumentChanger.getInternalDocument(),
                                                         fieldName, fieldChangeRecord, updatedValues );
        return updatedValues;

    }

    /**
     * Delete field values that are matching the specified value.
     * @param fieldName the name of the field, the values should be deleted from.
     * @param value the values to be deleted from the specified field
     * @param fieldType the type of the document processing fields specified.
     * @return The Collection represented the changes which have been made, in this case the deleted fields.
     */
    public Collection<?> deleteFieldValue( String fieldName,
                                     Object value,
                                     DocumentProcessingFieldType fieldType ) {

        // Check if this setFieldValue is going to actually make a change
        FieldChangeRecord fieldChangeRecord = shouldDeleteValue( fieldType, fieldName, value );

        if ( fieldChangeRecord == null ) {

            // no change required, just exit
            return null;
        }

        /**
         * Now what we have to do is 2 things.
         * 1) Make the actual change to the document metadata.
         * 2) Update our policyDataProcessingRecord with the FieldChangeRecord we recorded earlier, so dont
         * perform this update until we make the real change to the document JIC.
         */
        Collection<?> deletedValues = trackedDocumentChanger.deleteFieldValue( fieldType, fieldName, value );

        // finally update the Data Processing Record with our fieldChangeRecord information.
        DataProcessingRecordImpl.updateProcessingRecord( fieldType, trackedDocumentChanger.getInternalDocument(),
                                                         fieldName, fieldChangeRecord, deletedValues );

        return deletedValues;
    }

    /**
     * Delete field values that are matching the specified value(s)
     * @param fieldName the name of the field, the values should be deleted from.
     * @param values the values to be deleted from the specified field
     * @param fieldType the type of the document processing fields specified.
     * @return The Collection represented the changes which have been made, in this case the deleted fields.
     * @throws TrackedChangeTypeException The type of field requested is not valid.
     */
    public Collection<?> deleteFieldValues( String fieldName,
                                      Collection<?> values,
                                      DocumentProcessingFieldType fieldType ) throws TrackedChangeTypeException, TrackedChangeException, FieldChangeTypeException {

        // Check if this setFieldValue is going to actually make a change
        FieldChangeRecord fieldChangeRecord = shouldDeleteValue( fieldType, fieldName, values );

        if ( fieldChangeRecord == null ) {

            // no change required, just exit
            return null;
        }

        /**
         * Now what we have to do is 2 things.
         * 1) Make the actual change to the document metadata.
         * 2) Update our policyDataProcessingRecord with the FieldChangeRecord we recorded earlier, so dont
         * perform this update until we make the real change to the document JIC.
         */
        Collection<?> deletedValues = trackedDocumentChanger.deleteFieldValues( fieldType, fieldName, values );

        // finally update the Data Processing Record with our fieldChangeRecord information.
        DataProcessingRecordImpl.updateProcessingRecord( fieldType, trackedDocumentChanger.getInternalDocument(),
                                                         fieldName, fieldChangeRecord, deletedValues );
        
        return deletedValues;
    }
    
     /**
     * Delete all field values that have the given fieldName.
     * 
     * @param fieldName the name of the field, the values should be deleted from.
     * @param fieldType the type of the document processing fields specified.
     * @return The Collection represented the changes which have been made, in this case the deleted fields.
     */
    public Collection<?> deleteFieldValues( String fieldName,
                                      DocumentProcessingFieldType fieldType ) throws TrackedChangeTypeException, TrackedChangeException, FieldChangeTypeException {

        // Check if this setFieldValue is going to actually make a change
        FieldChangeRecord fieldChangeRecord = shouldDeleteValue( fieldType, fieldName );

        if ( fieldChangeRecord == null ) {

            // no change required, just exit
            return null;
        }

        /**
         * Now what we have to do is 2 things.
         * 1) Make the actual change to the document metadata.
         * 2) Update our policyDataProcessingRecord with the FieldChangeRecord we recorded earlier, so dont
         * perform this update until we make the real change to the document JIC.
         */
        Collection<?> deletedFieldValues = trackedDocumentChanger.deleteFieldValues( fieldType, fieldName );

        // finally update the Data Processing Record with our fieldChangeRecord information.
        DataProcessingRecordImpl.updateProcessingRecord( fieldType, trackedDocumentChanger.getInternalDocument(),
                                                         fieldName, fieldChangeRecord, deletedFieldValues );
        return deletedFieldValues;

    }

    /*
     * Non tracking public methods get value(s) are just pass through to get the values.
     */
    
    /**
     * Get the field value from the document, using the field name specified.
     * @param fieldType the type of the processing fields requested.
     * @param fieldName the name of the field to be requested.
     * @return the Object representing the field value requested
     */
    public Object getFieldValue( DocumentProcessingFieldType fieldType,
                                 String fieldName ) {
        return trackedDocumentChanger.getFieldAsObject( fieldType, fieldName );
    }

    /**
     * Get the field value from the document, using the field name specified.
     * @param fieldType the type of the processing fields requested.
     * @param fieldName the name of the field to be requested.
     * @return a Collection of values representing the field requested.
     */
    public Collection<?> getFieldValues( DocumentProcessingFieldType fieldType,
                                         String fieldName ) throws TrackedChangeTypeException {
        return trackedDocumentChanger.getFieldValues( fieldType, fieldName );
    }

    /**
     * Get the field value from the document, using the field name specified and returns its value as a string.
     * @param fieldType the type of the processing fields requested.
     * @param fieldName the name of the field to be requested.
     * @return a String representing the value of the field requested.
     */
    public String getFieldValueAsString( DocumentProcessingFieldType fieldType,
                                         String fieldName ) throws TrackedChangeTypeException, TrackedChangeException {
        return trackedDocumentChanger.getFieldValueAsString( fieldType, fieldName );
    }

    /**
     * ****************************************************
     * Start of Private internal methods.
     *****************************************************
     */
    /**
     * Internal method, to check if a setValue call should be performed or not.
     * We are either going:
     * Do nothing as its already at the given value.
     * Update the value as its a different value.
     *
     * @param fieldType
     * @param internalDocument
     * @param fieldName
     * @param newValue
     * @return
     * @throws TrackedChangeTypeException
     * @throws TrackedChangeException
     */
    private FieldChangeRecord shouldSetValue( DocumentProcessingFieldType fieldType,
                                              String fieldName,
                                              Object newValue
    ) throws TrackedChangeTypeException, TrackedChangeException, FieldChangeTypeException {

        return shouldChangeValue( fieldType, FieldChangeType.updated, fieldName, newValue );
    }

    /**
     * shoulSetValue is wrapper around shouldchangevalue for updated fields.
     *
     * @param fieldType
     * @param internalDocument
     * @param fieldName
     * @param newValues
     * @return
     */
    private FieldChangeRecord shouldSetValue( DocumentProcessingFieldType fieldType,
                                              String fieldName,
                                              Collection<?> newValues ) throws TrackedChangeTypeException, TrackedChangeException, FieldChangeTypeException {
        return shouldChangeValue( fieldType, FieldChangeType.updated, fieldName, newValues );
    }

    /**
     * shouldAddValue is wrapper around shouldchangevalue for added fields.
     *
     * @param fieldType
     * @param internalDocument
     * @param fieldName
     * @param newValue
     * @return
     * @throws TrackedChangeTypeException
     * @throws TrackedChangeException
     * @throws FieldChangeTypeException
     */
    private FieldChangeRecord shouldAddValue( DocumentProcessingFieldType fieldType,
                                              String fieldName,
                                              Object newValue
    ) throws TrackedChangeTypeException, TrackedChangeException, FieldChangeTypeException {

        return shouldChangeValue( fieldType, FieldChangeType.added, fieldName, newValue );
    }

    /**
     * Wrapper around shouldchangevalue for added fields.
     *
     * @param fieldType
     * @param internalDocument
     * @param fieldName
     * @param newValue
     * @return
     * @throws TrackedChangeTypeException
     * @throws TrackedChangeException
     */
    private FieldChangeRecord shouldAddValue( DocumentProcessingFieldType fieldType,
                                              String fieldName,
                                              Collection<?> newValues
    ) throws TrackedChangeTypeException, TrackedChangeException, FieldChangeTypeException {

        return shouldChangeValue( fieldType, FieldChangeType.added, fieldName, newValues );
    }

    /**
     * shouldAddValue is wrapper around shouldchangevalue for added fields.
     *
     * @param fieldType
     * @param internalDocument
     * @param fieldName
     * @param newValue
     * @return
     * @throws TrackedChangeTypeException
     * @throws TrackedChangeException
     * @throws FieldChangeTypeException
     */
    private FieldChangeRecord shouldDeleteValue( DocumentProcessingFieldType fieldType,
                                                 String fieldName 
    ) throws TrackedChangeTypeException, TrackedChangeException, FieldChangeTypeException {

        return shouldChangeValue( fieldType, FieldChangeType.deleted, fieldName, null );
    }

    /**
     * Wrapper around shouldchangevalue for added fields.
     *
     * @param fieldType
     * @param internalDocument
     * @param fieldName
     * @param newValue
     * @return
     * @throws TrackedChangeTypeException
     * @throws TrackedChangeException
     */
    private FieldChangeRecord shouldDeleteValue( DocumentProcessingFieldType fieldType,
                                                 String fieldName,
                                                 Collection<?> values
    ) throws TrackedChangeTypeException, TrackedChangeException, FieldChangeTypeException {

        return shouldChangeValue( fieldType, FieldChangeType.deleted, fieldName, values );
    }

    /**
     * 
     * @param fieldType
     * @param fieldName
     * @param values
     * @return
     * @throws TrackedChangeTypeException
     * @throws TrackedChangeException
     * @throws FieldChangeTypeException 
     */
    private FieldChangeRecord shouldDeleteValue( DocumentProcessingFieldType fieldType,
                                                 String fieldName,
                                                 Object values
    ) throws TrackedChangeTypeException, TrackedChangeException, FieldChangeTypeException {

        return shouldChangeValue( fieldType, FieldChangeType.deleted, fieldName, values );
    }

    
    /**
     * Internal method, to check if a change value call should be performed or not.
     *
     * @param fieldType
     * @param internalDocument
     * @param fieldName
     * @param newValue
     * @return
     * @throws TrackedChangeTypeException
     * @throws TrackedChangeException
     */
    private FieldChangeRecord shouldChangeValue( DocumentProcessingFieldType fieldType,
                                                 FieldChangeType typeOfChangeBeingPerformed,
                                                 String fieldName,
                                                 Object value ) {

        Collection<?> originalValues = getFieldValues( fieldType, fieldName );

        // decide if its a change or not.
        FieldChangeType changeType = TrackedChange.decideIfFieldChangeIsOccurring( originalValues, value, typeOfChangeBeingPerformed, null );

        if ( changeType == null ) {
            // there is no change required, return false.
            return null;
        }

        return FieldChangeRecordFactory.create( changeType, originalValues );
    }

    /**
     * Internal method, to check if a setValue call should be performed or not.
     * We are either going:
     * Do nothing as its already at the given value.
     * Update the value as its a different value.
     *
     * @param fieldType
     * @param internalDocument
     * @param fieldName
     * @param newValues
     * @return
     * @throws TrackedChangeTypeException
     * @throws TrackedChangeException
     */
    private FieldChangeRecord shouldChangeValue( DocumentProcessingFieldType fieldType,
                                                 FieldChangeType typeOfChangeBeingPerformed,
                                                 String fieldName,
                                                 Collection<?> values ) {

        Collection<?> originalValues = getFieldValues( fieldType, fieldName );

        // decide if its a change or not.
        FieldChangeType changeType = TrackedChange.decideIfFieldChangeIsOccurring( originalValues, values, typeOfChangeBeingPerformed, null );

        if ( changeType == null ) {
            // there is no change required, return false.
            return null;
        }

        return FieldChangeRecordFactory.create( changeType, originalValues );
    }

    /**
     * *
     * Utilitiy method, to decide if a change being made to a field is in fact
     * going to perform an update/delete/addition or leave the value as is.
     * 
     * DO NOT DECIDE the final type of fieldchangerecord to create based on this change!
     * This only informs the tracking code, that the user has requested an add / delete and a change is to take place.
     * Not what the final data Processing record to be record is!  
     * This is important as an addition of value x, may actually be recorded as an update record!!!
     *
     * @param originalValue
     * @param newValue
     * @param operationType
     * @param equality
     * @return Returns the FieldChangeType if a change is to take place, otherwise it returns null, if no change.
     * @throws TrackedChangeTypeException
     */
    private static FieldChangeType decideIfFieldChangeIsOccurring( String originalValue,
                                                                String newValue,
                                                                FieldChangeType operationType,
                                                                Boolean equality ) throws TrackedChangeTypeException {

        // either get or use the equality already supplied.
        boolean equals = getEquality( equality, originalValue, newValue );

        switch ( operationType ) {
            case added:
                // if someone adds a value, doesn't matter, just record an addition.
                if ( equals && newValue == null ) {
                    return null;
                }

                return FieldChangeType.added;

            case deleted:
                // if someone wants to delete a value x, if its there indicate 
                // deleted, otherwise return nothing will happen.
                return equals ? FieldChangeType.deleted : null;

            case updated:
                // if someone wants to update a field to value x, if its same 
                // value indicate no change, otherwise its an update.
                return equals ? null : FieldChangeType.updated;

            default:
                throw new TrackedChangeTypeException( "Unknown operation type: " + operationType );
        }
    }

    /**
     * Utility wrapping the decideIfFieldChangeIsOccurring taking collection of values, to make it easier to call for base types.
     * 
     * DO NOT DECIDE the final type of fieldchangerecord to create based on this change!
     * This only informs the tracking code, that the user has requested an add / delete and a change is to take place.
     * Not what the final data Processing record to be record is!  
     * This is important as an addition of value x, may actually be recorded as an update record!!!
     *
     * @param originalValues
     * @param newValue
     * @param operationType
     * @param equality
     * @return
     * @throws TrackedChangeTypeException
     */
    private static FieldChangeType decideIfFieldChangeIsOccurring( Collection<?> originalValues,
                                                                Object newValue,
                                                                FieldChangeType operationType,
                                                                Boolean equality ) throws TrackedChangeTypeException {

        if ( CollectionConversion.isObjectPlural( newValue ) ) {
            // we have been given a plural newValue object, use the collection plural 
            // decideFieldChangeMethod
            Collection<?> values = CollectionFactory.toCollection( newValue );
            return TrackedChange.decideIfFieldChangeIsOccurring( originalValues, values, operationType, equality );
        }

        // TODO I think we can now remove this equality check - always return the record, and instead
        // TODO use the changedFields list, which contains anything which has been really added/replaced/deleted
        
        // either get or use the equality already supplied.
        boolean equals = getEquality( equality, originalValues, newValue );

        switch ( operationType ) {
            case added:
                // if someone adds a value, doesn't matter, just record an addition.
                if ( equals && newValue == null ) {
                    return null;
                }

                return FieldChangeType.added;

            case deleted:
                // if someone wants to delete a value x, if its there indicate 
                // deleted, otherwise return nothing.
                // now if the original is more than 1 value say x,y and someone deletes x, 
                // then it is still a partial delete.
                // we could be instructed to delete all fields, in which case newValue is null.
                if ( newValue == null )
                {
                    // if there are any fields on the document i.e. originalValue, indicate deleted, 
                    // otherwise we have nothing to do as its already empty.
                    return ( originalValues != null && !originalValues.isEmpty() ) ? FieldChangeType.deleted : null;
                }
                
                // its a dead match, so delete it.
                if ( equals ) {
                    return FieldChangeType.deleted;
                }
                
                // finally partial match return deleted, or null as nothing needs to happen
                boolean anyItemsMatch = getAnyItemsEqual( originalValues, newValue );
                return anyItemsMatch ? FieldChangeType.deleted : null;
                
            case updated:
                // if someone wants to update a field to value x, if its same 
                // value indicate no change, otherwise its an update.
                return equals ? null : FieldChangeType.updated;

            default:
                throw new TrackedChangeTypeException( "Unknown operation type: " + operationType );
        }
    }

    /**
     * *
     * Utility method, to decide if a change being made to a field is in fact
     * going to perform an update/delete/addition or leave the value as is.
     * 
     * DO NOT DECIDE the final type of fieldchangerecord to create based on this change!
     * This only informs the tracking code, that the user has requested an add / delete and a change is to take place.
     * Not what the final data Processing record to be record is!  
     * This is important as an addition of value x, may actually be recorded as an update record!!!
     *
     * @param originalValues
     * @param newValues
     * @param operationType
     * @param equality
     * @return
     * @throws TrackedChangeTypeException
     */
    private static FieldChangeType decideIfFieldChangeIsOccurring( Collection<?> originalValues,
                                                                Collection<?> newValues,
                                                                FieldChangeType operationType,
                                                                Boolean equality ) throws TrackedChangeTypeException {

        // either get or use the equality already supplied.
        boolean equals = getEquals( equality, originalValues, newValues );

        // also depending upon the operation, we may want to check if any of the items matched.
        switch ( operationType ) {
            case added:
                // if someone adds a value, doesn't matter, just record an addition.
                // unless its already null, and new value is null, consider no change
                if ( ( newValues == null || newValues.isEmpty() ) && equals ) {
                    return null;
                }

                return FieldChangeType.added;

            case deleted:
                // if someone wants to delete a value x, if its there indicate 
                // deleted, otherwise return nothing.
                // now if the original is more than 1 value say x,y and someone deletes x, 
                // then it is still a partial delete.
                // we could be instructed to delete all fields, in which case newValues is null.
                if ( newValues == null )
                {
                    // if there are any fields on the document i.e. originalValue, indicate deleted, 
                    // otherwise we have nothing to do as its already empty.
                    return ( originalValues != null && !originalValues.isEmpty() ) ? FieldChangeType.deleted : null;
                }
                
                if ( equals ) {
                    return FieldChangeType.deleted;
                }
                boolean anyItemsMatch = getAnyItemsEqual( originalValues, newValues );

                return anyItemsMatch ? FieldChangeType.deleted : null;

            case updated:
                // if someone wants to update a field to value x,y if its same 
                // value indicate no change, otherwise its an update.
                return equals ? null : FieldChangeType.updated;

            default:
                throw new TrackedChangeTypeException( "Unknown operation type: " + operationType );
        }
    }

    private static boolean getAnyItemsEqual( Collection<?> originalValues, Object newValue ) throws TrackedChangeTypeException {

        if ( ( originalValues == null || originalValues.isEmpty() ) && ( newValue == null ) ){
            // if they are both empty - then they are equal.
            return true;
        }
        
        if ( ( originalValues == null || originalValues.isEmpty() ) || ( newValue == null ) ) {
            // we can't do any items equal comparison.
            return false;
        }

        // search in the collection for this new object value.
        return originalValues.contains( newValue );
    }
     
    private static boolean getAnyItemsEqual( Collection<?> originalValues, Collection<?> newValues ) throws TrackedChangeTypeException {

        if ( ( originalValues == null || originalValues.isEmpty() ) && ( newValues == null || newValues.isEmpty() ) ){
            // if they are both empty - then they are equal.
            return true;
        }
            
        // if one is empty and the other not, they aren't equal.
        if ( ( originalValues == null || originalValues.isEmpty() ) || ( newValues == null || newValues.isEmpty() ) ) {
            // we can't do any items equal comparison, so not equal
            return false;
        }

        for ( Object value : originalValues ) {
            if ( newValues.contains( value ) ) {
                // we got a value match, we dont have to continue.
                return true;
            }
        }

        return false;
    }

    private static boolean getEquals( Boolean existingEqualityComparison, Collection<?> originalValues,
                                      Collection<?> newValues ) throws TrackedChangeTypeException {

        // check if we need to just return an existin existingEqualityComparison value, or get 
        // a new one.
        if ( existingEqualityComparison != null ) {
            return existingEqualityComparison;
        }

        if ( originalValues == null ) {
            // if both are null, return equals
            return ( newValues == null );
        }

        if ( newValues == null ) {
            return false;
        }

        // no existingEqualityComparison has been provided, so work it out now.
        // This will validate the entire collection, if it happens to be one, or
        // contents of just a single object, if its just a string.
        return originalValues.equals( newValues );
    }

    private static boolean getEquality( Boolean equality, Object originalValue, Object newValue ) throws TrackedChangeTypeException {

        // check if we need to just return an existin equality value, or get 
        // a new one.
        if ( equality != null ) {
            return equality.booleanValue();
        }

        // if original value is null, and newvalue is null then true.
        // if original value is null, and newvalue is not null then false.
        // if original value is not null then equals newValue return true
        // if original value is not null then !equals newValue return false
        if (!(originalValue==null ? newValue==null : originalValue.equals(newValue)))
        {
            return false;
        }
        
        return true;         
    }
    
    private static boolean getEquality( Boolean equality, String originalValue, String newValue ) throws TrackedChangeTypeException {

        // check if we need to just return an existin equality value, or get 
        // a new one.
        if ( equality != null ) {
            return equality.booleanValue();
        }

        // no equality has been provided, so work it out now.
        return StringCompare.equals( originalValue, newValue );
    }
}
