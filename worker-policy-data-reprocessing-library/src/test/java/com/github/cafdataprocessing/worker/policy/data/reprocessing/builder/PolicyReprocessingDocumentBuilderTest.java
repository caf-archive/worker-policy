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
package com.github.cafdataprocessing.worker.policy.data.reprocessing.builder;

import com.github.cafdataprocessing.worker.policy.data.reprocessing.builder.exceptions.PolicyReprocessingReconstructMessageException;
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.entity.fields.*;
import com.github.cafdataprocessing.entity.fields.accessor.FieldValueAccessor;
import com.github.cafdataprocessing.entity.fields.factory.FieldValueFactory;
import com.hpe.caf.util.ref.ReferencedData;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;


/**
 * Tests for the PolicyReprocessingDocumentBuilder class.
 */
public class PolicyReprocessingDocumentBuilderTest {

    /**
     * Isolated testing that fields recorded as metadata ADDED are removed from Document passed in.
     */
    @Test
    public void reconstituteMetadataAddedFieldsTest() throws PolicyReprocessingReconstructMessageException {
        //create Document
        Document testDocument = TestUtilities.intialiseDocumentWithEmptyProcessingRecord();
        DocumentProcessingRecord processingRecord = testDocument.getPolicyDataProcessingRecord();
        Multimap<String, String> metadata = testDocument.getMetadata();

        //setup some metadata on the document that represents it in the 'original' state
        String originalFieldName_1 = "originalField1";
        String originalFieldValue_1 = "originalValue1";
        String originalFieldName_2 = UUID.randomUUID().toString();
        String originalFieldValue_2 = UUID.randomUUID().toString();

        metadata.put(originalFieldName_1, originalFieldValue_1);
        metadata.put(originalFieldName_2, originalFieldValue_2);

        //add metadata that is considered 'additional' that should be recorded on processing record.
        String addedField_1_Name = "addedField1";
        String addedField_1_Value = "addedFieldValue1";
        TestUtilities.addMetaDataAndRecord(metadata, processingRecord, addedField_1_Name, addedField_1_Value);

        String addedField_2_Name = UUID.randomUUID().toString();
        String addedField_2_Value = UUID.randomUUID().toString();
        TestUtilities.addMetaDataAndRecord(metadata, processingRecord, addedField_2_Name, addedField_2_Value);

        PolicyReprocessingDocumentBuilder.reconstituteOriginalDocument(testDocument);

        //verify that the document has had the added fields removed
        Multimap<String, String> updatedMetadata = testDocument.getMetadata();
        Assert.assertEquals("Expecting only two fields to be on Document", 2, updatedMetadata.size());
        Assert.assertTrue("Expecting metadata to contain field with matching first original field.",
                updatedMetadata.containsEntry(originalFieldName_1, originalFieldValue_1));
        Assert.assertEquals("Expecting only one value to be on first original field.", 1,
                updatedMetadata.get(originalFieldName_1).size());

        Assert.assertTrue("Expecting metadata to contain field with matching second original field.",
                updatedMetadata.containsEntry(originalFieldName_2, originalFieldValue_2));
        Assert.assertEquals("Expecting only one value to be on second original field.", 1,
                updatedMetadata.get(originalFieldName_2).size());
    }

    /**
     * Isolated testing that metadata reference fields recorded as ADDED are removed from Document passed in.
     */
    @Test
    public void reconstituteMetadataReferenceAddedFieldsTest() throws PolicyReprocessingReconstructMessageException {
        //create Document
        Document testDocument = TestUtilities.intialiseDocumentWithEmptyProcessingRecord();
        DocumentProcessingRecord processingRecord = testDocument.getPolicyDataProcessingRecord();
        Multimap<String, ReferencedData> metadataReferences = testDocument.getMetadataReferences();

        //setup some metadata on the document that represents it in the 'original' state
        String originalFieldName_1 = "originalField1";
        ReferencedData originalFieldValue_1 = ReferencedData.getReferencedData("originalValue1");
        String originalFieldName_2 = UUID.randomUUID().toString();
        ReferencedData originalFieldValue_2 = ReferencedData.getReferencedData(UUID.randomUUID().toString());
        String originalFieldName_3 = UUID.randomUUID().toString();
        ReferencedData originalFieldValue_3 = ReferencedData.getWrappedData(TestUtilities.getByteArrayFromString("originalValue3"));

        metadataReferences.put(originalFieldName_1, originalFieldValue_1);
        metadataReferences.put(originalFieldName_2, originalFieldValue_2);
        metadataReferences.put(originalFieldName_3, originalFieldValue_3);

        //add metadata that is considered 'additional' that should be recorded on processing record.
        String addedField_1_Name = "addedField1";
        ReferencedData addedField_1_Value = ReferencedData.getReferencedData("addedFieldValue1");
        TestUtilities.addMetaDataReferenceAndRecord(metadataReferences, processingRecord, addedField_1_Name, addedField_1_Value);

        String addedField_2_Name = UUID.randomUUID().toString();
        ReferencedData addedField_2_Value = ReferencedData.getReferencedData(UUID.randomUUID().toString());
        TestUtilities.addMetaDataReferenceAndRecord(metadataReferences, processingRecord, addedField_2_Name, addedField_2_Value);

        PolicyReprocessingDocumentBuilder.reconstituteOriginalDocument(testDocument);

        //verify that the document has had the added fields removed
        Multimap<String, ReferencedData> updatedMetadataReferences = testDocument.getMetadataReferences();
        Assert.assertEquals("Expecting only two fields to be on Document", 3, updatedMetadataReferences.size());

        TestUtilities.verifySingleFieldMetadataReferences(updatedMetadataReferences, originalFieldName_1, originalFieldValue_1);
        TestUtilities.verifySingleFieldMetadataReferences(updatedMetadataReferences, originalFieldName_2,
                originalFieldValue_2);
        TestUtilities.verifySingleFieldMetadataReferences(updatedMetadataReferences, originalFieldName_3,
                originalFieldValue_3);
    }

    /**
     * Isolated testing that metadata fields recorded as DELETED are removed from Document passed in.
     */
    @Test
    public void reconstituteMetadataDeletedFieldsTest() throws PolicyReprocessingReconstructMessageException {
        //create Document
        Document testDocument = TestUtilities.intialiseDocumentWithEmptyProcessingRecord();
        DocumentProcessingRecord processingRecord = testDocument.getPolicyDataProcessingRecord();
        Multimap<String, String> metadata = testDocument.getMetadata();

        //setup some metadata on the document that represents it in the 'original' state
        //setup some metadata on the document that represents it in the 'original' state
        String originalField_1_Name = "originalField1";
        String originalField_1_Value = "originalValue1";
        String originalField_2_Name = UUID.randomUUID().toString();
        String originalField_2_Value = UUID.randomUUID().toString();

        metadata.put(originalField_1_Name, originalField_1_Value);
        metadata.put(originalField_2_Name, originalField_2_Value);

        //record on the processing record that we deleted metadata
        String deletedField_1_Name = "deletedField1";
        String deletedField_1_Value = "deletedValue1";
        FieldValue deletedField_1_FieldValue = TestUtilities.createFieldValue(deletedField_1_Value, FieldEncoding.utf8);
        recordDeletedData(processingRecord, deletedField_1_Name, Arrays.asList(deletedField_1_FieldValue));

        //TODO this is caf_storage_reference encoding, need to verify if implementation turns this into a ReferenceData
        String deletedField_2_Name = "deletedField2";
        String deletedField_2_Value = "deletedValue2";
        FieldValue deletedField_2_FieldValue = TestUtilities.createFieldValue(deletedField_2_Value, FieldEncoding.caf_storage_reference);
        recordDeletedData(processingRecord, deletedField_2_Name, Arrays.asList(deletedField_2_FieldValue));

        PolicyReprocessingDocumentBuilder.reconstituteOriginalDocument(testDocument);

        //verify that the deleted fields were added back onto the document
        Multimap<String, String> updatedMetadata = testDocument.getMetadata();
        Assert.assertNotNull("Expecting updated document metadata to not be null", updatedMetadata);
        Assert.assertEquals("Expecting x fields on document after deleted fields added back.", 3, updatedMetadata.size());

        //verify that the deleted fields of type caf_storage_reference / base64 were also back onto the document
        Multimap<String, ReferencedData> updatedMetadataRef = testDocument.getMetadataReferences();
        Assert.assertNotNull("Expecting updated document metadatarefs to not be null", updatedMetadataRef);
        Assert.assertEquals("Expecting x fields on document after deleted fields added back.", 1, updatedMetadataRef.size());
        
        //verify that the document has had the original fields
        TestUtilities.verifySingleFieldMetadata(updatedMetadata, originalField_1_Name, originalField_1_Value);
        TestUtilities.verifySingleFieldMetadata(updatedMetadata, originalField_2_Name, originalField_2_Value);

        //verify the document has the the fields that had been deleted
        TestUtilities.verifySingleFieldMetadata(updatedMetadata, deletedField_1_Name, deletedField_1_Value);
        TestUtilities.verifySingleFieldMetadataReferences(updatedMetadataRef,
                deletedField_2_Name,
                FieldValueAccessor.createReferencedDataFromFieldValue(
                        deletedField_2_FieldValue));

    }

    /**
     * Isolated testing that metadata reference fields recorded as DELETED are removed from Document passed in.
     */
    @Test
    public void reconstituteMetadataReferenceDeletedFieldsTest() throws PolicyReprocessingReconstructMessageException {
        //create Document
        Document testDocument = TestUtilities.intialiseDocumentWithEmptyProcessingRecord();
        DocumentProcessingRecord processingRecord = testDocument.getPolicyDataProcessingRecord();
        Multimap<String, ReferencedData> metadataReferences = testDocument.getMetadataReferences();

        //setup some metadata on the document that represents it in the 'original' state
        String originalFieldName_1 = "originalField1";
        ReferencedData originalFieldReference_1 = ReferencedData.getReferencedData("originalValue1");
        String originalFieldName_2 = UUID.randomUUID().toString();
        ReferencedData originalFieldReference_2 = ReferencedData.getReferencedData(UUID.randomUUID().toString());

        metadataReferences.put(originalFieldName_1, originalFieldReference_1);
        metadataReferences.put(originalFieldName_2, originalFieldReference_2);

        //record on the processing record that we deleted a metadata reference field with base64 encoding (as it was a
        //data byte[]) TODO verify this is correct, metadata reference would never use UTF8 encoding right?
        String deletedField_1_Name = "deleted_base64_1";
        String deletedField_1_Value = "deletedField_value_1";
        FieldValue deletedField_1_FieldValue = createBase64FieldValue(deletedField_1_Value);
        
        recordDeletedData(processingRecord, deletedField_1_Name, Arrays.asList(deletedField_1_FieldValue));
        
        String deletedField_2_Name = "deleted_utf8_1";
        String deletedField_2_Value = "deletedField_utf8_value_1";
        FieldValue deletedField_2_FieldValue = TestUtilities.createFieldValue(deletedField_2_Value, FieldEncoding.utf8);
        recordDeletedData(processingRecord, deletedField_2_Name, Arrays.asList(deletedField_2_FieldValue));
        
        PolicyReprocessingDocumentBuilder.reconstituteOriginalDocument(testDocument);

        //verify that uft8 field was added onto document metadata
        Multimap<String, String> updatedMetadata = testDocument.getMetadata();
        Assert.assertEquals("Expecting three fields to be on Document", 1, updatedMetadata.size());
        
        // verify that the base64 field is added onto the metadata refs along with the existing values.
        Multimap<String, ReferencedData> updatedMetadataReferences = testDocument.getMetadataReferences();
        Assert.assertEquals("Expecting three fields to be on Document", 3, updatedMetadataReferences.size());

        TestUtilities.verifySingleFieldMetadataReferences(updatedMetadataReferences, originalFieldName_1, originalFieldReference_1);
        TestUtilities.verifySingleFieldMetadataReferences(updatedMetadataReferences, originalFieldName_2, originalFieldReference_2);
        TestUtilities.verifySingleFieldMetadataReferences(updatedMetadataReferences, deletedField_1_Name,
                FieldValueAccessor.createReferencedDataFromFieldValue(deletedField_1_FieldValue));
        TestUtilities.verifySingleFieldMetadata(updatedMetadata, deletedField_2_Name, deletedField_2_Value);

    }

    /**
     * Isolated testing that metadata reference fields recorded as UPDATED are reverted on Document passed in.
     * @throws PolicyReprocessingReconstructMessageException
     */
    @Test
    public void reconstituteMetadataUpdatedMetadataTest() throws PolicyReprocessingReconstructMessageException {
        //create Document
        Document testDocument = TestUtilities.intialiseDocumentWithEmptyProcessingRecord();
        DocumentProcessingRecord processingRecord = testDocument.getPolicyDataProcessingRecord();
        Multimap<String, String> metadata = testDocument.getMetadata();

        //setup some metadata on the document that represents it in the 'original' state
        String originalFieldName_1 = "originalField1";
        String originalFieldValue_1 = "originalValue1";
        String originalFieldName_2 = UUID.randomUUID().toString();
        String originalFieldValue_2 = UUID.randomUUID().toString();

        metadata.put(originalFieldName_1, originalFieldValue_1);
        metadata.put(originalFieldName_2, originalFieldValue_2);

        //add fields to represent updated fields
        String updatedField_1_Name = "updated_utf8_1";
        String updatedField_2_Name = "updated_utf8_2";

        //add these fields with 'modified' data that reconstitute will revert
        String updatedField_1_ModifiedValue = "modified value 1";
        metadata.put(updatedField_1_Name, updatedField_1_ModifiedValue);
        String updatedField_2_ModifiedValue_1 = "modified value 1 on field 2";
        metadata.put(updatedField_2_Name, updatedField_2_ModifiedValue_1);
        String updatedField_2_ModifiedValue_2 = "modified value 2 on field 2";
        metadata.put(updatedField_2_Name, updatedField_2_ModifiedValue_2);

        //record on processing record that fields were updated
        String updatedField_1_Value = "updated_utf8_value_1";
        FieldValue updatedFieldValue_1 = TestUtilities.createFieldValue(updatedField_1_Value, FieldEncoding.utf8);
        recordUpdatedData(processingRecord, updatedField_1_Name, Arrays.asList(updatedFieldValue_1));

        String updatedField_2_Value_1 = "updated_utf8 field 2 value 1";
        String updatedField_2_Value_2 = "updated_utf8 field 2 value 2";
        String updatedField_2_Value_3 = "updated_utf8 field 2 value 3";
        // Update record is a single record, with 3 values in it, not 3 records, it would just replace the value
        // each time in the map of string - fieldvalue(s).
        recordUpdatedData(processingRecord, updatedField_2_Name, 
                                            FieldValueFactory.create( Arrays.asList( updatedField_2_Value_1, updatedField_2_Value_2, updatedField_2_Value_3)));
        

        PolicyReprocessingDocumentBuilder.reconstituteOriginalDocument(testDocument);

        //verify that document had fields reverted to expected values (2 original fields + 1 updated field + 3 for second updated field)
        // check depending upon the type, in the correct map for the items.
        Multimap<String, String> updatedMetadata = testDocument.getMetadata();
        Assert.assertEquals("Expected number of field values returned in metadata should match.",
                6, updatedMetadata.size());

        //verify that document has no metadatarefs.
        Multimap<String, ReferencedData> updatedMetadataRefs = testDocument.getMetadataReferences();
        Assert.assertEquals("Expected number of field values returned in metadata refs should match.",
                0, updatedMetadataRefs.size());
        
        //check 'original' fields are unchanged
        TestUtilities.verifySingleFieldMetadata(updatedMetadata, originalFieldName_1, originalFieldValue_1);
        TestUtilities.verifySingleFieldMetadata(updatedMetadata, originalFieldName_2, originalFieldValue_2);

        //check first updated field has had its value changed to the value recorded on processing record
        TestUtilities.verifySingleFieldMetadata(updatedMetadata, updatedField_1_Name, updatedField_1_Value);

        //check second updated field has been reverted to the recorded values
        Collection<String> returnedUpdatedField_2_Values = updatedMetadata.get(updatedField_2_Name);
        List<String> expectedUpdatedField_2_Values = Arrays.asList(updatedField_2_Value_1, updatedField_2_Value_2,
                updatedField_2_Value_3);
        Assert.assertEquals("Expecting number of values on updated field to be what we expect.", expectedUpdatedField_2_Values.size(),
                returnedUpdatedField_2_Values.size());
        for(String returnedValue : returnedUpdatedField_2_Values){
            //check if this is one of the expected values
            Assert.assertTrue("Expecting value on updated field to be one of the expected values recorded on processing record.",
                    expectedUpdatedField_2_Values.contains(returnedValue));
            
        }
    }

    /**
     * Isolated testing that metadata reference fields recorded as UPDATED are reverted on Document passed in.
     * @throws PolicyReprocessingReconstructMessageException
     */
    @Test
    public void reconstituteMetadataUpdatedMetadataReferencesTest() throws PolicyReprocessingReconstructMessageException {
        //create Document
        Document testDocument = TestUtilities.intialiseDocumentWithEmptyProcessingRecord();
        DocumentProcessingRecord processingRecord = testDocument.getPolicyDataProcessingRecord();
        Multimap<String, ReferencedData> metadataReferences = testDocument.getMetadataReferences();

        //setup some metadata on the document that represents it in the 'original' state
        String originalFieldName_1 = "originalField1";
        String originalFieldValue_1 = "originalValue1";
        ReferencedData originalReferencedDataValue_1 = ReferencedData.getReferencedData(originalFieldValue_1);
        String originalFieldName_2 = UUID.randomUUID().toString();
        String originalFieldValue_2 = UUID.randomUUID().toString();
        byte[] originalFieldValue_2_AsBytes = TestUtilities.getByteArrayFromString(originalFieldValue_2);
        ReferencedData originalReferencedDataValue_2 = ReferencedData.getWrappedData(originalFieldValue_2_AsBytes);

        metadataReferences.put(originalFieldName_1, originalReferencedDataValue_1);
        metadataReferences.put(originalFieldName_2, originalReferencedDataValue_2);

        //add fields to represent updated fields
        String updatedField_1_Name = "updated_base64_1";
        String updatedField_2_Name = "updated field 2";

        //add these fields with 'updated' data that reconstitute will revert
        byte[] revert_1_Value = TestUtilities.getByteArrayFromString("updated data that should be reverted");
        ReferencedData revert_1_ReferencedData = ReferencedData.getWrappedData(revert_1_Value);

        String revert_2_Value_1 = "updated/storagereference_1";
        ReferencedData revert_2_ReferencedData_1 = ReferencedData.getReferencedData(revert_2_Value_1);
        byte[] revert_2_Value_2 = TestUtilities.getByteArrayFromString("updated data that should also be reverted");
        ReferencedData revert_2_ReferencedData_2 = ReferencedData.getWrappedData(revert_2_Value_2);

        metadataReferences.put(updatedField_1_Name, revert_1_ReferencedData);
        metadataReferences.put(updatedField_2_Name, revert_2_ReferencedData_1);
        metadataReferences.put(updatedField_2_Name, revert_2_ReferencedData_2);

        //record on processing record that we updated fields from other values
        String updatedField_1_Value = "updated_base64_value_1";
        FieldValue updatedFieldValue_1 = createBase64FieldValue(updatedField_1_Value);
        recordUpdatedData(processingRecord, updatedField_1_Name, Arrays.asList(updatedFieldValue_1));

        String updatedField_2_Value_1 = "tests/cafstorage";
        FieldValue updatedFieldValue_2_1 = TestUtilities.createFieldValue(updatedField_2_Value_1, FieldEncoding.caf_storage_reference);
        String updatedField_2_Value_2 = "a string to become a byte array";
        FieldValue updatedFieldValue_2_2 = createBase64FieldValue(updatedField_2_Value_2);

        recordUpdatedData(processingRecord, updatedField_2_Name, Arrays.asList(updatedFieldValue_2_1, updatedFieldValue_2_2));

        PolicyReprocessingDocumentBuilder.reconstituteOriginalDocument(testDocument);

        Multimap<String, ReferencedData> updatedMetadataReferences = testDocument.getMetadataReferences();
        Assert.assertEquals("Expected number of field values returned in metadata references should match.",
                5, updatedMetadataReferences.size());

        //verify that the original fields are still on document unchanged
        TestUtilities.verifySingleFieldMetadataReferences(updatedMetadataReferences, originalFieldName_1, originalReferencedDataValue_1);
        TestUtilities.verifySingleFieldMetadataReferences(updatedMetadataReferences, originalFieldName_2, originalReferencedDataValue_2);

        //check first updated field has had its value changed to the value recorded on processing record
        TestUtilities.verifySingleFieldMetadataReferences(updatedMetadataReferences, updatedField_1_Name,
                FieldValueAccessor.createReferencedDataFromFieldValue(updatedFieldValue_1));

        //check second updated field has been reverted to the recorded values
        Collection<ReferencedData> returnedUpdatedField_2_Values = updatedMetadataReferences.get(updatedField_2_Name);
        List<ReferencedData> expectedUpdatedField_2_Values = 
                Arrays.asList(FieldValueAccessor.createReferencedDataFromFieldValue( updatedFieldValue_2_1 ),
                              FieldValueAccessor.createReferencedDataFromFieldValue( updatedFieldValue_2_2));
        
        Assert.assertEquals("Expecting number of values on updated field to be what we expect.", expectedUpdatedField_2_Values.size(),
                returnedUpdatedField_2_Values.size());
        for(ReferencedData returnedValue : returnedUpdatedField_2_Values){
            //check if this is one of the expected values
            ReferencedData foundMatch = null;
            for (ReferencedData expectedValue : expectedUpdatedField_2_Values){
                
                if(Arrays.equals(expectedValue.getData(), returnedValue.getData())){
                    foundMatch = expectedValue;
                    break;
                }
                if(expectedValue.getReference() == returnedValue.getReference()){
                    foundMatch = expectedValue;
                    break;
                }
            }
            Assert.assertNotNull("Expected to find a match for returned reference data for field "+ updatedField_2_Name,
                    foundMatch);
        }
    }

    /**
     * Testing that passing no processing record causes no change to the document.
     * @throws PolicyReprocessingReconstructMessageException
     */
    @Test
    public void noProcessingRecordNoChangeTest() throws PolicyReprocessingReconstructMessageException {
        //create Document
        Document testDocument = new Document();
        Multimap<String, String> metadata = testDocument.getMetadata();

        //setup some metadata on the document that represents it in the 'original' state
        String originalFieldName_1 = "originalField1";
        String originalFieldValue_1 = "originalValue1";
        String originalFieldName_2 = UUID.randomUUID().toString();
        String originalFieldValue_2 = UUID.randomUUID().toString();

        metadata.put(originalFieldName_1, originalFieldValue_1);
        metadata.put(originalFieldName_2, originalFieldValue_2);

        PolicyReprocessingDocumentBuilder.reconstituteOriginalDocument(testDocument);

        Multimap<String, String> updatedMetadata = testDocument.getMetadata();
        Assert.assertEquals("Expected number of field values returned in metadata references should match.",
                2, updatedMetadata.size());

        //verify that the original fields are still on document unchanged
        TestUtilities.verifySingleFieldMetadata(updatedMetadata, originalFieldName_1, originalFieldValue_1);
        TestUtilities.verifySingleFieldMetadata(updatedMetadata, originalFieldName_2, originalFieldValue_2);

        //and check again that if processing record is initialized that we don't change the fields
        testDocument.createPolicyDataProcessingRecord();
        PolicyReprocessingDocumentBuilder.reconstituteOriginalDocument(testDocument);
        updatedMetadata = testDocument.getMetadata();
        Assert.assertEquals("Expected number of field values returned in metadata references should match.",
                2, updatedMetadata.size());

        //verify that the original fields are still on document unchanged
        TestUtilities.verifySingleFieldMetadata(updatedMetadata, originalFieldName_1, originalFieldValue_1);
        TestUtilities.verifySingleFieldMetadata(updatedMetadata, originalFieldName_2, originalFieldValue_2);
    }

    private void recordDeletedData(DocumentProcessingRecord processingRecord, String fieldName,
                                               Collection<FieldValue> fieldValues){
        FieldChangeRecord deletedFieldRecord = createFieldChangeRecord(FieldChangeType.deleted, fieldValues);
        processingRecord.metadataChanges.put(fieldName, deletedFieldRecord);
    }

    private void recordUpdatedData(DocumentProcessingRecord processingRecord, String fieldName,
                                   Collection<FieldValue> fieldValues){
        FieldChangeRecord updatedFieldRecord = createFieldChangeRecord(FieldChangeType.updated, fieldValues);
        processingRecord.metadataChanges.put(fieldName, updatedFieldRecord);
    }

    private FieldValue createBase64FieldValue(String value){
        return FieldValueFactory.create( value, FieldEncoding.base64 );
    }

    private FieldChangeRecord createFieldChangeRecord(FieldChangeType changeType, Collection<FieldValue> changeValues){
        FieldChangeRecord fieldChangeRecord = new FieldChangeRecord();
        fieldChangeRecord.changeType = changeType;
        fieldChangeRecord.changeValues = changeValues;
        return fieldChangeRecord;
    }
}
