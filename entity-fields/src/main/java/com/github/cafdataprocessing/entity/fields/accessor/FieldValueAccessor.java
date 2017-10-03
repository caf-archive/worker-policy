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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.cafdataprocessing.entity.fields.accessor;

import com.github.cafdataprocessing.entity.fields.exceptions.FieldValueAccessorException;
import com.github.cafdataprocessing.entity.fields.FieldEncoding;
import com.github.cafdataprocessing.entity.fields.FieldValue;
import com.hpe.caf.util.ref.ReferencedData;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base64;

/**
 * utility class, to enable user friendly access to the entity-field called FieldValue.
 * As it encodes the value in several ways, this is a handy class which allows access to the decoded values, 
 * and keeps the implementation away from the actual FieldValue contract / serialization.
 * @author trevor
 */
public class FieldValueAccessor {
    
    /**
     * create a string representation from the FieldValue supplied ( decoded value is returned. ) 
     * @param value
     * @return
     * @throws FieldValueAccessorException
     */
    public static String createStringFromFieldValue( FieldValue value )
    {
        if ( value == null )
        {
            throw new NullPointerException("Invalid parameter FieldValue is null" );
        }
        
        // if encoding is null, default to utf8.
        FieldEncoding encoding = ( value.valueEncoding != null ) ? value.valueEncoding : FieldEncoding.utf8;
        
        switch( encoding )
        {
            case utf8:
                return value.value;
            case base64:
                // create a string from the decode bytes, and return this to the caller.
                return new String( Base64.decodeBase64( value.value ), StandardCharsets.UTF_8 );
                
            case caf_storage_reference:
                // We are going to return the String value of the storage-reference, this method DOES NOT
                // retrieve the file content here and pass it back as a string, as the file could be huge!!
                return value.value;
                
            default:
                throw new FieldValueAccessorException("Unknown encoding type." + encoding);
        }
    }
    
    /**
     * create a ReferencedData representation from the FieldValue supplied ( decoded value is returned. )
     * 
     * @param value
     * @return
     * @throws FieldValueAccessorException 
     */
    public static ReferencedData createReferencedDataFromFieldValue( FieldValue value )
    {
        if ( value == null )
        {
            throw new NullPointerException("Invalid parameter FieldValue is null" );
        }
        
        // if encoding is null, default to utf8.
        FieldEncoding encoding = ( value.valueEncoding != null ) ? value.valueEncoding : FieldEncoding.utf8;
        
        switch( encoding )
        {
            case utf8:
                // this is already a string, get ReferencedData from it
                return ReferencedData.getWrappedData( value.value.getBytes(StandardCharsets.UTF_8) ) ;
            case base64:
                // Decode our base64 back to bytes, to create a ReferencedData from it.
                byte[] rawbytes = Base64.decodeBase64( value.value );
                // on 2 lines for easier debugging at this stage!
                return ReferencedData.getWrappedData( rawbytes );
                
            case caf_storage_reference:
                return ReferencedData.getReferencedData( value.value );
                
            default:
                throw new FieldValueAccessorException("Unknown encoding type." + encoding);
        }
    }
    
}
