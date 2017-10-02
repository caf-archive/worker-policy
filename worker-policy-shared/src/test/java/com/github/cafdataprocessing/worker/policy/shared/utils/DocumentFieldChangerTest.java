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
package com.github.cafdataprocessing.worker.policy.shared.utils;

import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.worker.policy.shared.DocumentProcessingFieldCategory;
import com.github.cafdataprocessing.entity.fields.FieldEncoding;
import com.github.cafdataprocessing.entity.fields.FieldValue;
import com.hpe.caf.util.ref.ReferencedData;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Testing of DocumentFieldChanger class
 */
public class DocumentFieldChangerTest {

    /**
     * Verify that calling the addFieldValues method using Category parameter adds values to appropriate part
     * of document.
     */
    @Test
    public void addFieldValuesUsingCategoryTest(){
        Document document = new Document();
        Multimap<String, String> metadata = document.getMetadata();
        Multimap<String, ReferencedData> metadataReferences = document.getMetadataReferences();
        List<String> expectedMetadataValues = new ArrayList<>();
        List<ReferencedData> expectedReferencedDataValues = new ArrayList<>();
        DocumentFieldChanger fieldChanger = new DocumentFieldChanger(document);

        //test adding Reference

        String referenceValue = "myReference";
        fieldChanger.addFieldValues(DocumentProcessingFieldCategory.REFERENCE, null, Arrays.asList(referenceValue));
        checkReference(referenceValue, document);

        //test adding Metadata

        String field_1_Name = "test field 1";
        String field_1_Value_1 = "test value 1";
        String field_1_Value_2 = "test value 2";
        expectedMetadataValues.add(field_1_Value_1);
        expectedMetadataValues.add(field_1_Value_2);
        Collection<String> field_1_Values = Arrays.asList(field_1_Value_1, field_1_Value_2);

        fieldChanger.addFieldValues(DocumentProcessingFieldCategory.ALLMETADATA, field_1_Name, field_1_Values);

        //check that reference hasn't been altered
        checkReference(referenceValue, document);

        Assert.assertEquals("Verifying we have expected number of metadata values on document.",
                expectedMetadataValues.size(), metadata.size());
        checkMetadataField(field_1_Name, field_1_Values, metadata);

        //now add some metadata references
        //Referenced Data
        String reference_field_1_Name = "test referenced data 1";
        ReferencedData reference_field_1_Value_1 = ReferencedData.getReferencedData("caf/storage");
        ReferencedData reference_field_1_Value_2 = ReferencedData.getWrappedData("test byte data".getBytes(StandardCharsets.UTF_8));
        expectedReferencedDataValues.add(reference_field_1_Value_1);
        expectedReferencedDataValues.add(reference_field_1_Value_2);
        Collection<ReferencedData> reference_field_1_Values = Arrays.asList(reference_field_1_Value_1,
                reference_field_1_Value_2);
        fieldChanger.addFieldValues(DocumentProcessingFieldCategory.ALLMETADATA, reference_field_1_Name,
                reference_field_1_Values);

        //check that reference hasn't been altered
        checkReference(referenceValue, document);

        //check that metadata hasn't been altered
        Assert.assertEquals("Verifying we have expected number of metadata values on document.",
                expectedMetadataValues.size(), metadata.size());
        checkMetadataField(field_1_Name, field_1_Values, metadata);

        //check that metadata references were added
        checkMetadataReferenceField(reference_field_1_Name, expectedReferencedDataValues, metadataReferences);
    }

    /**
     * Checking that passing in collections containing FieldValues that should be added as Metadata References
     * causes all the values passed in to be added as Metadata References.
     */
    @Test
    public void checkFieldValuesAddedCorrectly(){
        Document document = new Document();
        Multimap<String, String> metadata = document.getMetadata();
        Multimap<String, ReferencedData> metadataReferences = document.getMetadataReferences();
        DocumentFieldChanger fieldChanger = new DocumentFieldChanger(document);

        String field_1_Name = "My field";
        Collection<FieldValue> fieldValuesToAdd = new ArrayList<>();
        //creating field values of varying types
        String value_1 = "My string value";
        fieldValuesToAdd.add(createFieldValue(value_1, null));

        String reference_value = "caf/referencevalue";
        fieldValuesToAdd.add(createFieldValue(reference_value, FieldEncoding.caf_storage_reference));

        byte[] data_value = "byte array data".getBytes(StandardCharsets.UTF_8);
        fieldValuesToAdd.add(createFieldValue(getBase64EncodedByteArray(data_value), FieldEncoding.base64));

        fieldChanger.addFieldValues(DocumentProcessingFieldCategory.ALLMETADATA, field_1_Name, fieldValuesToAdd);

        //check that metadata was not updated
        Assert.assertEquals("Should be no metadata on Document", 0, metadata.size());

        //verify that values were added correctly as Metadata References

        Collection<ReferencedData> expectedReferences = new ArrayList<>();
        expectedReferences.add(ReferencedData.getWrappedData(value_1.getBytes(StandardCharsets.UTF_8)));
        expectedReferences.add(ReferencedData.getReferencedData(reference_value));
        expectedReferences.add(ReferencedData.getWrappedData(data_value));

        checkMetadataReferenceField(field_1_Name, expectedReferences, metadataReferences);

        //and verify fields added to metadata if all FieldValues with utf8 or null encoding
        String field_2_Name = "My second field";
        Collection<FieldValue> secondValuesToAdd = new ArrayList<>();
        String value_2 = "A different string value";
        secondValuesToAdd.add(createFieldValue(value_2, null));
        String value_3 = "Another string value";
        secondValuesToAdd.add(createFieldValue(value_3, FieldEncoding.utf8));

        fieldChanger.addFieldValues(DocumentProcessingFieldCategory.ALLMETADATA, field_2_Name, secondValuesToAdd);

        //check that metadata references are unchanged
        Assert.assertEquals("Expecting only one metadata reference field to exist, from previous additions.",
                1, metadataReferences.keySet().size());
        checkMetadataReferenceField(field_1_Name, expectedReferences, metadataReferences);

        //check the values were added as metadata
        Assert.assertEquals("Should be expected number of metadata entries.", secondValuesToAdd.size(),
                metadata.size());
        checkMetadataField(field_2_Name, Arrays.asList(value_2, value_3), metadata);
    }

    public static String getBase64EncodedByteArray(byte[] value){
        return new String(org.apache.commons.codec.binary.Base64.encodeBase64(value), StandardCharsets.UTF_8);
    }

    private static FieldValue createFieldValue(String value, FieldEncoding encoding){
        FieldValue fieldValue = new FieldValue();
        fieldValue.value = value;
        fieldValue.valueEncoding = encoding;
        return fieldValue;
    }

    private static void checkMetadataReferenceField(String fieldName, Collection<ReferencedData> expectedFieldValues,
                                                    Multimap<String, ReferencedData> metadata){
        Assert.assertTrue("Expecting metadata reference field to have been added to Document: " + fieldName,
                metadata.containsKey(fieldName));
        Collection<ReferencedData> returnedValues = metadata.get(fieldName);
        Assert.assertEquals("The number of values on the field should be the amount we expect.",
                expectedFieldValues.size(), returnedValues.size());
        List<ReferencedData> expectedFieldValuesAsList = new ArrayList<>(expectedFieldValues);
        for(ReferencedData returnedValue : returnedValues){
            //check if this is one of the expected values
            ReferencedData foundMatch = null;
            for (ReferencedData expectedValue : expectedFieldValuesAsList){
                if(Arrays.equals(expectedValue.getData(), returnedValue.getData())){
                    foundMatch = expectedValue;
                }
                if(expectedValue.getReference() == returnedValue.getReference()){
                    foundMatch = expectedValue;
                }
            }
            Assert.assertNotNull("Expected to find a match for returned reference data for field "+ fieldName,
                    foundMatch);
            expectedFieldValuesAsList.remove(foundMatch);
        }
    }

    private static void checkMetadataField(String fieldName, Collection<String> expectedFieldValues,
                                           Multimap<String, String> metadata){
        Assert.assertTrue("Expecting metadata field to have been added to Document: "+fieldName,
                metadata.containsKey(fieldName));
        Collection<String> returnedValues = metadata.get(fieldName);
        Assert.assertEquals("The number of values on the field should be the amount we expect.",
                expectedFieldValues.size(), returnedValues.size());
        for(String fieldValue : expectedFieldValues) {
            Assert.assertTrue("Expecting value on metadata", returnedValues.contains(fieldValue));
        }
    }

    private static void checkReference(String expectedValue, DocumentInterface document){
        Assert.assertEquals("Document reference should have been set to expected value.", expectedValue,
                document.getReference());
    }
}
