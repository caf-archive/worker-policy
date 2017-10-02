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

import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.entity.fields.*;
import com.hpe.caf.util.ref.ReferencedData;
import org.junit.Assert;

import java.nio.charset.StandardCharsets;
import java.rmi.UnexpectedException;
import java.util.Collection;
import java.util.HashMap;

/**
 * Utilty methods used in testing the project.
 */
public class TestUtilities {
    public static Document intialiseDocumentWithEmptyProcessingRecord(){
        Document testDocument = new Document();
        DocumentProcessingRecord processingRecord = testDocument.createPolicyDataProcessingRecord();
        processingRecord.metadataChanges = new HashMap<>();
        return testDocument;
    }

    public static String getBase64EncodedByteArrayFromString(String value){
        if(value==null){
            return "";
        }
        return getBase64EncodedByteArray(org.apache.commons.codec.binary.Base64.encodeBase64(getByteArrayFromString(value)));
    }

    public static String getBase64EncodedByteArray(byte[] value){
        return new String(org.apache.commons.codec.binary.Base64.encodeBase64(value), StandardCharsets.UTF_8);
    }

    public static byte[] getByteArrayFromString(String value){
        return value.getBytes(StandardCharsets.UTF_8);
    }

    public static FieldValue createFieldValue(String value, FieldEncoding encoding){
        FieldValue fieldValue = new FieldValue();
        fieldValue.value = value;
        fieldValue.valueEncoding = encoding;
        return fieldValue;
    }

    public static void addMetaDataAndRecord(Multimap<String, String> metadata, DocumentProcessingRecord processingRecord,
                                      String fieldName, String fieldValue){
        metadata.put(fieldName, fieldValue);
        FieldChangeRecord addedField_1_Record = createAddedFieldChangeRecord();
        processingRecord.metadataChanges.put(fieldName, addedField_1_Record);
    }

    public static void addMetaDataReferenceAndRecord(Multimap<String, ReferencedData> metadataReferences, DocumentProcessingRecord processingRecord,
                                               String fieldName, ReferencedData fieldValue){
        if ( !metadataReferences.put(fieldName, fieldValue) )
        {
            throw new RuntimeException( "Code hasn't added the filed - so dont continue and create a test record. Investigate what is there already in the map!");
        }
        
        FieldChangeRecord addedField_1_Record = createAddedFieldChangeRecord();
        processingRecord.metadataChanges.put(fieldName, addedField_1_Record);
    }

    public static FieldChangeRecord createAddedFieldChangeRecord(){
        FieldChangeRecord fieldChangeRecord = new FieldChangeRecord();
        fieldChangeRecord.changeType = FieldChangeType.added;
        return fieldChangeRecord;
    }

    public static void verifySingleFieldMetadataReferences(Multimap<String, ReferencedData> updatedMetadataReferences,
                                                           String fieldName, ReferencedData expectedFieldValue){
        Assert.assertTrue("Expecting metadata to contain field with matching field name: " + fieldName,
                updatedMetadataReferences.containsKey(fieldName));

        Collection<ReferencedData> returnedValues = updatedMetadataReferences.get(fieldName);
        Assert.assertEquals("Expecting only a single value on field ", 1,
                returnedValues.size());
        ReferencedData returnedValue = returnedValues.iterator().next();
        //compare referenced data against expected
        Assert.assertEquals("Expecting reference on returned ReferencedData to match expected.", expectedFieldValue.getReference(),
                returnedValue.getReference());
        Assert.assertArrayEquals("Expecting data on returned ReferencedData to match expected.", expectedFieldValue.getData(),
                returnedValue.getData());
    }

    public static void verifySingleFieldMetadata(Multimap<String, String> updatedMetadata, String fieldName, String fieldValue){
        Assert.assertTrue("Expecting metadata to contain field with matching value.",
                updatedMetadata.containsEntry(fieldName, fieldValue));
        Assert.assertEquals("Expecting only one value to be on field.", 1,
                updatedMetadata.get(fieldName).size());
    }
}
