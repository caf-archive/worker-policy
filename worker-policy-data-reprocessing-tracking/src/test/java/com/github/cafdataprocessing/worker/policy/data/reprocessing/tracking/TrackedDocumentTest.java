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
package com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking;

import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.github.cafdataprocessing.entity.fields.*;
import com.github.cafdataprocessing.entity.fields.accessor.FieldValueAccessor;
import com.github.cafdataprocessing.entity.fields.exceptions.FieldValueAccessorException;
import com.github.cafdataprocessing.entity.fields.factory.FieldValueFactory;

import java.util.*;

import com.hpe.caf.util.ref.ReferencedData;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test the ability for TrackedDocument to monitor all types of changes to the properties it contains.
 *
 * @author trevor.getty@hpe.com
 */
public class TrackedDocumentTest {

    @Test
    public void testCreationOfTrackedDocument() {

        // test creation of blank new TrackedDocument.
        DocumentInterface document = new TrackedDocument();

        Assert.assertNotNull( "TrackedDocument metadata cannot be null", document.getMetadata() );
        Assert.assertEquals( "TrackedDocument metadata size must be 0", 0, document.getMetadata().size() );
    }

    @Test
    public void testCreationOfTrackedDocumentFromExistingDocument() {
        // test creation of blank new TrackedDocument.
        DocumentInterface originalDocument = new Document();

        // now setup some properties on this document, that we can access via the tracked document.
        originalDocument.getMetadata().put( "MyKey", "MyValue" );
        originalDocument.getMetadata().put( "MyKey2", "MyValue123" );

        // create tracked document from original document
        DocumentInterface trackedDocument = new TrackedDocument( originalDocument );

        // check our properties are all present.
        Assert.assertNotNull( "TrackedDocument metadata cannot be null", trackedDocument.getMetadata() );
        Assert.assertEquals( "TrackedDocument metadata size must match original document",
                             originalDocument.getMetadata().size(), trackedDocument.getMetadata().size() );
        Assert.assertEquals( "TrackedDocument metadata must match original document",
                             originalDocument.getMetadata().get( "MyKey" ), trackedDocument.getMetadata().get( "MyKey" ) );

    }

    @Test
    public void testUpdateDocumentReferenceValues() {

        // test creation of blank new TrackedDocument.
        DocumentInterface originalDocument = new Document();

        final String initialValue = "abc";
        final String updateValue = "xyz";

        // now setup some properties on this document, that we can access via the tracked document.
        originalDocument.setReference( initialValue );

        // create tracked document from original document
        DocumentInterface trackedDocument = new TrackedDocument( originalDocument );

        // check our properties are all present.
        Assert.assertNotNull( "TrackedDocument reference cannot be null", trackedDocument.getReference() );
        Assert.assertEquals( "TrackedDocument reference must match original document reference", initialValue,
                             trackedDocument.getReference() );

        // now update the document, check its the new value.
        trackedDocument.setReference( updateValue );
        Assert.assertEquals( "TrackedDocument reference must match updated document reference", updateValue,
                             trackedDocument.getReference() );

        {
            // Now check we have an update documentprocessingrecord, declaring this change, and it has the original value on it.
            DocumentProcessingRecord processingRecord = trackedDocument.getPolicyDataProcessingRecord();

            Assert.assertNotNull( "TrackedDocument processing record cannot be null", processingRecord );
            Assert.assertTrue( "TrackedDocument metadata records, should be empty.",
                               processingRecord.metadataChanges == null || processingRecord.metadataChanges.isEmpty() );
            Assert.assertNotNull( "TrackedDocument processing record, for reference field change must not be null.",
                                  processingRecord.referenceChange );
            Assert.assertEquals( 
                    "TrackedDocument processing record, for reference field change must contain update FieldChangeType.",
                    FieldChangeType.updated, processingRecord.referenceChange.changeType );
            Assert.assertEquals( "TrackedDocument processing record, for reference field change must initialValue.",
                                 initialValue,
                                 processingRecord.referenceChange.changeValues.stream().findFirst().get().value );
        }

        {
            // Now update the value again, and ensure no changes happen to the record.
            final String updateValue2 = "do not reflect this change.";

            trackedDocument.setReference( updateValue2 );

            Assert.assertEquals( "TrackedDocument reference must match updated document reference", updateValue2,
                                 trackedDocument.getReference() );

            // Now check we have an update documentprocessingrecord, declaring this change, and it has the original value on it.
            DocumentProcessingRecord processingRecord = trackedDocument.getPolicyDataProcessingRecord();

            Assert.assertNotNull( "TrackedDocument processing record cannot be null", processingRecord );
            Assert.assertTrue( "TrackedDocument metadata records, should be empty.",
                               processingRecord.metadataChanges == null || processingRecord.metadataChanges.isEmpty() );
            Assert.assertNotNull( "TrackedDocument processing record, for reference field change must not be null.",
                                  processingRecord.referenceChange );
            Assert.assertEquals( 
                    "TrackedDocument processing record, for reference field change must contain update FieldChangeType.",
                    FieldChangeType.updated, processingRecord.referenceChange.changeType );
            Assert.assertEquals( "TrackedDocument processing record, for reference field change must initialValue.",
                                 initialValue,
                                 processingRecord.referenceChange.changeValues.stream().findFirst().get().value );

        }
    }

    @Test
    public void testUpdateDocumentReferenceValuesToNull() {
        // we can decipher what an addition or update is between a/b or x/z.
        // but what about a->null.
        // Is this a delete or a update to null, I am treating as update to null for reference field, 
        // as this key can't be deleted unlike a metadata value ( key. )
        DocumentInterface originalDocument = new Document();

        final String initialValue = "abc";
        final String updateValue = null;

        // now setup some properties on this document, that we can access via the tracked document.
        originalDocument.setReference( initialValue );

        // create tracked document from original document
        DocumentInterface trackedDocument = new TrackedDocument( originalDocument );

        // check our properties are all present.
        Assert.assertNotNull( "TrackedDocument reference cannot be null", trackedDocument.getReference() );
        
        Assert.assertEquals( "TrackedDocument reference must match original document reference", initialValue,
                             trackedDocument.getReference() );
 
        // now update the document, check its the new value.
        trackedDocument.setReference( updateValue );
        
        if ( updateValue == null )
        {
            Assert.assertNull("TrackedDocument reference must be null", trackedDocument.getReference() );
        }
        else
        {
            Assert.assertEquals( "TrackedDocument reference must match updated document reference", updateValue,
                             trackedDocument.getReference() );
        }
        
        {
            // Now check we have an update documentprocessingrecord, declaring this change, and it has the original value on it.
            DocumentProcessingRecord processingRecord = trackedDocument.getPolicyDataProcessingRecord();

            Assert.assertNotNull( "TrackedDocument processing record cannot be null", processingRecord );
            Assert.assertTrue( "TrackedDocument metadata records, should be empty.",
                               processingRecord.metadataChanges == null || processingRecord.metadataChanges.isEmpty() );
            Assert.assertNotNull( "TrackedDocument processing record, for reference field change must not be null.",
                                  processingRecord.referenceChange );
            Assert.assertEquals( 
                    "TrackedDocument processing record, for reference field change must contain update FieldChangeType.",
                    FieldChangeType.updated, processingRecord.referenceChange.changeType );
            Assert.assertEquals( "TrackedDocument processing record, for reference field change must initialValue.",
                                 initialValue,
                                 processingRecord.referenceChange.changeValues.stream().findFirst().get().value );
        }

        {
            // Now update the value again, and ensure no changes happen to the record.
            final String updateValue2 = "do not reflect this change.";

            trackedDocument.setReference( updateValue2 );

            Assert.assertEquals( "TrackedDocument reference must match updated document reference", updateValue2,
                                 trackedDocument.getReference() );

            // Now check we have an update documentprocessingrecord, declaring this change, and it has the original value on it.
            DocumentProcessingRecord processingRecord = trackedDocument.getPolicyDataProcessingRecord();

            Assert.assertNotNull( "TrackedDocument processing record cannot be null", processingRecord );
            Assert.assertTrue( "TrackedDocument metadata records, should be empty.",
                               processingRecord.metadataChanges == null || processingRecord.metadataChanges.isEmpty() );
            Assert.assertNotNull( "TrackedDocument processing record, for reference field change must not be null.",
                                  processingRecord.referenceChange );
            Assert.assertEquals( 
                    "TrackedDocument processing record, for reference field change must contain update FieldChangeType.",
                    FieldChangeType.updated, processingRecord.referenceChange.changeType );
            Assert.assertEquals( "TrackedDocument processing record, for reference field change must initialValue.",
                                 initialValue,
                                 processingRecord.referenceChange.changeValues.stream().findFirst().get().value );

        }
    }
    
    @Test
    public void testAddNewMetadataField() throws FieldValueAccessorException { 

        // add a new key which doesn't already exist and test record, should be added / no value.
        DocumentInterface originalDocument = new Document();
        
        final String keyName = "MyField";
        final String initialValue = null;
        final String updateValue = "abc";
        final String secondUpdateValue = "do not reflect this change.";
        
        testAlterMetadataFields(originalDocument, keyName, 
                                                  initialValue, 
                                                  updateValue, 
                                                  secondUpdateValue, 
                                                  FieldChangeType.added, 
                                                  FieldChangeType.added, 
                                                  FieldChangeType.added,
                                                  FieldChangeType.added,
                                                  false );
    }
    
    @Test
    public void testAddMetadataFieldToExistingFieldRecordUpdateRecord() throws FieldValueAccessorException { 

        // add a value, to a field which already exists on the document.
        // Not adding a value to an existing field, should record an update record!!
        DocumentInterface originalDocument = new Document();
        
        final String keyName = "MyField";
        final String initialValue = "has a value.";
        final String updateValue = "adding abc should not appear in the update record!";  // add this value!
        final String secondUpdateValue = "do not reflect this change in the update record!."; // then add this value!
        
        testAlterMetadataFields(originalDocument, keyName, initialValue, updateValue, secondUpdateValue, FieldChangeType.added, FieldChangeType.updated, FieldChangeType.updated, FieldChangeType.updated, false);
    }

    @Test
    public void testAdd_ToExistingFields_ThenDeleteSameFieldValue_ResultsInNonChangingUpdateRecord(){
        
        // If possible in the future we may track UPDATE record, and merge them even more, to remove them when they
        // are cancelled out.  But for now once an update, always an update!  IT is always safe as it deletes whatever
        // is present, and sets to its give state.  So even though this COULD merge the add/delete together and get no change, 
        // in reality it stays an update record!
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final String value1 = "abc";
        final String value2 = "def";
        final String value3 = "ghi";
        
        // so we start with abc,def   
        final Collection<String> initialValues = Arrays.asList(value1,value2);
        // add ghi
        final Collection<String> updatedValues = Arrays.asList(value3);
        // then delete, ghi
        final Collection<String> secondUpdateValues = Arrays.asList(value3);
        
   
        
        testAlterMetadataFields( originalDocument, keyName, initialValues, updatedValues, 
                                                                          secondUpdateValues, 
                                                                          FieldChangeType.added, 
                                                                          FieldChangeType.deleted, 
                                                                          FieldChangeType.updated, 
                                                                          FieldChangeType.updated, false );
    }
    
     @Test
    public void testAdd_ToExistingFields_ThenDeleteSameFieldValue_AsReferencedDataType_ResultsInNonChangingUpdateRecord(){
        
        // If possible in the future we may track UPDATE record, and merge them even more, to remove them when they
        // are cancelled out.  But for now once an update, always an update!  IT is always safe as it deletes whatever
        // is present, and sets to its give state.  So even though this COULD merge the add/delete together and get no change, 
        // in reality it stays an update record!
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final String value1 = "abc";
        final String value2 = "def";
        final String value3 = "ghi";
        final FieldValue value3_asReferencedData = FieldValueFactory.create( value3, FieldEncoding.base64 );
        final ReferencedData value3_data = FieldValueAccessor.createReferencedDataFromFieldValue( value3_asReferencedData );
        
        // so we start with abc,def   
        final Collection<String> initialValues = Arrays.asList(value1,value2);
        // add ghi
        final Collection<String> updatedValues = Arrays.asList(value3);
        // then delete, ghi ( but delete it as a ReferencedData object ).
        final Collection<ReferencedData> secondUpdateValues = Arrays.asList(value3_data);
        
   
        
        testAlterMetadataFields( originalDocument, keyName, initialValues, updatedValues, 
                                                                          secondUpdateValues, 
                                                                          FieldChangeType.added, 
                                                                          FieldChangeType.deleted, 
                                                                          FieldChangeType.updated, 
                                                                          FieldChangeType.updated, false );
    }
    
    @Test
    public void testSetAndUpdateOfFieldValuesToNull() throws FieldValueAccessorException {
        // we can decipher what an addition or update is between a/b or x/z.
        // but what about a->null.
        // Is this a delete or a update to null, I am treating as update to null for reference but should be 
        // a delete record, if we end up with no key in the metadata list.
        
         // add a new key which doesn't already exist and test record, should be added / no value.
        DocumentInterface originalDocument = new Document();
        
        final String keyName = "MyField";
        final String initialValue = "abc";
        final String updateValue = null;
        final String secondUpdateValue = "do not reflect this change.";
        
        testAlterMetadataFields( originalDocument, 
                                 keyName, 
                                 initialValue, 
                                 updateValue, 
                                 secondUpdateValue, 
                                 FieldChangeType.updated, 
                                 FieldChangeType.updated, 
                                 FieldChangeType.updated,
                                 FieldChangeType.updated,
                                 false );
    }

    @Test
    public void testUpdate_MetadataField_singleValue(){
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final String initialValue = "abc";
        final String updateValue = "def";
        final String secondUpdateValue = "do not reflect this change.";

        testAlterMetadataFields( originalDocument, keyName, initialValue, updateValue, secondUpdateValue, 
                                                                                       FieldChangeType.updated, 
                                                                                       null, 
                                                                                       FieldChangeType.updated,
                                                                                       null, 
                                                                                       false );
    }
    
    @Test
    public void testDelete_Fields_ThenAddSameFieldValue_ResultsInNoRecord(){
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final String value1 = "abc";
        final String value2 = "def";
        final String valueToAdd = "ghi";
        
        // so we start with abc,def   
        final Collection<String> initialValues = Arrays.asList(value1,value2);
        // delete def
        final Collection<String> updatedValues = Arrays.asList(value1);
        // then readd, def
        final Collection<String> secondUpdateValues = Arrays.asList(value1);
        
   
        
        testAlterMetadataFields( originalDocument, keyName, initialValues, updatedValues, 
                                                                          secondUpdateValues, 
                                                                          FieldChangeType.deleted, 
                                                                          FieldChangeType.added, 
                                                                          FieldChangeType.deleted, null, false );
    }

    @Test
    public void testDelete_Field_ThenAddSameFieldValue_ResultsInNoRecord(){
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final String initialValue = "abc";
        final String updateValue = "abc";
        final String secondUpdateValue = "abc";

        testAlterMetadataFields( originalDocument, keyName, initialValue, updateValue, 
                                                                          secondUpdateValue, 
                                                                          FieldChangeType.deleted, 
                                                                          FieldChangeType.added, 
                                                                          FieldChangeType.deleted, 
                                                                          null, 
                                                                          false );
    }

    @Test
    public void testUpdate_MetadataField_multivalue_addvalueSimple(){
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final String value1 = "abc";
        final String value2 = "def";
        final String valueToAdd = "ghi";
        final Collection<String> initialValues = Arrays.asList(value1,value2);
        final Collection<String> updatedValues = Arrays.asList(value1,value2,valueToAdd);
        final Collection<String> secondUpdateValues = null;
   
        // Testing via simple, and existing way!
        DocumentInterface originalDocument2 = new Document();
        testAlterMetadataFields( originalDocument2, keyName, initialValues, updatedValues, secondUpdateValues, FieldChangeType.added, null, FieldChangeType.updated, null, false );

        originalDocument.getMetadata().putAll(keyName, initialValues);

        TrackedDocument trackedDocument = new TrackedDocument(originalDocument);
        // now track the change to the metadata!
        trackedDocument.getMetadata().put(keyName, valueToAdd);

        //expecting an update record to exist now
        DocumentProcessingRecord record = trackedDocument.getPolicyDataProcessingRecord();
        Assert.assertNotNull("processing record should exist to reflect new value added", record);
        Assert.assertNull("Should be no reference change on processing record.", record.referenceChange);
        Assert.assertNotNull("Expecting metadata change recorded on processing record", record.metadataChanges);
        Assert.assertEquals("Expecting only one metadata change on processing record", 1, record.metadataChanges.size());
        Collection<String> expectedUpdatedValues = Arrays.asList(value1, value2);
        for(Map.Entry<String, FieldChangeRecord> metadataChange : record.metadataChanges.entrySet()){
            Assert.assertEquals("Expecting key for metadata change to be field we specified", keyName,
                    metadataChange.getKey());
            FieldChangeRecord fieldChangeRecord = metadataChange.getValue();
            Assert.assertEquals("Expecting this addition of a value to have been recorded as an update.",
                    fieldChangeRecord.changeType, FieldChangeType.updated);
            //verify it contains the values we expect from 'original' state
            Assert.assertEquals("Should have expected number of 'original' values recorded.",
                    expectedUpdatedValues.size(), fieldChangeRecord.changeValues.size());

            for(FieldValue fieldValue : fieldChangeRecord.changeValues){
                Assert.assertTrue("Encoding on field value should be utf8", ( fieldValue.valueEncoding == null || fieldValue.valueEncoding == FieldEncoding.utf8 ));
                Assert.assertTrue("Value on FieldValue should be one of the expected values. Value looking for is: " +
                        fieldValue.value,
                        expectedUpdatedValues.contains(fieldValue.value));
            }

        }

    }

    @Test
    public void testUpdate_MetadataField_multivalue_replaceValue(){
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final String value1 = "abc";
        final String value2 = "def";
        final String valueToAdd = "ghi";
        final Collection<String> initialValues = Arrays.asList(value1,value2);
        final Collection<String> updatedValues = Arrays.asList(value1,valueToAdd);
        final Collection<String> secondUpdateValues = null;

        testAlterMetadataFields( originalDocument, keyName, initialValues, updatedValues, secondUpdateValues, FieldChangeType.updated, null, FieldChangeType.updated, null );
    }

    @Test
    public void testUpdate_MetadataField_multivalue_replaceAllValues(){
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final String value1 = "abc";
        final String value2 = "def";
        final String valueToAdd = "ghi";
        final Collection<String> initialValues = Arrays.asList(value1,value2);
        final Collection<String> updatedValues = Arrays.asList(valueToAdd);
        final Collection<String> secondUpdateValues = null;

        testAlterMetadataFields( originalDocument, keyName, initialValues, updatedValues, secondUpdateValues, FieldChangeType.updated, null, FieldChangeType.updated, null );
    }
    
    @Test
    public void testDeleteMetadataFieldOnlyValue() throws FieldValueAccessorException { 

        // add a new key which doesn't already exist and test record, should be added / no value.
        DocumentInterface originalDocument = new Document();
        
        final String keyName = "MyField";
        final String initialValue = "has a value.";
        final String deleteValue = "has a value.";
        final String secondUpdateValue = "do not reflect this change.";
        
        testAlterMetadataFields(originalDocument, keyName, initialValue, deleteValue, secondUpdateValue, FieldChangeType.deleted, 
                                                                                                         FieldChangeType.added, 
                                                                                                         FieldChangeType.deleted,
                                                                                                         FieldChangeType.updated,
                                                                                                         false);
    }
        
    @Test
    public void testDeleteMetadataFieldValueFromValues() throws FieldValueAccessorException { 

        // Test should cover having multiple values in a field.
        // "myfield" : [ "abc", "def" ]
        // then remove the abc field.
        // we should have a deleted record for "abc".
        DocumentInterface originalDocument = new Document();
        
        final String keyName = "MyField";
        Collection<String> initialValues = new ArrayList<>();
        Collection<String> deleteValues = new ArrayList<>();
        
        initialValues.add( "abc" );
        initialValues.add("def");
        
        deleteValues.add("abc");
                
        DocumentInterface trackedDocument = 
                testAlterMetadataFields( originalDocument, keyName, initialValues, deleteValues, null, FieldChangeType.deleted, null, FieldChangeType.deleted, null );
        
        Assert.assertNotNull("Must competed with a tracked document returned");
    }
    
    
    @Test
    public void testDeleteMetadataFieldValueThenAnotherDeleteRecordASingleDeleteRecordWithValues() throws FieldValueAccessorException { 

        // Test should cover having multiple values in a field.
        // "myfield" : [ "abc", "def" ]
        // then remove the abc field.
        // we should have a deleted record for "abc".
        DocumentInterface originalDocument = new Document();
        
        final String keyName = "MyField";
        Collection<String> initialValues = new ArrayList<>();
        Collection<String> deleteValues = new ArrayList<>();
        
        initialValues.add( "abc" );
        initialValues.add("def");
        initialValues.add("ghi");
        
        deleteValues.add("abc");
        
        Collection<String> secondDeleteValues = new ArrayList();
        secondDeleteValues.add("def");
                
        DocumentInterface trackedDocument = 
                testAlterMetadataFields( originalDocument, keyName, initialValues, deleteValues, secondDeleteValues, 
                                                                                                 FieldChangeType.deleted, 
                                                                                                 FieldChangeType.deleted, 
                                                                                                 FieldChangeType.deleted, 
                                                                                                 FieldChangeType.deleted, 
                                                                                                 false );
        
        Assert.assertNotNull("Must competed with a tracked document returned",trackedDocument);
    }
    
    @Test
    public void testDeleteMetadataFieldAllValues() throws FieldValueAccessorException { 

        // add a new key which doesn't already exist and test record, should be added / no value.
        DocumentInterface originalDocument = new Document();
        
        final String keyName = "MyField";
        Collection<String> initialValues = new ArrayList<>();
        
        // A null deleteValues list deletes all values!
        Collection<String> deleteValues = null;
        
        initialValues.add( "abc" );
        initialValues.add( "def" );
                
        DocumentInterface trackedDocument = 
                testAlterMetadataFields( originalDocument, keyName, initialValues, deleteValues, null, FieldChangeType.deleted, null, FieldChangeType.deleted, null);
        
        Assert.assertNotNull( "Must competed with a tracked document returned");
        
        // Check we have policy data processing record, with a deleted record with both values in it.
        DocumentProcessingRecord processingRecord = trackedDocument.getPolicyDataProcessingRecord();

        Assert.assertNotNull( "TrackedDocument processing record cannot be null", processingRecord );
        Assert.assertTrue( "TrackedDocument reference records, should be empty.",
                           processingRecord.referenceChange == null );
        Assert.assertTrue( "TrackedDocument processing record, for metadata field changes must not be null.",
                           processingRecord.metadataChanges != null && ( processingRecord.metadataChanges.size() == 1 ) );

        // Get the change record!
        Map.Entry<String, FieldChangeRecord> change = processingRecord.metadataChanges.entrySet().stream().findFirst().get();
        
        Assert.assertEquals("Tracked change record, must be for key", keyName, change.getKey());
        Assert.assertEquals("Tracked change record, must be of type deleted", FieldChangeType.deleted, change.getValue().changeType);
        
        matchExpectedChangeRecordValues(initialValues, change);

    }
    
    
    @Test
    public void testDeleteMetadataFieldValueAndThenAddANewValueToCreateUpdateRecord() throws FieldValueAccessorException { 

        /**
         * Test should cover having multiple values in a field.
         * "myfield" : [ "abc", "def" ]
         * then remove the abc field.
         * we should have a deleted record for "abc".
         * then add new value "xyz" so document has:
         * "myfield" : ["def", "xyz" ] and we should then get 
         * an update record:
         *              "myfield" : [ "abc", "def" ]
         */
        DocumentInterface originalDocument = new Document();
        
        final String keyName = "MyField";
        Collection<String> initialValues = new ArrayList<>();
        Collection<String> deleteValues = new ArrayList<>();
        
        initialValues.add("abc");
        initialValues.add("def");
        
        deleteValues.add("abc");
                
        final String secondUpdateValue = "add some other field to make update record abc,def, but should not have this value!!.";
        Collection<String> updateAfterDeleteFields = new ArrayList<>();
        updateAfterDeleteFields.add(secondUpdateValue);
        
        DocumentInterface trackedDocument = 
                testAlterMetadataFields( originalDocument, keyName, initialValues, deleteValues, updateAfterDeleteFields, FieldChangeType.deleted, FieldChangeType.added, FieldChangeType.deleted, FieldChangeType.updated );
        
        Assert.assertNotNull("Must competed with a tracked document returned");
        
    }
    
    @Test
    public void testMetadataMapBehaviour(){
        // in order that we know how the map behaves, run simple tests here, to confirm our behaviour!
        Document testDoc = new Document();
        final String myKey = "MyKeyName";
        final String value1 = "abc";
        final String value2 = "def";
        final String value3 = "any old value.";
        
        Collection<String> values = new ArrayList<>();
        values.add(value1);
        values.add(value2);
        
        testDoc = resetAsNewDocument( values, myKey );
        
        // we should have 2 fields.
        Assert.assertEquals( "fields must have x values", 2, testDoc.getMetadata().size());
        
        testDoc.getMetadata().removeAll(myKey);
        
        Assert.assertEquals("fields must have x values", 0, testDoc.getMetadata().size());
        
        Assert.assertEquals("fields must have no key after removeAll", false, testDoc.getMetadata().keySet().contains( myKey ));
        
        testDoc = resetAsNewDocument( values, myKey );
        
        // selective remove only one key now.
        testDoc.getMetadata().remove(myKey, value2);
        
        Assert.assertEquals("fields must have x values", 1, testDoc.getMetadata().size());
        Assert.assertEquals("fields must have value", value1, testDoc.getMetadata().get( myKey ).stream().findFirst().get() );
       
        // try another replaceValues with a new value3 - its really a replace with this
        testDoc = resetAsNewDocument( values, myKey );
        testDoc.getMetadata().replaceValues( myKey, Collections.singletonList( value3 ) );
        Assert.assertEquals("fields must have x values", 1, testDoc.getMetadata().size());
        Assert.assertEquals("fields must have value", value3, testDoc.getMetadata().get( myKey ).stream().findFirst().get() );
        // reset doc
        testDoc = resetAsNewDocument( values, myKey );
               
        // try replaceValues value2, which is in the list - leaving value2 still.
        testDoc.getMetadata().replaceValues( myKey, Collections.singletonList( value2 ) );
        Assert.assertEquals("fields must have x values", 1, testDoc.getMetadata().size());
        Assert.assertEquals("fields must have value", value2, testDoc.getMetadata().get( myKey ).stream().findFirst().get() );
    }


    @Test
    public void testAdd_MetadataReference_New() throws FieldValueAccessorException {

        // add a new key which doesn't already exist and test record, should be added / no value.
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final ReferencedData initialValue = null;
        final ReferencedData updateValue = ReferencedData.getReferencedData("abc");

        testAlterMetadataFields(originalDocument, keyName, initialValue, updateValue, null, FieldChangeType.added, null, FieldChangeType.added, null, true );
    }

    @Test
    public void testAdd_MetadataReference_ExistingValues() throws FieldValueAccessorException {

        // test add a value, to a key which already exists - this should result in an updated record!
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final ReferencedData initialValue = ReferencedData.getReferencedData("has a value.");
        final ReferencedData updateValue = ReferencedData.getReferencedData("abc");


        testAlterMetadataFields(originalDocument, keyName, initialValue, updateValue, null, FieldChangeType.added, null, FieldChangeType.updated, null, true);
    }


    @Test
    public void testDelete_MetatdataReference_SingleValue_RemoveAll() {
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final ReferencedData initialValue = ReferencedData.getReferencedData("has a value.");
        final ReferencedData deletedValue = null;

        DocumentInterface trackedDocument = testAlterMetadataFields(originalDocument, keyName, initialValue, deletedValue, null, FieldChangeType.deleted, null, FieldChangeType.deleted, null, true );

        Assert.assertNotNull(trackedDocument);

        FieldChangeRecord fieldChangeRecord = trackedDocument.getPolicyDataProcessingRecord().metadataChanges.get(keyName);
        Assert.assertNotNull(fieldChangeRecord);
        Assert.assertEquals("fieldChangeRecord type should be delete", FieldChangeType.deleted, fieldChangeRecord.changeType);
        Assert.assertEquals("Should have 1 changed value", 1, fieldChangeRecord.changeValues.size());
        Assert.assertEquals("Changed value should match", "has a value.", fieldChangeRecord.changeValues.stream().findFirst().orElse(new FieldValue()).value);

    }

    @Test
    public void testDelete_MetatdataReference_MultiValue_RemoveAll() {
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final ReferencedData referencedData1 = ReferencedData.getReferencedData("has a value.");
        final ReferencedData referencedData2 = ReferencedData.getReferencedData("has a second value.");
        final Collection<ReferencedData> initialValues = Arrays.asList(referencedData1, referencedData2);
        final Collection<ReferencedData> deletedValue = null;

        DocumentInterface trackedDocument = testAlterMetadataFields(originalDocument, keyName, initialValues, deletedValue, null, FieldChangeType.deleted, null, FieldChangeType.deleted, null, true);

        Assert.assertNotNull(trackedDocument);

        FieldChangeRecord fieldChangeRecord = trackedDocument.getPolicyDataProcessingRecord().metadataChanges.get(keyName);
        Assert.assertNotNull(fieldChangeRecord);
        Assert.assertEquals("fieldChangeRecord type should be delete", FieldChangeType.deleted, fieldChangeRecord.changeType);
        Assert.assertEquals("Should have 2 changed value", 2, fieldChangeRecord.changeValues.size());
        Assert.assertEquals("Changed value should match", referencedData1.getReference(), fieldChangeRecord.changeValues.stream().filter(e -> e.value.equals(referencedData1.getReference())).findFirst().orElse(new FieldValue()).value);
        Assert.assertEquals("Changed value should match", referencedData2.getReference(), fieldChangeRecord.changeValues.stream().filter(e -> e.value.equals(referencedData2.getReference())).findFirst().orElse(new FieldValue()).value);

    }

    @Test
    public void testDelete_MetatdataReference_MultiValue_removeSingleValue(){
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final ReferencedData referencedData1 = ReferencedData.getReferencedData("has a value.");
        final ReferencedData referencedData2 = ReferencedData.getReferencedData("has a second value.");
        final Collection<ReferencedData> initialValues = Arrays.asList(referencedData1, referencedData2);
        final Collection<ReferencedData> deletedValues = Arrays.asList(referencedData1);

        DocumentInterface trackedDocument = testAlterMetadataFields(originalDocument, keyName, initialValues, deletedValues, null, 
                                                                                                                             FieldChangeType.deleted, 
                                                                                                                             null,
                                                                                                                             FieldChangeType.deleted,
                                                                                                                             null, 
                                                                                                                             true);

        Assert.assertNotNull(trackedDocument);

        trackedDocument.getPolicyDataProcessingRecord().metadataChanges.get(keyName);
        FieldChangeRecord fieldChangeRecord = trackedDocument.getPolicyDataProcessingRecord().metadataChanges.get(keyName);
        Assert.assertNotNull("field change record cannot be null",fieldChangeRecord);
        Assert.assertEquals("change tpye should be delete", FieldChangeType.deleted, fieldChangeRecord.changeType);
        Assert.assertEquals("Should only be 1 changed value",1,fieldChangeRecord.changeValues.size());
        Assert.assertEquals("changed value should match",referencedData1.getReference(),fieldChangeRecord.changeValues.stream().findFirst().orElse(new FieldValue()).value);
    }

    @Test
    public void testDelete_MetadataReference_ThenUpdate(){
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final ReferencedData referencedData1 = ReferencedData.getReferencedData("has a value.");
        final ReferencedData referencedData2 = ReferencedData.getReferencedData("has a second value.");
        final ReferencedData referencedDataUpdate = ReferencedData.getReferencedData("New Data");
        final Collection<ReferencedData> initialValues = Arrays.asList(referencedData1, referencedData2);
        final Collection<ReferencedData> deletedValues = Arrays.asList(referencedData1);
        final Collection<ReferencedData> secondUpdateValues = Arrays.asList(referencedDataUpdate);

        DocumentInterface trackedDocument = testAlterMetadataFields(originalDocument, keyName, initialValues, 
                                                                                               deletedValues, 
                                                                                               secondUpdateValues, 
                                                                                               FieldChangeType.deleted, 
                                                                                               FieldChangeType.updated, 
                                                                                               FieldChangeType.deleted, 
                                                                                               FieldChangeType.updated,
                                                                                               true);

        FieldChangeRecord fieldChangeRecord = trackedDocument.getPolicyDataProcessingRecord().metadataChanges.get(keyName);
        Assert.assertNotNull("Change record cannot be null", fieldChangeRecord);
        Assert.assertEquals("Change type should be update instead of delete as field was changed after the delete", FieldChangeType.updated, fieldChangeRecord.changeType);
        
        // all of the initial values have to be in the updated record!
        checkAllReferenceValuesAreInFieldChangeRecord( initialValues, fieldChangeRecord );
    }

    @Test
    public void testUpdate_MetadataReference_singleValue_replace() {
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final ReferencedData referencedData1 = ReferencedData.getReferencedData("has a value.");
        final ReferencedData referencedData2 = ReferencedData.getReferencedData("has a second value.");
        final Collection<ReferencedData> initialValues = Arrays.asList(referencedData1);
        final Collection<ReferencedData> updatedValues = Arrays.asList(referencedData2);
        final Collection<ReferencedData> secondUpdateValues = null;
        testAlterMetadataFields(originalDocument, keyName, initialValues, updatedValues, secondUpdateValues, FieldChangeType.updated, null, FieldChangeType.updated, null, true);
    }

    @Test
    public void testUpdate_MetadataReference_MultiValue_addValue() {
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final ReferencedData referencedData1 = ReferencedData.getReferencedData("has a value.");
        final ReferencedData referencedData2 = ReferencedData.getReferencedData("has a second value.");
        final ReferencedData referencedDataToAdd = ReferencedData.getReferencedData("New Reference.");
        final Collection<ReferencedData> initialValues = Arrays.asList(referencedData1, referencedData2);
        final Collection<ReferencedData> updatedValues = Arrays.asList(referencedData1, referencedData2, referencedDataToAdd);
        final Collection<ReferencedData> secondUpdateValues = null;
        testAlterMetadataFields(originalDocument, keyName, initialValues, updatedValues, secondUpdateValues, FieldChangeType.updated, null, FieldChangeType.updated, null, true);
    }

    @Test
    public void testUpdate_MetadataReference_MultiValue_RemoveValue() {
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final ReferencedData referencedData1 = ReferencedData.getReferencedData("has a value.");
        final ReferencedData referencedData2 = ReferencedData.getReferencedData("has a second value.");
        final ReferencedData referencedDataToAdd = ReferencedData.getReferencedData("New Reference should not be recorded.");
        final Collection<ReferencedData> initialValues = Arrays.asList(referencedData1, referencedData2);
        final Collection<ReferencedData> updatedValues = Arrays.asList(referencedData1);
        final Collection<ReferencedData> secondUpdateValues = Arrays.asList(referencedDataToAdd);
        testAlterMetadataFields(originalDocument, keyName, initialValues, updatedValues, secondUpdateValues, FieldChangeType.updated, FieldChangeType.added, FieldChangeType.updated, FieldChangeType.updated, true);
    }

    @Test
    public void testUpdate_MetadataReference_MultiValue_SetToEmptyList() {
        DocumentInterface originalDocument = new Document();

        final String keyName = "MyField";
        final ReferencedData referencedData1 = ReferencedData.getReferencedData("has a value.");
        final ReferencedData referencedData2 = ReferencedData.getReferencedData("has a second value.");
        final Collection<ReferencedData> initialValues = Arrays.asList(referencedData1, referencedData2);
        final Collection<ReferencedData> updatedValues = new ArrayList<>();
        final Collection<ReferencedData> secondUpdateValues = null;
        testAlterMetadataFields(originalDocument, keyName, initialValues, updatedValues, secondUpdateValues, FieldChangeType.updated, null, FieldChangeType.updated, null, true );
    }

    /**
     * Private Methods Below here!
     */

    private Document resetAsNewDocument( Collection<String> values, final String myKey ) {
        Document testDoc;
        testDoc = new Document();
        for ( String value : values )
        {
            testDoc.getMetadata().put(myKey, value);
        }
        return testDoc;
    }

    
      private DocumentInterface testAlterMetadataFields( DocumentInterface originalDocument, 
                                                       String keyName, 
                                                       Collection<?> initialValues, 
                                                       Collection<?> updateValues, 
                                                       Collection<?> secondUpdateValues, 
                                                       FieldChangeType typeOfOperation, 
                                                       FieldChangeType secondOperationType, 
                                                       FieldChangeType expectedPolicyDataProcessingRecordChangeType,
                                                       FieldChangeType secondOperationExpectedPolicyDataProcessingRecordChangeType )
      {
          return testAlterMetadataFields( originalDocument, keyName, initialValues, updateValues, secondUpdateValues,
                                          typeOfOperation, secondOperationType,  
                                          expectedPolicyDataProcessingRecordChangeType,
                                          secondOperationExpectedPolicyDataProcessingRecordChangeType, false );
      }
      
    /**
     * Utility to create an original document, then start to track it and do some initial setup / testing,
     * this is used as the heart of several of the tests above which use metadata field changes.3
     * 
     * @param originalDocument
     * @param keyName
     * @param initialValue
     * @param updateValue
     * @return 
     */
    private DocumentInterface testAlterMetadataFields( DocumentInterface originalDocument, 
                                                       String keyName, 
                                                       Collection<?> initialValues, 
                                                       Collection<?> updateValues, 
                                                       Collection<?> secondUpdateValues, 
                                                       FieldChangeType typeOfOperation, // type of operation to perform using updateValues collection
                                                       FieldChangeType secondOperationType, // type of operation to perform 2nd using secondUpdate values
                                                       FieldChangeType expectedPolicyDataProcessingRecordChangeType, // test for this change record type
                                                       FieldChangeType secondOperationExpectedPolicyDataProcessingRecordChangeType, // test after 2nd operation for this change type
                                                       boolean useMetadataRefs ) throws FieldValueAccessorException {
       
        if ( originalDocument == null )
        {
            // create a new document now, and add initial state, before tracking takes place.
            originalDocument = new Document();
        }
        
        // now setup some properties on this document, that we can access via the tracked document.
        originalDocument.setReference( "I dont care" );
        
        // always add initial valus.
        changeDocumentAsRequested( FieldChangeType.added, originalDocument, keyName, initialValues, useMetadataRefs );
             
        // create tracked document from original document
        DocumentInterface trackedDocument = new TrackedDocument( originalDocument );

        if ( useMetadataRefs )
        {
            checkReferencedDataValuesAreEqual((Collection<ReferencedData>)initialValues, trackedDocument, keyName);
        }
        else
        {
            checkValuesAreEqual((Collection<String>)initialValues, trackedDocument, keyName);
        }
        
        // perform first change operation.
        peformChangeOperation( typeOfOperation, trackedDocument, keyName, updateValues, initialValues, expectedPolicyDataProcessingRecordChangeType, useMetadataRefs );

        {
            if ( secondOperationType == null )
            {
                // someone has requested no second operation to take place, so exit.
                return trackedDocument;
            }
            
            // Now update the value again, and ensure no changes happen to the record.
            if ( secondUpdateValues == null || secondUpdateValues.isEmpty() )
            {
                // no changes supplied for this stage!.
                return trackedDocument;
            }

            // Now check we have an update documentprocessingrecord, declaring this change, and it has the original value on it.
            changeDocumentAsRequested( secondOperationType, trackedDocument, 
                                                            keyName, 
                                                            secondUpdateValues,
                                                            useMetadataRefs );
            
            DocumentProcessingRecord processingRecord = trackedDocument.getPolicyDataProcessingRecord();
            
            Assert.assertNotNull( "TrackedDocument processing record cannot be null", processingRecord );
            Assert.assertTrue( "TrackedDocument reference records, should be empty.",
                               processingRecord.referenceChange == null );
            
            if ( secondOperationExpectedPolicyDataProcessingRecordChangeType == null )
            {
                Assert.assertTrue(  "Must contain no metadata changes, if second operation change type is null.", 
                                    processingRecord.metadataChanges == null || processingRecord.metadataChanges.isEmpty() );
                return trackedDocument;
            }
            
            Assert.assertTrue( "TrackedDocument processing record, for metadata field changes must not be null.",
                               processingRecord.metadataChanges != null );
            
            Assert.assertEquals( "TrackedDocument processing record, for metadata field changes must have x values.",
                               1, processingRecord.metadataChanges.size());
            
             // Get the change record!
            Map.Entry<String, FieldChangeRecord> change = processingRecord.metadataChanges.entrySet().stream().findFirst().get();
            
            Assert.assertEquals( 
                    "TrackedDocument processing record, for metadata field change must contain add FieldChangeType.",
                    secondOperationExpectedPolicyDataProcessingRecordChangeType, change.getValue().changeType );
            
            Assert.assertEquals( 
                    "TrackedDocument processing record, for metadata field change must be for correct fieldname.",
                    keyName, change.getKey() );
                        
                // the comparison here depends on the operation.
            switch( typeOfOperation )
            {
                case added:
                    // ?? all initial values should be there.
                    matchExpectedChangeRecordValues( initialValues, change, useMetadataRefs );
                    
                    break;
                case updated:
                    // all original values should be in the record values.
                    matchExpectedChangeRecordValues( initialValues, change, useMetadataRefs );
                    break;
                    
                case deleted:
                    // ensure that any of the deleted values are no longer on the document.
                    matchExpectedChangeRecordValues( updateValues, change, useMetadataRefs );
                    
                    // if we have deleted, and deleted I expected both original update,and second update to be present.
                    if ((typeOfOperation == FieldChangeType.deleted) && ( secondOperationType == FieldChangeType.deleted ))
                    {
                        matchExpectedChangeRecordValues( secondUpdateValues, change, useMetadataRefs );
                    }
                    break;
            }
        }
        
        return trackedDocument;
    }

    private void peformChangeOperation( FieldChangeType typeOfOperation, 
                                        DocumentInterface trackedDocument,
                                        String keyName,
                                        Collection<?> updateValues,
                                        Collection<?> initialValues, 
                                        FieldChangeType expectedDataProcessingRecordType,
                                        boolean useMetadataRefs)  throws FieldValueAccessorException, NotImplementedException {
        
        changeDocumentAsRequested( typeOfOperation, trackedDocument, keyName, updateValues, useMetadataRefs );
        
        
        {
            // Now check we have an update documentprocessingrecord, declaring this change, and it has the original value on it.
            DocumentProcessingRecord processingRecord = trackedDocument.getPolicyDataProcessingRecord();

            Assert.assertNotNull( "TrackedDocument processing record cannot be null", processingRecord );
            Assert.assertTrue( "TrackedDocument reference records, should be empty.",
                               processingRecord.referenceChange == null );

            Assert.assertTrue("TrackedDocument processing record, for metadata field changes must not be null.",
                    processingRecord.metadataChanges != null);
            
            Assert.assertEquals("TrackedDocument processing record, for metadata field changes must have x values.",
                    1, processingRecord.metadataChanges.size());
            
            // Get the change record!
            Map.Entry<String, FieldChangeRecord> change = processingRecord.metadataChanges.entrySet().stream().findFirst().get();
            
            Assert.assertEquals( 
                    "TrackedDocument processing record, for metadata field change must contain FieldChangeType we performed.",
                    expectedDataProcessingRecordType, change.getValue().changeType );
            
            Assert.assertEquals( 
                    "TrackedDocument processing record, for metadata field change must be for correct fieldname.",
                    keyName, change.getKey() );
            
            
            // the comparison here depends on the operation.
            switch( typeOfOperation )
            {
                case added:
                    // ?? all initial values should be there.
                    matchExpectedChangeRecordValues( initialValues, change, useMetadataRefs );
                    
                    break;
                case updated:
                    // all original values should be in the record values.
                    matchExpectedChangeRecordValues( initialValues, change, useMetadataRefs );
                    break;
                    
                case deleted:
                    // ensure that any of the deleted values are no longer on the document.
                    matchExpectedChangeRecordValues( updateValues, change, useMetadataRefs );
                    break;
            }
            
        }
    }

    
    private void changeDocumentAsRequested( FieldChangeType typeOfOperation, DocumentInterface trackedDocument,
                                           String keyName, Collection<?> updateValues, boolean useMetadataRefs ) throws NotImplementedException {
        
        if ( useMetadataRefs )
        {
            changeDocumentAsRequestedForMetdataRefs( typeOfOperation, trackedDocument, keyName, (Collection<ReferencedData>)updateValues );
            return;
        }
        
        changeDocumentAsRequested( typeOfOperation, trackedDocument, keyName, (Collection<String>)updateValues );
        
    }
    private void changeDocumentAsRequested( FieldChangeType typeOfOperation, DocumentInterface trackedDocument,
                                           String keyName, Collection<?> updateValues ) throws NotImplementedException {
        
        if ( typeOfOperation == null )
        {
            // if type of operation is null, it is to perform no changes!
            return;
        }
        
        switch( typeOfOperation )
        {
            case added:
            {
                trackedDocument.getMetadata().putAll( keyName, (Collection<String>)updateValues );
                break;
            }
            case deleted:
            {
                if ( updateValues == null )
                {
                    // treat having no update values, as remove all with key x.
                    trackedDocument.getMetadata().removeAll( keyName );     
                    break;
                }
                 
                
                updateValues.stream().forEach( ( value ) -> {
                    if ( value instanceof ReferencedData )
                    {
                        trackedDocument.getMetadataReferences().remove(keyName, value);
                    }
                    else
                    {
                        trackedDocument.getMetadata().remove(keyName, value);
                    }
            } );
                
                break;
            }
            case updated:
            {
                trackedDocument.getMetadata().replaceValues( keyName, (Collection<String>)updateValues );   
                break;
            }   
            default:
                throw new NotImplementedException("Dont know this test type...");
        }
    }
    
    
    private void changeDocumentAsRequestedForMetdataRefs( FieldChangeType typeOfOperation, DocumentInterface trackedDocument,
                                           String keyName, Collection<ReferencedData> updateValues ) throws NotImplementedException {
        
        if ( typeOfOperation == null )
        {
            // if type of operation is null, it is to perform no changes!
            return;
        }
        
        switch( typeOfOperation )
        {
            case added:
            {
                trackedDocument.getMetadataReferences().putAll( keyName, updateValues );
                break;
            }
            case deleted:
            {
                if ( updateValues == null || updateValues.isEmpty() )
                {
                    // treat having no update values, as remove all with key x.
                    trackedDocument.getMetadataReferences().removeAll( keyName );     
                    break;
                }
                 
                
                updateValues.stream().forEach( ( value ) -> {
                    trackedDocument.getMetadataReferences().remove(keyName, value);
            } );
                
                break;
            }
            case updated:
            {
                trackedDocument.getMetadataReferences().replaceValues( keyName, updateValues );
                break;
            }   
            default:
                throw new NotImplementedException("Dont know this test type...");
        }
    }
    
    private DocumentInterface testAlterMetadataFields( DocumentInterface originalDocument,
                                                       String keyName, 
                                                       Object initialValue, 
                                                       Object updateValue, 
                                                       Object secondUpdateValue, 
                                                       FieldChangeType testChangeType, 
                                                       FieldChangeType testSecondChangeType, 
                                                       FieldChangeType expectedDataProcessingRecordType, 
                                                       FieldChangeType secondOperationExpectedPolicyDataProcessingRecordChangeType, 
                                                       boolean useMetadataRefs ) throws FieldValueAccessorException {
        Collection<Object> initialValues = new ArrayList<>();
        if ( initialValue != null )
        {
            initialValues.add( initialValue );
        }
        
        Collection<Object> updateValues = new ArrayList<>();
        if ( updateValue != null )
        {
            updateValues.add( updateValue );
        }
        
        Collection<Object> secondUpdateValues = new ArrayList<>();
        if ( secondUpdateValue != null )
        {
            secondUpdateValues.add( secondUpdateValue );
        }
        
        
        return testAlterMetadataFields( originalDocument, 
                                        keyName, 
                                        initialValues, 
                                        updateValues, 
                                        secondUpdateValues, 
                                        testChangeType, 
                                        testSecondChangeType, 
                                        expectedDataProcessingRecordType,
                                        secondOperationExpectedPolicyDataProcessingRecordChangeType,
                                        useMetadataRefs );
    }

    
    private static void checkAllValuesArePresent( Collection<String> sourceValues, Collection<String> targetValues )
    {
        // can't compare.
        if ( sourceValues == null || sourceValues.isEmpty() )
        {
            return;
        }
        // can't compare.
        if ( targetValues == null || targetValues.isEmpty() )
        {
            return;
        }
    
        targetValues.stream().forEach( ( value ) -> {
            Assert.assertTrue( 
                    "TrackedDocument metadata must match each of the initialValues trying for value: " + value,
                    sourceValues.contains( value ) );
        } );
    }

    private static final void checkAllReferencedDataValuesArePresent( Collection<ReferencedData> sourceValues, Collection<ReferencedData> targetValues )
    {
        // can't compare.
        if ( sourceValues == null || sourceValues.isEmpty() )
        {
            return;
        }
        // can't compare.
        if ( targetValues == null || targetValues.isEmpty() )
        {
            return;
        }

        targetValues.stream().forEach( ( value ) -> {
            Assert.assertTrue(
                    "TrackedDocument metadata must match each of the initialValues trying for value: " + value,
                    sourceValues.contains( value ) );
        } );
    }
    
    /* Try to ignore the fact that its in a collection / map, try to find all matches.
     * @param initialValues
     * @param trackedDocument
     * @param keyName 
     */
    private void checkValuesAreEqual( Collection<String> initialValues, DocumentInterface trackedDocument,
                                      String keyName ) {
        // check our properties are all present.
        Assert.assertEquals( "TrackedDocument metadata must match initialValue", initialValues.size(), trackedDocument.getMetadata().get( keyName ).size() );
        
        Collection<String> metadataVals = trackedDocument.getMetadata().get( keyName );
        
        for ( String value : initialValues ) {
            boolean foundMatch = false;
            for ( String compareValue : metadataVals ) {
                if ( compareValue.equals( value ) ) {
                    foundMatch = true;
                    break;
                }
            }

            Assert.assertTrue( 
                    "TrackedDocument metadata must match each of the initialValues trying for value: " + value,
                    foundMatch );
        }
    }

    private void checkReferencedDataValuesAreEqual( Collection<ReferencedData> initialValues, DocumentInterface trackedDocument,
                                      String keyName ) {
        // check our properties are all present.
        Assert.assertEquals( "TrackedDocument metadata must match initialValue", initialValues.size(), trackedDocument.getMetadataReferences().get( keyName ).size() );

        Collection<ReferencedData> metadataRefVals = trackedDocument.getMetadataReferences().get( keyName );

        for ( ReferencedData value : initialValues ) {
            boolean foundMatch = false;
            for ( ReferencedData compareValue : metadataRefVals ) {
                if ( compareValue.getReference().equals(value.getReference()) && Arrays.equals(compareValue.getData(),value.getData()) ) {
                    foundMatch = true;
                    break;
                }
            }

            Assert.assertTrue(
                    "TrackedDocument metadata must match each of the initialValues trying for value: " + value,
                    foundMatch );
        }
    }

    // Try to ignore the fact that its in a collection / map, try to find all matches.
    private void checkValuesArePresent( Collection<String> requestedValues, DocumentInterface trackedDocument,
                                      String keyName ) {
        
        // if we have no requested values, dont bother checking for them...
        if ( requestedValues == null || requestedValues.isEmpty())
        {
            return;
        }
        
        // check our properties are all present, but both collection dont need to be exact matches.        
        Collection<String> metadataVals = trackedDocument.getMetadata().get( keyName );
        
        for ( String value : requestedValues ) {
            
            boolean foundMatch = false;
            for ( String compareValue : metadataVals ) {
                if ( compareValue.equals( value ) ) {
                    foundMatch = true;
                    break;
                }
            }

            Assert.assertTrue( 
                    "TrackedDocument metadata must contain each of the requested values - trying for value: " + value,
                    foundMatch );
        }
    }

    private void checkReferencedDataValuesArePresent( Collection<ReferencedData> requestedValues, DocumentInterface trackedDocument,
                                        String keyName ) {

        // if we have no requested values, dont bother checking for them...
        if ( requestedValues == null || requestedValues.isEmpty())
        {
            return;
        }

        // check our properties are all present, but both collection dont need to be exact matches.
        Collection<ReferencedData> metadataVals = trackedDocument.getMetadataReferences().get( keyName );

        for ( ReferencedData value : requestedValues ) {

            boolean foundMatch = false;
            for ( ReferencedData compareValue : metadataVals ) {
                if ( compareValue.getReference().equals(value.getReference()) && Arrays.equals(compareValue.getData(),value.getData())) {
                    foundMatch = true;
                    break;
                }
            }

            Assert.assertTrue(
                    "TrackedDocument metadata must contain each of the requested values - trying for value: " + value,
                    foundMatch );
        }
    }
    
    // Try to ignore the fact that its in a collection / map, try to find all matches.
    private void checkValuesAreNotPresent( Collection<String> requestedValues, 
                                              DocumentInterface trackedDocument,
                                              String keyName ) {
        
        // if we have no requested values, dont bother checking for them...
        if ( requestedValues == null || requestedValues.isEmpty())
        {
            return;
        }
        
        // check our properties are all present, but both collection dont need to be exact matches.        
        Collection<String> metadataVals = trackedDocument.getMetadata().get( keyName );
        
        for ( String value : requestedValues ) {
            
            boolean foundMatch = false;
            for ( String compareValue : metadataVals ) {
                if ( compareValue.equals( value ) ) {
                    foundMatch = true;
                    break;
                }
            }

            Assert.assertTrue( 
                    "TrackedDocument metadata must not contain each of the requested values - trying for value: " + value,
                    !foundMatch );
        }
    }

    private void checkReferencedDataValuesAreNotPresent( Collection<ReferencedData> requestedValues,
                                           DocumentInterface trackedDocument,
                                           String keyName ) {

        // if we have no requested values, dont bother checking for them...
        if ( requestedValues == null || requestedValues.isEmpty())
        {
            return;
        }

        // check our properties are all present, but both collection dont need to be exact matches.
        Collection<ReferencedData> metadataRefVals = trackedDocument.getMetadataReferences().get( keyName );

        for ( ReferencedData value : requestedValues ) {

            boolean foundMatch = false;
            for ( ReferencedData compareValue : metadataRefVals ) {
                if ( compareValue.getReference().equals(value.getReference()) && Arrays.equals(compareValue.getData(),value.getData()) ) {
                    foundMatch = true;
                    break;
                }
            }

            Assert.assertTrue(
                    "TrackedDocument metadataReferences must not contain each of the requested values - trying for value: " + value,
                    !foundMatch );
        }
    }

    /**
     * check our properties are all present, but both collection dont need to be exact matches.        
     * 
     * @param trackedDocument
     * @param keyName 
     */
    private void checkNoValuesArePresent( DocumentInterface trackedDocument, String keyName ) {
               
        // check our properties are all present, but both collection dont need to be exact matches.        
        Collection<String> metadataVals = trackedDocument.getMetadata().get( keyName );
        
        Assert.assertTrue( 
                    "TrackedDocument metadata must contain no values for key: " + keyName,
                    ( metadataVals == null || metadataVals.isEmpty()) );
    }
    
    private void matchExpectedChangeRecordValues( Collection<?> initialValues, Map.Entry<String, FieldChangeRecord> change, boolean useMetatadataRefs ) throws FieldValueAccessorException {
        
        if ( useMetatadataRefs )
        {
            matchExpectedChangeRecordReferencedDataValues( (Collection<ReferencedData>)initialValues, change);
            return;
        }
        
        matchExpectedChangeRecordValues( (Collection<String>)initialValues, change );
    }
        
    private void matchExpectedChangeRecordValues( Collection<String> initialValues, Map.Entry<String, FieldChangeRecord> change ) throws FieldValueAccessorException {
        
        // if the field change record is added, there should be no original values here to compare so skip.
        if ( initialValues == null )
        {
            return;
        }
        
        TrackedDocumentTest.this.checkAllValuesAreInFieldChangeRecord( initialValues, change.getValue() );
    }


    private void matchExpectedChangeRecordReferencedDataValues( Collection<ReferencedData> initialValues, Map.Entry<String, FieldChangeRecord> change ) throws FieldValueAccessorException {

        // if the field change record is added, there should be no original values here to compare so skip.
        if ( initialValues == null )
        {
            return;
        }

        checkAllReferenceValuesAreInFieldChangeRecord( initialValues, change.getValue() );
    }

    
    private void checkAllValuesAreInFieldChangeRecord( Collection<String> initialValues,
                                                       FieldChangeRecord change ) {
        for ( String value : initialValues )
        {
            boolean foundMatch = false;
            
            // if the type of this change, is added, we dont do the compare.
            if ( change.changeType == FieldChangeType.added )
            {
                Assert.assertNull(
                        "TrackedDocument processing record, for metadata added field change must have no values!",
                    change.changeValues );
                return;
            }
            
            for ( FieldValue updateRecordValue : change.changeValues )
            {
                String decodedValue = FieldValueAccessor.createStringFromFieldValue( updateRecordValue );
                
                if( value.equals( decodedValue ))
                {
                    foundMatch = true;
                    break;
                }
            }
            
            Assert.assertTrue( "TrackedDocument processing record, for metadata field change must contain all initialValues looking for: " + value,
                               foundMatch );
        }
    }
    
    private void checkAllReferenceValuesAreInFieldChangeRecord( Collection<ReferencedData> initialValues, FieldChangeRecord change ) {
        // if the field change record is added, there should be no original values here to compare so skip.
        for ( ReferencedData value : initialValues )
        {
            boolean foundMatch = false;

            // if the type of this change, is added, we dont do the compare.
            if ( change.changeType == FieldChangeType.added )
            {
                Assert.assertNull(
                        "TrackedDocument processing record, for metadata added field change must have no values!",
                        change.changeValues );
                return;
            }

            for ( FieldValue updateRecordValue : change.changeValues )
            {
                ReferencedData decodedValue = FieldValueAccessor.createReferencedDataFromFieldValue(updateRecordValue);

                if( value.getReference().equals(decodedValue.getReference()) && Arrays.equals(value.getData(), decodedValue.getData()) )
                {
                    foundMatch = true;
                    break;
                }
            }

            Assert.assertTrue( "TrackedDocument processing record, for metadata field change must contain all initialValues looking for: " + value,
                               foundMatch );
        }
    }

}
