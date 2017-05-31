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
package com.github.cafdataprocessing.entity.fields.factory;

import com.google.common.base.Strings;
import com.github.cafdataprocessing.entity.fields.FieldEncoding;
import com.github.cafdataprocessing.entity.fields.FieldValue;
import com.hpe.caf.util.ref.ReferencedData;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.codec.binary.Base64;

/**
 * Factory methods, to create the FieldValue object using different encodings.
 *
 * @author trevor
 */
public final class FieldValueFactory {

    private FieldValueFactory(){
        
    }
    
    /**
     * Factory method to create a fieldValue from a string.
     * @param value
     * @return 
     */
    public static final FieldValue create( String value ) {
                 
        FieldValue fieldValue = new FieldValue();
        fieldValue.value = value;
        // Not specifying the valueEncoding as its an optional field which defaults to 
        // utf8 encoding when not specified, but Im forcing it here, to make it clearer to consumer.
        fieldValue.valueEncoding = FieldEncoding.utf8;
        
        return fieldValue;
    }

    /**
     * Factory method to create a fieldValue from a ReferencedData object.
     * @param value
     * @return 
     */
    public static final FieldValue create( ReferencedData value ) {
        
        checkValueNotNull( value );
         
        FieldValue fieldValue = new FieldValue();
        
        if ( !Strings.isNullOrEmpty( value.getReference() ) ) {
            // we have a storage reference set value to the string
            // value, and set encoding to caf-storage.
            fieldValue.value = value.getReference();
            fieldValue.valueEncoding = FieldEncoding.caf_storage_reference;
            return fieldValue;
        }
        
        // otherwise, use the base64 encoding on the data array.value.getData() 
        fieldValue.value = Base64.encodeBase64String( value.getData() );
        fieldValue.valueEncoding = FieldEncoding.base64;
                
        return fieldValue;
    }

    /**
     * Factory method to create a fieldValue from a string, defaults to utf8 encoding.
     * You can override the encoding, to have a string, placed into a referencedData object for you.
     * @param value
     * @param requestedEncoding
     * @return 
     */
    public static final FieldValue create( String value, FieldEncoding requestedEncoding ) {
        
        // we have a special case, where the reference field can be null, so we allow a FieldValue to be
        // created with a null value element.  This does not apply to other types, to enforce null checks.
        checkValueNotNull( value, requestedEncoding );
                
        FieldValue fieldValue = new FieldValue();
        
        FieldEncoding encodingToUse = requestedEncoding == null ? FieldEncoding.utf8 : requestedEncoding;
        
        switch( encodingToUse )
        {
            case utf8:
                fieldValue.value = value;    
                return fieldValue;        
            case base64:            
                
                fieldValue.valueEncoding = encodingToUse;
                // turn value to raw byte array.
                byte[] rawbytes = getByteArrayFromString( value );
                fieldValue.value = Base64.encodeBase64String( rawbytes );
                return fieldValue;       
                
            case caf_storage_reference:
                // we are creating a referenced data object from the value assuming its a reference,
                // this method is not here to store into CAF-Storage for you..
                return create( ReferencedData.getReferencedData( value ) );        
                
            default:
                throw new InvalidParameterException("Invalid encoding specified for FieldValue as: " + encodingToUse );
        }
    }
    
    /**
     * Factory method to create a fieldValue from an object.
     * @param value
     * @return 
     */
    public static FieldValue create( Object value ) {
        
        // depending upon the type of object, we should now encode it differently onto the actual 
        // FieldValue.
        if ( value instanceof ReferencedData ) {
            checkValueNotNull( value );
            return create( (ReferencedData) value );
        }
        
        if ( value instanceof String ) {
            return create( ( String ) value );
        }

        // otherwise attempt to encode the item as a string, we could fail here, but I want to support
        // passing of long etc, as strings, also.
        return create( String.valueOf(value) );
    }

    public static Collection<FieldValue> create( Collection<?> originalValues ) {

        if ( originalValues == null )
        {
            // unable to create a field value from null.
            throw new NullPointerException("Unable to create a fieldvalue from null collection of values, we do not hold null values in our maps.");
        }
        
        Collection<FieldValue> fieldValues = new ArrayList<>();

        for ( Object value : originalValues ) {
            
            // depending upon the object, we not need to encode each fieldValue differently.
            fieldValues.add( create( value ) );
        }

        return fieldValues;
    }
    
    /** 
     * Get byte array from a string value - this is the encoding used for raw bytes within our referenced data blob.
     * 
     * @param value
     * @return 
     */
     public static byte[] getByteArrayFromString( String value ) {

        if ( value == null ) {
            return null;
        }

        return value.getBytes( StandardCharsets.UTF_8 );
    }

     /**
      * 
      * @param value
      * @param requestedEncoding
      * @throws NullPointerException 
      */
     private static void checkValueNotNull( Object value, FieldEncoding requestedEncoding ) throws NullPointerException {
         
        // we have a special case, where the reference field can be null, so we allow a FieldValue to be
        // created with a null value element.  This does not apply to other types, to enforce null checks.
        if ( requestedEncoding == FieldEncoding.utf8 ) {
            return;
        }
        
        // other types aren't allows to be null.
        checkValueNotNull( value );
    }
     
    private static void checkValueNotNull( Object value ) throws NullPointerException {
        if ( value == null ) {
            // unable to create a field value from null.
            throw new NullPointerException( 
                    "Unable to create a fieldvalue from null, we do not hold null values in our maps." );
        }
    }

}
