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
package com.github.cafdataprocessing.worker.policy.version.tagging;

import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.entity.fields.DocumentProcessingRecord;
import com.github.cafdataprocessing.entity.fields.FieldChangeRecord;
import com.github.cafdataprocessing.entity.fields.FieldChangeType;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.TrackedDocument;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.UUID;

/**
 * Unit tests for WorkerReprocessingVersionTagging class
 */
public class PolicyReprocessingVersionTaggingTest {
    /**
     * Verify the field returned by 'getProcessingFieldName' method is the expected value.
     */
    @Test
    public void getProcessingFieldNameTest(){
        String testClassifier = UUID.randomUUID().toString();
        String expectedFieldName = "PROCESSING_"+testClassifier+"_VERSION";

        Assert.assertEquals("Expecting processing field name returned by WorkerReprocessingVersionTagging to match " +
                        "expected field name.", expectedFieldName,
                PolicyReprocessingVersionTagging.getProcessingFieldName(testClassifier));
    }

    @Test
    public void addProcessingWorkerVersionToChildrenTest() throws PolicyReprocessingVersionTaggingException {
        String testClassifier = UUID.randomUUID().toString();
        String expectedFieldName = "PROCESSING_"+testClassifier+"_VERSION";
        Document testDocument = new Document();
        //add some sub documents to the document
        DocumentInterface subDocument_1 = testDocument.addSubDocument(UUID.randomUUID().toString());
        DocumentInterface subDocument_2 = testDocument.addSubDocument(UUID.randomUUID().toString());
        //add another level of sub-documents to verify field also added to that level
        DocumentInterface secondLevelSubDocument = subDocument_1.addSubDocument(UUID.randomUUID().toString());
        //and another level of sub-documents to verify field also added to that level
        DocumentInterface thirdLevelSubDocument = secondLevelSubDocument.addSubDocument(UUID.randomUUID().toString());

        String testVersion = "testVersion";
        WorkerProcessingInfo workerProcessingInfo = new WorkerProcessingInfo(testVersion,testClassifier);

        PolicyReprocessingVersionTagging.addProcessingWorkerVersion(testDocument, workerProcessingInfo);

        //check that version field was added to the root document
        Collection<String> rootDocumentWorkerVersionValues = testDocument.getMetadata().get(expectedFieldName);
        Assert.assertEquals("Expecting one worker version field to have been added to root document.",
                1, rootDocumentWorkerVersionValues.size());
        String rootDocumentVersionFieldValue = rootDocumentWorkerVersionValues.iterator().next();
        Assert.assertEquals("Expecting worker version field added to root document to have expected value.",
                testVersion, rootDocumentVersionFieldValue);

        //check that version field was added to the first sub document at level one
        Collection<String> subDocument_1_WorkerVersionValues = subDocument_1.getMetadata().get(expectedFieldName);
        Assert.assertEquals("Expecting one worker version field to have been added to the first sub-document.",
                1, subDocument_1_WorkerVersionValues.size());
        String subDocument_1_WorkerVersionValue = subDocument_1_WorkerVersionValues.iterator().next();
        Assert.assertEquals("Expecting worker version field added to first sub-document to have expected value.",
                testVersion, subDocument_1_WorkerVersionValue);

        //check that version field was added to the second sub document at level one
        Collection<String> subDocument_2_WorkerVersionValues = subDocument_2.getMetadata().get(expectedFieldName);
        Assert.assertEquals("Expecting one worker version field to have been added to the second sub-document.",
                1, subDocument_2_WorkerVersionValues.size());
        String subDocument_2_WorkerVersionValue = subDocument_2_WorkerVersionValues.iterator().next();
        Assert.assertEquals("Expecting worker version field added to second sub-document to have expected value.",
                testVersion, subDocument_2_WorkerVersionValue);

        //check that version field was added to the sub document at level two
        Collection<String> secondLevelSubDocumentWorkerVersionValues = secondLevelSubDocument.getMetadata().get(expectedFieldName);
        Assert.assertEquals("Expecting one worker version field to have been added to the second level sub-document.",
                1, secondLevelSubDocumentWorkerVersionValues.size());
        String secondLevelSubDocumentWorkerVersionValue = secondLevelSubDocumentWorkerVersionValues.iterator().next();
        Assert.assertEquals("Expecting worker version field added to second level sub-document to have expected value.",
                testVersion, secondLevelSubDocumentWorkerVersionValue);

        //check that version field was added to the second sub document at level three
        Collection<String> thirdLevelSubDocumentWorkerVersionValues = thirdLevelSubDocument.getMetadata().get(expectedFieldName);
        Assert.assertEquals("Expecting one worker version field to have been added to the third level sub-document.",
                1, thirdLevelSubDocumentWorkerVersionValues.size());
        String thirdLevelSubDocumentWorkerVersionValue = thirdLevelSubDocumentWorkerVersionValues.iterator().next();
        Assert.assertEquals("Expecting worker version field added to third level sub-document to have expected value.",
                testVersion, thirdLevelSubDocumentWorkerVersionValue);
    }

    /**
     * Verify that null arguments passed to addProcessingWorkerVersion' method throw a WorkerReprocessingVersionTaggingException
     * exception.
     */
    @Test
    public void addProcessingWorkerVersionNullArgumentsTest(){
        callAddProcessingWorkerVersionWithNullArguments(null, null, null);
        Document testDocument = new Document();
        String testClassifier = "testClassifier";
        String testVersion = "testVersion";
        callAddProcessingWorkerVersionWithNullArguments(testDocument, null, null);
        callAddProcessingWorkerVersionWithNullArguments(testDocument, testClassifier, null);
        callAddProcessingWorkerVersionWithNullArguments(testDocument, null, testVersion);
    }

    private void callAddProcessingWorkerVersionWithNullArguments(Document document, String workerVersion, String workerClassifier){
        try {
            PolicyReprocessingVersionTagging.addProcessingWorkerVersion(document, new WorkerProcessingInfo(workerVersion, workerClassifier));
        }
        catch(PolicyReprocessingVersionTaggingException e){
            Assert.assertEquals("Expecting NullPointerException on cause of exception.", NullPointerException.class, e.getCause().getClass());
        }
    }

    /**
     * Testing that the verison field is recorded when a TrackedDocument is passed in to addProcessingWorkerVersion.
     * @throws PolicyReprocessingVersionTaggingException
     */
    @Test
    public void addProcessingWorkerVersionAddFieldTrackingDocumentTest() throws PolicyReprocessingVersionTaggingException{
        String testClassifier = UUID.randomUUID().toString();
        String testVersion = "1.5";

        //construct Document to pass in
        Document testDocument = new Document();
        //adding some dummy metadata
        String dummyFieldName = UUID.randomUUID().toString();
        String dummyFieldValue = UUID.randomUUID().toString();
        testDocument.getMetadata().put(dummyFieldName, dummyFieldValue);

        TrackedDocument trackDocument = new TrackedDocument(testDocument);
        PolicyReprocessingVersionTagging.addProcessingWorkerVersion(trackDocument, new WorkerProcessingInfo(testVersion, testClassifier));

        Multimap<String, String> testDocumentMetadata = testDocument.getMetadata();

        //verify the method didn't change existing metadata on the document
        Assert.assertEquals("Expecting two fields to be on Document metadata", 2, testDocumentMetadata.size());
        Collection<String> returnedDummyFields = testDocumentMetadata.get(dummyFieldName);
        Assert.assertEquals("Expecting Document metadata to still have original field.", 1, returnedDummyFields.size());
        String returnedDummyFieldValue = returnedDummyFields.iterator().next();
        Assert.assertEquals("Expecting dummy field value returned to be the same as original.", dummyFieldValue,
                returnedDummyFieldValue);

        //verify the worker version field was added
        Assert.assertNotNull("metadata on Document should not be null", testDocumentMetadata);

        Collection<String> versionFields = testDocumentMetadata.get(PolicyReprocessingVersionTagging.getProcessingFieldName(testClassifier));
        Assert.assertEquals("Expecting only one version field on Document", 1, versionFields.size());

        String versionField = versionFields.iterator().next();
        Assert.assertEquals("Expecting value on version field to be the testVersion property passed to addProcessingWorkerVersion",
                testVersion, versionField);

        //check processing record
        DocumentProcessingRecord record = testDocument.getPolicyDataProcessingRecord();
        Assert.assertNotNull("Expecting processing record to not be null due to recording addition of new field.", record);
        Assert.assertEquals("Expecting only one metadata change to have been recorded.", 1, record.metadataChanges.size());
        FieldChangeRecord changeRecord = record.metadataChanges.get(PolicyReprocessingVersionTagging.getProcessingFieldName(testClassifier));
        Assert.assertEquals("Expecting change type to be 'added'.", FieldChangeType.added, changeRecord.changeType);
    }

    /**
     * Verify that calling 'addProcessingWorkerVersion' method adds a metadata entry with expected field name and value.
     */
    @Test
    public void addProcessingWorkerVersionAddFieldTest() throws PolicyReprocessingVersionTaggingException {
        String testClassifier = UUID.randomUUID().toString();
        String testVersion = "1.5";

        //construct Document to pass in
        Document testDocument = new Document();
        //adding some dummy metadata
        String dummyFieldName = UUID.randomUUID().toString();
        String dummyFieldValue = UUID.randomUUID().toString();
        testDocument.getMetadata().put(dummyFieldName, dummyFieldValue);

        PolicyReprocessingVersionTagging.addProcessingWorkerVersion(testDocument, new WorkerProcessingInfo(testVersion, testClassifier));

        Multimap<String, String> testDocumentMetadata = testDocument.getMetadata();

        //verify the method didn't change existing metadata on the document
        Assert.assertEquals("Expecting two fields to be on Document metadata", 2, testDocumentMetadata.size());
        Collection<String> returnedDummyFields = testDocumentMetadata.get(dummyFieldName);
        Assert.assertEquals("Expecting Document metadata to still have original field.", 1, returnedDummyFields.size());
        String returnedDummyFieldValue = returnedDummyFields.iterator().next();
        Assert.assertEquals("Expecting dummy field value returned to be the same as original.", dummyFieldValue,
                returnedDummyFieldValue);

        //verify the worker version field was added
        Assert.assertNotNull("metadata on Document should not be null", testDocumentMetadata);

        Collection<String> versionFields = testDocumentMetadata.get(PolicyReprocessingVersionTagging.getProcessingFieldName(testClassifier));
        Assert.assertEquals("Expecting only one version field on Document", 1, versionFields.size());

        String versionField = versionFields.iterator().next();
        Assert.assertEquals("Expecting value on version field to be the testVersion property passed to addProcessingWorkerVersion",
                testVersion, versionField);
    }

    @Test
    public void addProcessingWorkerVersionAlreadyExistsFieldTest() throws PolicyReprocessingVersionTaggingException {
        String testClassifier_1 = UUID.randomUUID().toString() + "_1";
        String testVersion_1 = "1.5";

        //construct Document to pass in
        Document testDocument = new Document();
        //adding some dummy metadata
        String dummyFieldName = UUID.randomUUID().toString();
        String dummyFieldValue = UUID.randomUUID().toString();
        testDocument.getMetadata().put(dummyFieldName, dummyFieldValue);
        //add version field once
        PolicyReprocessingVersionTagging.addProcessingWorkerVersion(testDocument, new WorkerProcessingInfo(testVersion_1, testClassifier_1));
        //try to add a second time and verify the field is not added again
        PolicyReprocessingVersionTagging.addProcessingWorkerVersion(testDocument, new WorkerProcessingInfo(testVersion_1, testClassifier_1));

        Multimap<String, String> testDocumentMetadata = testDocument.getMetadata();

        //verify the method didn't change existing metadata on the document
        Assert.assertEquals("Expecting two fields to be on Document metadata", 2, testDocumentMetadata.size());
        Collection<String> returnedDummyFields = testDocumentMetadata.get(dummyFieldName);
        Assert.assertEquals("Expecting Document metadata to still have original field.", 1, returnedDummyFields.size());
        String returnedDummyFieldValue = returnedDummyFields.iterator().next();
        Assert.assertEquals("Expecting dummy field value returned to be the same as original.", dummyFieldValue,
                returnedDummyFieldValue);

        //verify the worker version field was added
        Assert.assertNotNull("metadata on Document should not be null", testDocumentMetadata);

        Collection<String> versionFields = testDocumentMetadata.get(PolicyReprocessingVersionTagging.getProcessingFieldName(testClassifier_1));
        Assert.assertEquals("Expecting only one version field on Document", 1, versionFields.size());

        String versionField = versionFields.iterator().next();
        Assert.assertEquals("Expecting value on version field to be the testVersion property passed to addProcessingWorkerVersion",
                testVersion_1, versionField);

        //verify that we can add a field with a different classifier to the document
        String testClassifier_2 = UUID.randomUUID().toString() + "_2";
        PolicyReprocessingVersionTagging.addProcessingWorkerVersion(testDocument, new WorkerProcessingInfo(testVersion_1, testClassifier_2));

        Assert.assertEquals("Expecting three fields to be on Document metadata", 3, testDocumentMetadata.size());

        verifySingleFieldValue(testDocumentMetadata, PolicyReprocessingVersionTagging.getProcessingFieldName(testClassifier_2),
                testVersion_1);

        //verify that adding field with new value results in another entry in metadata
        String testVersion_2 = UUID.randomUUID().toString();
        PolicyReprocessingVersionTagging.addProcessingWorkerVersion(testDocument, new WorkerProcessingInfo(testVersion_2, testClassifier_2));
        Assert.assertEquals("Expecting three fields to be on Document metadata", 3, testDocumentMetadata.keySet().size());

        Collection<String> returnedFields = testDocumentMetadata.get(PolicyReprocessingVersionTagging.getProcessingFieldName(testClassifier_2));
        Assert.assertEquals("Expecting Document metadata to two version values", 2, returnedFields.size());
        Assert.assertTrue("Expected metadata to contain first version value", returnedFields.contains(testVersion_1));
        Assert.assertTrue("Expected metadata to contain second version value", returnedFields.contains(testVersion_2));
    }

    private void verifySingleFieldValue(Multimap<String, String> testDocumentMetadata, String fieldName, String expectedFieldValue){
        Collection<String> returnedFields = testDocumentMetadata.get(fieldName);
        Assert.assertEquals("Expecting Document metadata to have expected field: "+fieldName, 1, returnedFields.size());
        String returnedFieldValue = returnedFields.iterator().next();
        Assert.assertEquals("Expecting field value returned to be expected value.", expectedFieldValue,
                returnedFieldValue);
    }
}
