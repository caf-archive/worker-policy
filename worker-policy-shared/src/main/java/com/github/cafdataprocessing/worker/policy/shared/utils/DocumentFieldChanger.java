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
package com.github.cafdataprocessing.worker.policy.shared.utils;

import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.google.common.base.Strings;
import com.github.cafdataprocessing.worker.policy.shared.DocumentProcessingFieldCategory;
import com.github.cafdataprocessing.worker.policy.shared.DocumentProcessingFieldType;
import com.github.cafdataprocessing.entity.fields.FieldEncoding;
import com.github.cafdataprocessing.entity.fields.FieldValue;
import com.github.cafdataprocessing.entity.fields.accessor.FieldValueAccessor;
import com.hpe.caf.util.ref.ReferencedData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author trevor
 */
public class DocumentFieldChanger {

    private final DocumentInterface internalDocument;

    public DocumentFieldChanger( DocumentInterface documentToBeChanged ) {
        internalDocument = documentToBeChanged;
    }

    public Object getFieldAsObject( DocumentProcessingFieldType fieldType, String fieldName ) {
        switch ( fieldType ) {
            case REFERENCE:
                return internalDocument.getReference();
            case METADATA:
                return internalDocument.getMetadata().get( fieldName );
            case METADATA_REFERENCE:
                return internalDocument.getMetadataReferences().get( fieldName );
            default:
                throw new RuntimeException( "Unknown DocumentProcessingFieldType: " + fieldType );
        }
    }

    public Collection<?> getFieldValues( DocumentProcessingFieldType fieldType, String fieldName ) {
        switch ( fieldType ) {
            case REFERENCE:
                return Collections.singletonList( internalDocument.getReference() );
            case METADATA:
                return internalDocument.getMetadata().get( fieldName );
            case METADATA_REFERENCE:
                return internalDocument.getMetadataReferences().get( fieldName );
            default:
                throw new RuntimeException( "Unknown DocumentProcessingFieldType: " + fieldType );
        }
    }

    public String getFieldValueAsString( DocumentProcessingFieldType fieldType,
                                         String fieldName ) {

        Object value = getFieldAsObject( fieldType, fieldName );

        // user has requested this as a string, for now only return string types.
        if ( value == null ) {
            return null;
        }

        if ( value instanceof String ) {
            // just return value as is   
            return ( String ) value;
        }

        if ( value instanceof FieldValue ) {
            return FieldValueAccessor.createStringFromFieldValue( ( FieldValue ) value );
        }

        if ( CollectionConversion.isObjectPlural( value ) ) {
            // if the object is plural we can get it as a collection, return first member as the string
            // if that is what the caller wanted?
            Collection<String> values = CollectionConversion.createAsStringCollection( value );

            if ( values != null && values.size() > 1 ) {
                // we have an issue, someone requested the value as a string, but we have more than 1.
                throw new RuntimeException(
                        "Invalid request for getFieldValueAsString as value of object, has more than 1 member.  numValues: " + values.size() );
            }

            return CollectionConversion.getFirstValue( values );
        }
        // throw as not correct type for now, maybe could convert to that type later on?
        throw new RuntimeException( "Invalid request for String response on object type: " + value.getClass().getName() );
    }

    public Collection<?> setFieldValues( DocumentProcessingFieldType fieldType,
                                         String fieldName,
                                         Iterable newValues ) {

        switch ( fieldType ) {
            case REFERENCE: {
                // ignore any replace on reference, its always just a set.
                Collection<String> originalRef
                        = CollectionConversion.createAsStringCollection( internalDocument.getReference() );

                Collection<String> values = CollectionConversion.createAsStringCollection( newValues );
                String decodedValue = CollectionConversion.getFirstValue( values );
                internalDocument.setReference( decodedValue );
                return originalRef;
            }
            case METADATA: {
                // this is setFieldValues, so removes all existing values, for these new ones.
                // I am using createAsStringCollection just in case someone is calling replaceValues but using a
                // different representation of the value e.g. String("abc") could also be FieldValue->value("abc")
                Collection<String> decodedValues = CollectionConversion.createAsStringCollection( newValues );

                // if for some reason I shouldn't be decoding the values first - lets check and re-run tests using the 
                // source iterator.
                return internalDocument.getMetadata().replaceValues( fieldName, decodedValues );
            }
            case METADATA_REFERENCE: {
                // we do not monitor the actual deletion of references, this the
                // callers responsibility, we only track the actual change here.
                Collection<ReferencedData> decodedValues = CollectionConversion.createAsReferencedDataCollection(
                        newValues);
                return internalDocument.getMetadataReferences().replaceValues( fieldName,
                                                                               decodedValues );
            }
            default:
                throw new RuntimeException( "Unknown DocumentProcessingFieldType: " + fieldType );
        }
    }

    public Collection<?> setFieldValues( DocumentProcessingFieldType fieldType,
                                   String fieldName,
                                   Collection<?> newValues ) {
        
        switch ( fieldType ) {
            case REFERENCE: {
                // ignore any replace on reference, its always just a set.
                Collection<String> originalRef
                        = CollectionConversion.createAsStringCollection(internalDocument.getReference());

                Collection<String> values = CollectionConversion.createAsStringCollection(newValues);
                String decodedValue = CollectionConversion.getFirstValue( values );
                internalDocument.setReference( decodedValue );
                if( ( originalRef != null ) && !originalRef.isEmpty() )
                {
                    return originalRef;
                }
                
                return null;
            }
            case METADATA: {
                // this is setFieldValues, so removes all existing values, for these new ones.
                // I am using createAsStringCollection just in case someone is calling replaceValues but using a
                // different representation of the value e.g. String("abc") could also be FieldValue->value("abc")
                Collection<String> decodedValues = CollectionConversion.createAsStringCollection( newValues );

                // if for some reason I shouldn't be decoding the values first - lets check and re-run tests using the 
                // source iterator.
                Collection<String> replacedValues = internalDocument.getMetadata().replaceValues(fieldName, decodedValues);
                return replacedValues;
            }
            case METADATA_REFERENCE: {
                // we do not monitor the actual deletion of CAF-Storage references, this the
                // callers responsibility, we only track the actual change here.
                Collection<ReferencedData> decodedValues = CollectionConversion.createAsReferencedDataCollection(
                        newValues );
                Collection<ReferencedData> replacedValues = internalDocument.getMetadataReferences().replaceValues(
                        fieldName,
                        decodedValues);
                return replacedValues;
            }
            default:
                throw new RuntimeException( "Unknown DocumentProcessingFieldType: " + fieldType );
        }
    }

    public Collection<?> setFieldValue( DocumentProcessingFieldType fieldType,
                                  String fieldName,
                                  Object newValue ) {

        // check if the object given is actually multiple values, like a collection if so
        // call setFieldValues instead.
        if ( CollectionConversion.isObjectPlural( newValue ) ) {
            Collection<?> collectionRepresentation = CollectionConversion.createCollectionFromPluralObject( newValue );
            return setFieldValues( fieldType, fieldName, collectionRepresentation );
        }

        switch ( fieldType ) {
            case REFERENCE: {
                // ignore any replace on reference, its always just a set.
                String decodedValue = CollectionConversion.createAsString(newValue);
                
                String originalRef = internalDocument.getReference();
                internalDocument.setReference( decodedValue );
                
                // if we have no original value, we need to return an empty list instead of null, so if we need to we can treat
                // differently, as this is not the same as having no key - we always have the reference key, its only null.
                if (Strings.isNullOrEmpty( originalRef ) )
                {
                    return new ArrayList<>();
                }
                
                if ( originalRef.equals ( decodedValue ))
                {
                    // actually they are the same value - exit with no change.
                    return null;
                }
                
                // replaced values are originalRef.
                return CollectionConversion.createAsStringCollection(originalRef);
            }
            case METADATA: {
                // this is setFieldValues, so removes all existing values, for these new ones.
                Collection<String> decodedValue = CollectionConversion.createAsStringCollection(newValue);
                return internalDocument.getMetadata().replaceValues( fieldName, decodedValue );
            }
            case METADATA_REFERENCE: {
                // we do not monitor the actual deletion of CAF-Storage references, this the
                // callers responsibility, we only track the actual change here.
                Collection<ReferencedData> decodedValues = CollectionConversion.createAsReferencedDataCollection( newValue );
                
                internalDocument.getMetadataReferences().replaceValues( fieldName, decodedValues );
            }
            default:
                throw new RuntimeException( "Unknown DocumentProcessingFieldType: " + fieldType );
        }
    }

    public Collection<?> addFieldValues(DocumentProcessingFieldCategory fieldCategory, String fieldName,
                                        Collection<?> newValues){
        switch(fieldCategory){
            case REFERENCE: {
                return addFieldValues(DocumentProcessingFieldType.REFERENCE, fieldName, newValues);
            }
            case ALLMETADATA: {
                //check newValues to see if it contains any entries that should be added to MetadataReferences.
                //If there are then we will consider all of these as metadata references to be added.
                boolean hasMetadataReferences = checkIfShouldBeMetadataReferences(newValues);
                if(hasMetadataReferences){
                    return addFieldValues(DocumentProcessingFieldType.METADATA_REFERENCE, fieldName, newValues);
                }
                return addFieldValues(DocumentProcessingFieldType.METADATA, fieldName, newValues);
            }
            default:
                throw new RuntimeException("Unknown DocumentProcessingFieldCategory: " + fieldCategory);
        }
    }

    /**
     * Checks if a given Collection of field values contains any entries that should be represented as a Metadata
     * Reference.
     * @param newValues The FieldValues Collection to check.
     * @return Flag indicating whether any FieldValue in the passed in Collection represents a Metadata Reference.
     */
    private static boolean checkIfShouldBeMetadataReferences(Collection<?> newValues){
        
        if ( newValues == null )
        {
            return false;
        }
        
        for(Object value : newValues){
            if(value instanceof ReferencedData){
                return true;
            }
            if (!(value instanceof FieldValue)) {
                continue;
            }
            FieldValue fieldValue = (FieldValue)value;
            if(fieldValue.valueEncoding == FieldEncoding.base64 ||
                    fieldValue.valueEncoding == FieldEncoding.caf_storage_reference){
                return true;
            }
        }
        return false;
    }

    public Collection<?> addFieldValues( DocumentProcessingFieldType fieldType,
                                   String fieldName,
                                   Collection<?> newValues ) {

        switch ( fieldType ) {
            case REFERENCE: {
                // treat add as set on reference field.
                Collection<String> values = CollectionConversion.createAsStringCollection(newValues);
                String decodedValue = CollectionConversion.getFirstValue(values);
                internalDocument.setReference( decodedValue );
                
                // if values size is 1, return it, otherwise, return a new collection containing only decoded value.
                if( values == null || values.isEmpty() || (values.size() == 1) )
                {
                    return values;
                }
                
                // create a collection with only this value
                return CollectionConversion.createAsStringCollection( decodedValue );
            }

            case METADATA: {
                // this is addFieldValues, so just add new values onto existing data
                Collection<String> values = CollectionConversion.createAsStringCollection(newValues);
                
                // we can either change createAsStringCollection to return empty collection ,but there is other 
                // code which can distinguish between null collection and empty collection.
                if ( values == null )
                {
                    return null;
                }
                
                if (internalDocument.getMetadata().putAll( fieldName, values ) )
                {
                    return values;
                }
                
                return null;
            }

            case METADATA_REFERENCE: {
                // this is addFieldValues, so just add new values onto existing data
                Collection<ReferencedData> values = CollectionConversion.createAsReferencedDataCollection(newValues);
                
                // we can either change createAsStringCollection to return empty collection ,but there is other 
                // code which can distinguish between null collection and empty collection.
                if ( values == null )
                {
                    return null;
                }
                
                if ( internalDocument.getMetadataReferences().putAll( fieldName, values ) )
                {
                    return values;
                }
                
                return null;
            }
            default:
                throw new RuntimeException( "Unknown DocumentProcessingFieldType: " + fieldType );
        }
    }

    public Collection<?> addFieldValue(DocumentProcessingFieldType fieldType,
                                  String fieldName,
                                  Object newValue ) {

        // Decide if the Object type happens to be a collection, or a plural object type
        // like iterable, or arraylist, in which case turn it into a Collection and continue.
        // with addFieldValues instead.
        if ( CollectionConversion.isObjectPlural( newValue ) ) {
            Collection<?> collectionRepresentation = CollectionConversion.createCollectionFromPluralObject( newValue );
            return addFieldValues( fieldType, fieldName, collectionRepresentation );
        }

        switch ( fieldType ) {
            case REFERENCE: {
                // ignore any add on reference, its always just a set.
                String decodedValue = CollectionConversion.createAsString(newValue);
                String originalRef = internalDocument.getReference();
                
                if ( originalRef.equals ( decodedValue ))
                {
                    // actually they are the same value - exit with no change.
                    return null;
                }
                     
                internalDocument.setReference( decodedValue );
                
                // replaced value is originalRef.
                return CollectionConversion.createAsStringCollection(originalRef);
            }
            case METADATA: {
                // this is addFieldValues, just add the field with the new ones.
                String decodedValue = CollectionConversion.createAsString(newValue);
                
                if ( internalDocument.getMetadata().put( fieldName, decodedValue ) )
                {
                    return CollectionConversion.createAsStringCollection( decodedValue );
                }
                 
                return null;                
            }
            case METADATA_REFERENCE: {
                // this is addFieldValues, just add the field with the new ones.
                ReferencedData decodedValue = CollectionConversion.createAsReferencedDataObject(newValue);
                if ( internalDocument.getMetadataReferences().put( fieldName, decodedValue ) )
                {
                     return CollectionConversion.createAsStringCollection( decodedValue );
                }
                 
                return null;    
            }
            default:
                throw new RuntimeException( "Unknown DocumentProcessingFieldType: " + fieldType );
        }
    }

    /**
     *
     * @param fieldType
     * @param fieldName
     * @return
     */
    public Collection<?> deleteFieldValues( DocumentProcessingFieldType fieldType,
                                            String fieldName ) {

        switch ( fieldType ) {
            case REFERENCE: {
                // we have a match, set reference to null and exit.
                Collection<String> originalRef
                        = CollectionConversion.createAsStringCollection(internalDocument.getReference());
                internalDocument.setReference( null );
                return originalRef;
            }

            case METADATA: {
                // this is addFieldValues, so just add new values onto existing data
                Collection<?> values = internalDocument.getMetadata().removeAll(fieldName);
                return values;
            }

            case METADATA_REFERENCE: {
                // this is addFieldValues, so just add new values onto existing data
                Collection<?> values = internalDocument.getMetadataReferences().removeAll(fieldName);
                return values;
            }
            default:
                throw new RuntimeException( "Unknown DocumentProcessingFieldType: " + fieldType );
        }
    }

    /**
     *
     * @param fieldType
     * @param fieldName
     * @param newValues
     * @return
     */
    public Collection<?> deleteFieldValues( DocumentProcessingFieldType fieldType,
                                            String fieldName,
                                            Collection<?> newValues ) {

        Collection<Object> deletedValues = new ArrayList();

        switch ( fieldType ) {
            case REFERENCE: {
                // if any of the values supplied match, remove the reference, and set to null                
                String reference = internalDocument.getReference();

                // its already empty, nothing else we can do for reference field.
                if ( Strings.isNullOrEmpty(reference) ) {
                    return null;
                }

                for ( Object value : newValues ) {
                    String comparisonValue = String.valueOf( value );

                    if ( reference.equals( comparisonValue ) ) {
                        // we have a match, set reference to null and exit.
                        internalDocument.setReference( null );
                        // add this value on to our deleted list, and exit#
                        deletedValues.add( comparisonValue );
                        return deletedValues;
                    }
                }

                // otherwise we found no values, matching, so return no changes.
                return null;
            }

            case METADATA: {
                // this is addFieldValues, so just add new values onto existing data
                if ( !internalDocument.getMetadata().containsKey( fieldName ) ) {
                    return null;
                }

                for ( Object value : newValues ) {
                    // otherwise, remove values that match.
                    if ( internalDocument.getMetadata().remove( fieldName, value ) ) {
                        deletedValues.add( value );
                    }
                }

                // return null if no changes were made.
                if ( deletedValues.isEmpty() ) {
                    return null;
                }

                return deletedValues;
            }

            case METADATA_REFERENCE: {
                // this is addFieldValues, so just add new values onto existing data
                if ( !internalDocument.getMetadataReferences().containsKey( fieldName ) ) {
                    return null;
                }

                for ( Object value : newValues ) {
                    // otherwise, remove values that match.
                    if ( internalDocument.getMetadataReferences().remove( fieldName, value ) ) {
                        deletedValues.add( value );
                    }
                }
                // return null if no changes were made.
                if ( deletedValues.isEmpty() ) {
                    return null;
                }

                return deletedValues;
            }
            default:
                throw new RuntimeException( "Unknown DocumentProcessingFieldType: " + fieldType );
        }
    }

    /**
     *
     * @param fieldType
     * @param fieldName
     * @param existingValue
     * @return
     */
    public Collection<?> deleteFieldValue( DocumentProcessingFieldType fieldType,
                                           String fieldName,
                                           Object existingValue ) {

        // Decide if the Object type happens to be a collection, or a plural object type
        // like iterable, or arraylist, in which case turn it into a Collection and continue.
        // with deleteFieldValues instead.
        if ( CollectionConversion.isObjectPlural( existingValue ) ) {
            Collection<?> collectionRepresentation = CollectionConversion.createCollectionFromPluralObject( 
                    existingValue );
            return deleteFieldValues( fieldType, fieldName, collectionRepresentation );
        }

        // there can only be one deleted value, but it keeps code consistent back in the calling code.
        Collection<Object> deletedValues = new ArrayList();

        switch ( fieldType ) {
            case REFERENCE: {
                // if any of the values supplied match, remove the reference, and set to null                
                String reference = internalDocument.getReference();

                // its already empty, nothing else we can do for reference field.
                if ( Strings.isNullOrEmpty( reference ) ) {
                    return null;
                }

                String comparisonValue = String.valueOf( existingValue );
                if ( reference.equals( comparisonValue ) ) {
                    // we have a match, set reference to null and exit.
                    internalDocument.setReference( null );
                    deletedValues.add( comparisonValue );
                    return deletedValues;
                }

                // otherwise we found no values, matching, so return no changes.
                return null;
            }

            case METADATA: {
                // this is addFieldValues, so just add new values onto existing data
                if ( !internalDocument.getMetadata().containsKey( fieldName ) ) {
                    return null;
                }

                if ( internalDocument.getMetadata().remove( fieldName, existingValue ) ) {
                    deletedValues.add( existingValue );
                }
                
                // return null if no changes were made.
                if ( deletedValues.isEmpty() ) {
                    return null;
                }

                return deletedValues;
            }

            case METADATA_REFERENCE: {
                // this is addFieldValues, so just add new values onto existing data
                if ( !internalDocument.getMetadataReferences().containsKey( fieldName ) ) {
                    return null;
                }

                if ( internalDocument.getMetadataReferences().remove( fieldName, existingValue ) ) {
                    deletedValues.add( existingValue );
                }
                
                // return null if no changes were made.
                if ( deletedValues.isEmpty() ) {
                    return null;
                }

                return deletedValues;
            }
            default:
                throw new RuntimeException( "Unknown DocumentProcessingFieldType: " + fieldType );
        }
    }

    /**
     * @return the internalDocument
     */
    public DocumentInterface getInternalDocument() {
        return internalDocument;
    }

}
