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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.cafdataprocessing.entity.fields.accessor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.cafdataprocessing.entity.fields.exceptions.FieldValueAccessorException;
import com.github.cafdataprocessing.entity.fields.factory.FieldValueFactory;
import com.github.cafdataprocessing.entity.fields.FieldEncoding;
import com.github.cafdataprocessing.entity.fields.FieldValue;
import com.hpe.caf.util.ref.ReferencedData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author trevor
 */
public class FieldValueAccessorTest {
    
    @Test
    public void testValueEncodingDecodingString() throws FieldValueAccessorException {
        String originalValue = "Is this my value";
        FieldValue value = FieldValueFactory.create(originalValue);
        
        Assert.assertNotNull( "Value can't be null", value );
        Assert.assertTrue( "Value must be utf8 encoded or null", value.valueEncoding == null || ( value.valueEncoding == FieldEncoding.utf8 ));
        
        // decode the FieldValue
        String decodedValue = FieldValueAccessor.createStringFromFieldValue( value );
        
        Assert.assertEquals(  "OriginalValue must match encoded value", originalValue, decodedValue);
        // check decoding to same value.
        
    }
    
    @Test
    public void testvalueEncodingDecodingReferenceData_Binary() throws FieldValueAccessorException{
        String originalValue = "Is this my value";
        
        ReferencedData data = ReferencedData.getWrappedData(originalValue.getBytes(StandardCharsets.UTF_8));
        
        FieldValue value = FieldValueFactory.create(data);
        
        Assert.assertNotNull( "Value can't be null", value );
        Assert.assertTrue( "Value must be base64 encoded", ( value.valueEncoding == FieldEncoding.base64 ));
        
        // decode the FieldValue
        String decodedValue = FieldValueAccessor.createStringFromFieldValue( value );
        
        Assert.assertEquals(  "OriginalValue must match encoded value", originalValue, decodedValue);
        // check decoding to same value.
    }
    
    
    @Test
    public void testvalueEncodingDecodingReferenceData_StorageReference() throws FieldValueAccessorException{
        String originalValue = "SomeGuid/SomeGuid";
        ReferencedData data = ReferencedData.getReferencedData( originalValue );
        
        FieldValue value = FieldValueFactory.create(data);
        
        Assert.assertNotNull( "Value can't be null", value );
        Assert.assertTrue( "Value must be base64 encoded", ( value.valueEncoding == FieldEncoding.caf_storage_reference ));
        
        // cant decode this as its not a real reference, but of course, we can compare the refs.
        Assert.assertEquals(  "OriginalValue must match encoded value", originalValue, value.value);
    }
    
    @Test
    public void testValueEncodingDecodingAsReferenceData_FromString_ForcedEncoding() throws FieldValueAccessorException{
        String originalValue = "Is this my value";
        
        // perform via standard root, this is us encoding.
        ReferencedData data = ReferencedData.getWrappedData(TestUtilities.getByteArrayFromString( originalValue ));
        FieldValue valueNormal = FieldValueFactory.create(data);
        
        FieldValue value = FieldValueFactory.create(originalValue, FieldEncoding.base64);
        
        Assert.assertNotNull( "Value can't be null", value );
        Assert.assertTrue( "Value must be base64 encoded", ( value.valueEncoding == FieldEncoding.base64 ));
        
        // decode the FieldValue
        String decodedValue = FieldValueAccessor.createStringFromFieldValue( value );
        
        Assert.assertEquals(  "OriginalValue must match encoded value", originalValue, decodedValue);
        // check decoding to same value.
    }
    
    @Test
    public void testSerializationOfFieldValue() throws IOException{
         // when we do not transmit the property FieldValue->valueEncoding, or we do transmit it but with a value of null,
        // they should behave the same as if passed with utf8.  
        // ** Our default behaviour of the value field, if valueEncoding is not specified is utf8 encoded.**
        ObjectMapper objectMapper = new ObjectMapper();
        final String initialValue =  "test this string";
        
        ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
        node.put("value", initialValue);
        node.put("valueEncoding", FieldEncoding.utf8.toString());
        
        // now create a field value from this representation
        FieldValue decodedValue = (FieldValue)objectMapper.readValue( node.toString(), FieldValue.class );
        
        Assert.assertNotNull( "FieldValue must be decoded correctly", decodedValue );
        
        // check newValue has utf8 encoding if specified!.
        Assert.assertEquals( "Value Encoding must match", FieldEncoding.utf8, decodedValue.valueEncoding);
        
        // check new value equals our initial value.
        String decodeString = FieldValueAccessor.createStringFromFieldValue( decodedValue );
        
        Assert.assertEquals( "Value must match original value.", initialValue, decodeString);
    }
    
    @Test
    public void testSerializationOfFieldValueWithNullEncoding() throws JsonProcessingException, IOException{
        // when we do not transmit the property FieldValue->valueEncoding, or we do transmit it but with a value of null,
        // they should behave the same as if passed with utf8.  
        // ** Our default behaviour of the value field, if valueEncoding is not specified is utf8 encoded.**
        ObjectMapper objectMapper = new ObjectMapper();
        final String initialValue =  "test this string";
        
        ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
        node.put("value", initialValue);
        
        // now create a field value from this representation
        FieldValue decodedValue = (FieldValue)objectMapper.readValue( node.toString(), FieldValue.class );
        
        Assert.assertNotNull( "FieldValue must be decoded correctly", decodedValue );
        // check newValue has no encoding.
        Assert.assertEquals( "Value Encoding must be null", null, decodedValue.valueEncoding);
        
        // check new value equals our initial value.
        String decodeString = FieldValueAccessor.createStringFromFieldValue( decodedValue );
        
        Assert.assertEquals( "Value must match original value.", initialValue, decodeString);
    }
}
