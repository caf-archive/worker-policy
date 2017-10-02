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

import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.exceptions.TrackedChangeException;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.exceptions.TrackedChangeTypeException;
import com.hpe.caf.util.ref.ReferencedData;
import java.util.Collection;
import java.util.Collections;

/**
 * Internal implementation class, which can only be used in the same package, usually by the TrackedChange class.
 *
 * @author trevor.getty@hpe.com
 */
class DocumentValueUtils {

    static Object getFieldValue( DocumentProcessingFieldType fieldType,
                                 DocumentInterface internalDocument,
                                 String fieldName ) throws TrackedChangeTypeException, TrackedChangeException {

        Collection<?> fieldValues = getFieldValues( fieldType, internalDocument, fieldName );

        // check how many items we have.
        if ( fieldValues.isEmpty() ) {
            return null;
        }

        // if we have one value, return it, otherwise, we can't return a single representation.
        if ( fieldValues.size() > 1 ) {
            throw new TrackedChangeException(
                    "Invalid request for a single fieldValue on a field which has many values." );
        }

        return fieldValues.stream().findFirst().get();
    }

    static Collection<?> getFieldValues( DocumentProcessingFieldType fieldType, DocumentInterface internalDocument,
                                         String fieldName ) throws TrackedChangeTypeException {
        switch ( fieldType ) {
            case REFERENCE:
                return Collections.singletonList( internalDocument.getReference() );
            case METADATA:
                return internalDocument.getMetadata().get( fieldName );
            case METADATA_REFERENCE:
                return internalDocument.getMetadataReferences().get( fieldName );
            default:
                throw new TrackedChangeTypeException( "Unknown TrackedDocumentFieldType" );
        }
    }

    static String getFieldValueAsString( DocumentProcessingFieldType fieldType,
                                         DocumentInterface internalDocument,
                                         String fieldName
    ) throws TrackedChangeTypeException, TrackedChangeException {

        Object value = getFieldValue( fieldType, internalDocument, fieldName );

        // user has requested this as a string, for now only return string types.
        if ( value == null ) {
            return null;
        }

        if ( value instanceof String ) {
            // just return value as is   
            return ( String ) value;
        }

        // throw as not correct type for now, maybe could convert to that type later on?
        throw new TrackedChangeTypeException( "Invalid request for String response on object type: " + value );
    }

    static boolean setFieldValues( DocumentProcessingFieldType fieldType, DocumentInterface internalDocument,
                                   String fieldName, Collection<?> newValues ) throws TrackedChangeTypeException {
        switch ( fieldType ) {
            case REFERENCE:
                // ignore any replace on reference, its always just a set.
                String newValue = ( String ) newValues.stream().findFirst().get();
                boolean causedUpdate = StringCompare.equals( internalDocument.getReference(), ( String ) newValue );
                internalDocument.setReference( ( String ) newValue );
                return causedUpdate;

            case METADATA:
                // this is setFieldValues, so removes all existing values, for these new ones.
                internalDocument.getMetadata().removeAll( fieldName );
                return internalDocument.getMetadata().putAll( fieldName, ( Collection<String> ) newValues );

            case METADATA_REFERENCE:
                // we do not monitor the actual deletion of CAF-Storage references, this the
                // callers responsibility, we only track the actual change here.
                internalDocument.getMetadata().removeAll( fieldName );
                return internalDocument.getMetadataReferences().putAll( fieldName,
                                                                        ( Collection<ReferencedData> ) newValues );
            default:
                throw new TrackedChangeTypeException( "Unknown TrackedDocumentFieldType: " + fieldType );
        }
    }

    static boolean setFieldValue( DocumentProcessingFieldType fieldType, DocumentInterface internalDocument,
                                  String fieldName, Object newValue ) throws TrackedChangeTypeException {
        switch ( fieldType ) {
            case REFERENCE:
                // ignore any replace on reference, its always just a set.
                boolean causedUpdate = StringCompare.equals( internalDocument.getReference(), ( String ) newValue );
                internalDocument.setReference( ( String ) newValue );
                return causedUpdate;
            case METADATA:
                // this is setFieldValues, so removes all existing values, for these new ones.
                internalDocument.getMetadata().removeAll( fieldName );
                return internalDocument.getMetadata().put( fieldName, ( String ) newValue );
            case METADATA_REFERENCE:
                // we do not monitor the actual deletion of CAF-Storage references, this the
                // callers responsibility, we only track the actual change here.
                internalDocument.getMetadata().removeAll( fieldName );
                return internalDocument.getMetadataReferences().put( fieldName, ( ReferencedData ) newValue );

            default:
                throw new TrackedChangeTypeException( "Unknown TrackedDocumentFieldType: " + fieldType );
        }
    }

    static boolean addFieldValues( DocumentProcessingFieldType fieldType, DocumentInterface internalDocument,
                                   String fieldName, Collection<?> newValues ) throws TrackedChangeTypeException {
        switch ( fieldType ) {
            case REFERENCE:
                // ignore any replace on reference, its always just a set.
                String newValue = ( String ) newValues.stream().findFirst().get();
                boolean causedUpdate = StringCompare.equals( internalDocument.getReference(), ( String ) newValue );
                internalDocument.setReference( ( String ) newValue );
                return causedUpdate;

            case METADATA:
                // this is addFieldValues, so just add new values onto existing data
                return internalDocument.getMetadata().putAll( fieldName, ( Collection<String> ) newValues );

            case METADATA_REFERENCE:
                // this is addFieldValues, so just add new values onto existing data
                return internalDocument.getMetadataReferences().putAll( fieldName,
                                                                        ( Collection<ReferencedData> ) newValues );
            default:
                throw new TrackedChangeTypeException( "Unknown TrackedDocumentFieldType: " + fieldType );
        }
    }

    static boolean addFieldValue( DocumentProcessingFieldType fieldType, DocumentInterface internalDocument,
                                  String fieldName, Object newValue ) throws TrackedChangeTypeException {
        switch ( fieldType ) {
            case REFERENCE:
                // ignore any add on reference, its always just a set.
                boolean causedUpdate = StringCompare.equals( internalDocument.getReference(), ( String ) newValue );
                internalDocument.setReference( ( String ) newValue );
                return causedUpdate;
            case METADATA:
                // this is addFieldValues, just add the field with the new ones.
                return internalDocument.getMetadata().put( fieldName, ( String ) newValue );
            case METADATA_REFERENCE:
                // this is addFieldValues, just add the field with the new ones.
                return internalDocument.getMetadataReferences().put( fieldName, ( ReferencedData ) newValue );
            default:
                throw new TrackedChangeTypeException( "Unknown TrackedDocumentFieldType: " + fieldType );
        }
    }
}
