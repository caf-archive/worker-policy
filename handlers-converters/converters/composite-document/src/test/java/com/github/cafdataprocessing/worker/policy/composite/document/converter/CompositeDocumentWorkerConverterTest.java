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
package com.github.cafdataprocessing.worker.policy.composite.document.converter;

import com.github.cafdataprocessing.entity.fields.DocumentProcessingRecord;
import com.github.cafdataprocessing.entity.fields.FieldChangeRecord;
import com.github.cafdataprocessing.entity.fields.FieldChangeType;
import com.github.cafdataprocessing.entity.fields.FieldValue;
import com.github.cafdataprocessing.worker.policy.common.ApiStrings;
import com.github.cafdataprocessing.worker.policy.common.DocumentFields;
import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.github.cafdataprocessing.worker.policy.shared.PolicyWorkerConstants;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerDocumentTask;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Unit tests for the CompositeDocumentWorkerConveter class
 */
public class CompositeDocumentWorkerConverterTest {

    /**
     * Test that a context and DocumentWorkerTask are correctly combined to reconstruct a Policy Worker Task
     * where the document has all metadata, metadata references and sub-documents that it should.
     */
    @Test
    public void convertPolicyWorkerContextTest() throws CodecException {
        Codec codec = new JsonCodec();
        //define the policy worker task to pass in context
        TaskData reducedPolicyWorkerTaskData = new TaskData();
        reducedPolicyWorkerTaskData.setWorkflowId(UUID.randomUUID().toString());
        reducedPolicyWorkerTaskData.setCollectionSequence(Arrays.asList(UUID.randomUUID().toString()));
        reducedPolicyWorkerTaskData.setProjectId(UUID.randomUUID().toString());
        reducedPolicyWorkerTaskData.setOutputPartialReference(UUID.randomUUID().toString());
        reducedPolicyWorkerTaskData.setExecutePolicyOnClassifiedDocuments(true);
        reducedPolicyWorkerTaskData.setPoliciesToExecute(Arrays.asList(ThreadLocalRandom.current().nextLong()));

        // prepare policy document to pass in context that will have the policy worker progress fields
        Document contextDocument = new Document();
        String contextDocReference = UUID.randomUUID().toString();
        contextDocument.setReference(contextDocReference);
        // add policy worker progress fields
        String collectionSequencesValue = "2";
        String collectonSequencesCompletedValue = "1";
        String policyWorkerFailureValue = UUID.randomUUID().toString();
        String collectionSequence_1_Value = UUID.randomUUID().toString();
        contextDocument.getMetadata().put(ApiStrings.POLICYWORKER_COLLECTION_SEQUENCE, collectionSequencesValue);
        contextDocument.getMetadata().put(ApiStrings.POLICYWORKER_COLLECTION_SEQUENCES_COMPLETED,
                collectonSequencesCompletedValue);
        contextDocument.getMetadata().put(PolicyWorkerConstants.POLICYWORKER_FAILURE, policyWorkerFailureValue);
        String executedPoliciesFieldName = DocumentFields.getTagExecutedPoliciesPerCollectionSequence(
                Long.parseLong(collectionSequencesValue));
        contextDocument.getMetadata().put(executedPoliciesFieldName,
                collectionSequence_1_Value);

        reducedPolicyWorkerTaskData.setDocument(contextDocument);
        byte[] reducedContext = codec.serialise(reducedPolicyWorkerTaskData);

        //prepare the DocumentWorkerDocumentTask which will have the other fields/sub-documents from the original document
        DocumentWorkerDocumentTask docWorkerTask = new DocumentWorkerDocumentTask();
        DocumentWorkerDocument documentWorkerDocument = new DocumentWorkerDocument();
        Map<String, List<DocumentWorkerFieldValue>> expectedDocfieldValues = new HashMap<>();
        // add some data with utf8 encoding
        String metadata_1_Name = UUID.randomUUID().toString();
        DocumentWorkerFieldValue metadata_1_FieldValue = new DocumentWorkerFieldValue();
        metadata_1_FieldValue.encoding = DocumentWorkerFieldEncoding.utf8;
        metadata_1_FieldValue.data = UUID.randomUUID().toString();
        expectedDocfieldValues.put(metadata_1_Name, Arrays.asList(metadata_1_FieldValue));

        // add some data with no encoding specified
        String metadata_2_Name = UUID.randomUUID().toString();
        DocumentWorkerFieldValue metadata_2_FieldValue_1 = new DocumentWorkerFieldValue();
        metadata_2_FieldValue_1.encoding = null;
        metadata_2_FieldValue_1.data = UUID.randomUUID().toString();
        DocumentWorkerFieldValue metadata_2_FieldValue_2 = new DocumentWorkerFieldValue();
        metadata_2_FieldValue_2.encoding = null;
        metadata_2_FieldValue_2.data = UUID.randomUUID().toString();
        expectedDocfieldValues.put(metadata_2_Name, Arrays.asList(metadata_2_FieldValue_1, metadata_2_FieldValue_2));

        //add storage reference field value
        String storageRefField_Name = UUID.randomUUID().toString();
        DocumentWorkerFieldValue storageRefField_FieldValue = new DocumentWorkerFieldValue();
        storageRefField_FieldValue.encoding = DocumentWorkerFieldEncoding.storage_ref;
        storageRefField_FieldValue.data = UUID.randomUUID().toString();
        expectedDocfieldValues.put(storageRefField_Name, Arrays.asList(storageRefField_FieldValue));

        //add base64 field
        String base64Field_Name = UUID.randomUUID().toString();
        String base64Field_Data = UUID.randomUUID().toString();
        DocumentWorkerFieldValue base64Field_FieldValue = new DocumentWorkerFieldValue();
        base64Field_FieldValue.encoding = DocumentWorkerFieldEncoding.base64;
        base64Field_FieldValue.data = new String(org.apache.commons.codec.binary.Base64.encodeBase64(
                base64Field_Data.getBytes()));
        expectedDocfieldValues.put(base64Field_Name, Arrays.asList(base64Field_FieldValue));

        documentWorkerDocument.fields = expectedDocfieldValues;

        //create some sub-documents
        DocumentWorkerDocument firstLevelSubDoc = new DocumentWorkerDocument();
        firstLevelSubDoc.reference = UUID.randomUUID().toString();
        String firstLevelSubDocFieldName = UUID.randomUUID().toString();
        DocumentWorkerFieldValue firstLevelSubDocFieldValue_1 = new DocumentWorkerFieldValue();
        firstLevelSubDocFieldValue_1.data = UUID.randomUUID().toString();
        Map<String, List<DocumentWorkerFieldValue>> firstLevelSubDocFields = new HashMap<>();
        firstLevelSubDocFields.put(firstLevelSubDocFieldName, Arrays.asList(firstLevelSubDocFieldValue_1));
        firstLevelSubDoc.fields = firstLevelSubDocFields;

        DocumentWorkerDocument secondLevelSubDoc = new DocumentWorkerDocument();
        secondLevelSubDoc.reference = UUID.randomUUID().toString();
        String secondLevelSubDocFieldName = UUID.randomUUID().toString();
        DocumentWorkerFieldValue secondLevelSubDocFieldValue_1 = new DocumentWorkerFieldValue();
        secondLevelSubDocFieldValue_1.data = UUID.randomUUID().toString();
        Map<String, List<DocumentWorkerFieldValue>> secondLevelSubDocFields = new HashMap<>();
        secondLevelSubDocFields.put(secondLevelSubDocFieldName, Arrays.asList(secondLevelSubDocFieldValue_1));
        secondLevelSubDoc.fields = secondLevelSubDocFields;

        firstLevelSubDoc.subdocuments = Arrays.asList(secondLevelSubDoc);
        documentWorkerDocument.subdocuments = Arrays.asList(firstLevelSubDoc);

        docWorkerTask.document = documentWorkerDocument;
        byte[] docWorkerTaskDataReturned = codec.serialise(docWorkerTask);

        CompositeDocumentWorkerConverter converter = new CompositeDocumentWorkerConverter();
        TaskData constructedTaskData =
                converter.convertPolicyWorkerContext(reducedContext, docWorkerTaskDataReturned, codec);
        Assert.assertNotNull("Task Data built by converter should not be null.", constructedTaskData);
        //verify returned task data has all expected information
        compareTaskDataExceptDocument(reducedPolicyWorkerTaskData, constructedTaskData);

        //check the field that were passed in the document on context are present
        Document constructedDocument = constructedTaskData.getDocument();
        Assert.assertEquals("Expected document reference to be that passed on context document.", contextDocReference,
                constructedDocument.getReference());

        Multimap<String, String> constructedMetadata = constructedDocument.getMetadata();
        Collection<String> returnedColSeqValues  = constructedMetadata.get(ApiStrings.POLICYWORKER_COLLECTION_SEQUENCE);
        Assert.assertEquals("Expecting only a single value for the collection sequence field.", 1, returnedColSeqValues.size());
        Assert.assertEquals("Value on collection sequence field should be as expected.",
                collectionSequencesValue, returnedColSeqValues.iterator().next());

        Collection<String> returnedColSeqCompletedValues  =
                constructedMetadata.get(ApiStrings.POLICYWORKER_COLLECTION_SEQUENCES_COMPLETED);
        Assert.assertEquals("Expecting only a single value for the collection sequence completed field.", 1,
                returnedColSeqCompletedValues.size());
        Assert.assertEquals("Value on collection sequence completed field should be as expected.",
                collectonSequencesCompletedValue, returnedColSeqCompletedValues.iterator().next());

        Collection<String> returnedPolWorkerFailureValues  =
                constructedMetadata.get(PolicyWorkerConstants.POLICYWORKER_FAILURE);
        Assert.assertEquals("Expecting only a single value for the policy worker failure field.", 1,
                returnedPolWorkerFailureValues.size());
        Assert.assertEquals("Value on policy worker failure field should be as expected.",
                policyWorkerFailureValue, returnedPolWorkerFailureValues.iterator().next());

        Collection<String> executedPoliciesValues  =
                constructedMetadata.get(executedPoliciesFieldName);
        Assert.assertEquals("Expecting only a single value for the executed policies field.", 1,
                executedPoliciesValues.size());
        Assert.assertEquals("Value on executed policies field should be as expected.",
                collectionSequence_1_Value, executedPoliciesValues.iterator().next());

        //check that the metadata from the document worker task is present on constructed document
        checkDocMetadataHasValues(expectedDocfieldValues, constructedMetadata);

        //check that the storage reference and base 64 value have been added as metadata references
        Multimap<String, ReferencedData> constructedMetadataReferences = constructedDocument.getMetadataReferences();
        Assert.assertEquals("There should be 2 metadata references on constructed document.",
                2, constructedMetadataReferences.size());
        Collection<ReferencedData> constructedStorageRefs = constructedMetadataReferences.get(storageRefField_Name);
        Assert.assertEquals("Expecting only one value entry for the storage ref field.", 1, constructedStorageRefs.size());
        ReferencedData constructedStorageRefValue = constructedStorageRefs.iterator().next();
        Assert.assertEquals("Reference on storage ref field should be as expected.",
                storageRefField_FieldValue.data, constructedStorageRefValue.getReference());

        Collection<ReferencedData> constructedBase64Values = constructedMetadataReferences.get(base64Field_Name);
        Assert.assertEquals("Expecting only one value entry for the base 64 field.", 1, constructedBase64Values.size());
        ReferencedData constructedBase64Value = constructedBase64Values.iterator().next();
        Assert.assertEquals("Value of base 64 field should be as expected.",
                new String(base64Field_Data.getBytes()),
                new String(constructedBase64Value.getData()));

        //check sub-documents are present
        Collection<DocumentInterface> constructedSubDocs = constructedDocument.getSubDocuments();
        Assert.assertNotNull("Sub documents on the constructed document should not be null.", constructedSubDocs);
        Assert.assertEquals("There should be one first level sub document on the constructed document.",
                1, constructedSubDocs.size());
        DocumentInterface constructedFirstLevelSubDoc = constructedSubDocs.iterator().next();
        checkSubDoc(firstLevelSubDoc, constructedFirstLevelSubDoc);
    }

    private void checkSubDoc(DocumentWorkerDocument expectedSubDoc, DocumentInterface returnedSubDoc){
        Assert.assertNotNull("Returned sub doc should not be null.", returnedSubDoc);
        Assert.assertEquals("Sub doc reference should be expected value.",
                expectedSubDoc.reference, returnedSubDoc.getReference());
        Map<String, List<DocumentWorkerFieldValue>> expectedFields = expectedSubDoc.fields;
        Multimap<String, String> returnedMetadata = returnedSubDoc.getMetadata();
        Multimap<String, ReferencedData> returnedMetadataReferences = returnedSubDoc.getMetadataReferences();
        for(Map.Entry<String, List<DocumentWorkerFieldValue>> fieldEntry: expectedFields.entrySet()){
            String expectedFieldName = fieldEntry.getKey();
            List<DocumentWorkerFieldValue> expectedFieldValues = fieldEntry.getValue();
            for(DocumentWorkerFieldValue expectedValue: expectedFieldValues){
                if(expectedValue.encoding == null || expectedValue.encoding == DocumentWorkerFieldEncoding.utf8){
                    Collection<String> returnedValues = returnedMetadata.get(expectedFieldName);
                    Assert.assertNotNull("Returned values for sub-document metadata key should not be null.",
                            returnedValues);
                    Assert.assertTrue("Expected value should be present on returned sub-doc.",
                            returnedValues.contains(expectedValue.data));
                }
                else if (expectedValue.encoding == DocumentWorkerFieldEncoding.storage_ref) {
                    Collection<ReferencedData> returnedRefs = returnedMetadataReferences.get(expectedFieldName);
                    Assert.assertNotNull("Returned values for sub-document metadata ref key should not be null.",
                            returnedRefs);
                    Assert.assertTrue("Expecting a match for the reference value on returned sub-doc.",
                            returnedRefs.stream().anyMatch(rf -> expectedValue.data.equals(rf.getReference())));
                }
                else {
                    Collection<ReferencedData> returnedRefs = returnedMetadataReferences.get(expectedFieldName);
                    Assert.assertNotNull("Returned values for sub-document metadata ref key should not be null.",
                            returnedRefs);
                    Assert.assertTrue("Expecting a match for the reference data on returned sub-doc.",
                            returnedRefs.stream().anyMatch(rf ->
                                    new String(expectedValue.data).equals(new String(rf.getData()))));
                }
            }
        }
        //check sub-docs of this document
        if(expectedSubDoc.subdocuments!=null && !expectedSubDoc.subdocuments.isEmpty()){
            Collection<DocumentInterface> returnedNextLevelSubDocs = returnedSubDoc.getSubDocuments();
            Assert.assertNotNull("Returned sub document's sub documents should not be null.", returnedNextLevelSubDocs);
            Assert.assertEquals("Returned sub document should have expected number of sub documents",
                    expectedSubDoc.subdocuments.size(), returnedNextLevelSubDocs.size());

            for(DocumentWorkerDocument nextLevelExpectedSubDoc: expectedSubDoc.subdocuments) {
                Optional<DocumentInterface> returnedNextLevelSubDocOptional = returnedNextLevelSubDocs.stream()
                        .filter(sd -> sd.getReference().equals(nextLevelExpectedSubDoc.reference))
                        .findFirst();
                Assert.assertTrue("Should have a matching next level sub-document on returned sub-document.",
                        returnedNextLevelSubDocOptional.isPresent());
                checkSubDoc(nextLevelExpectedSubDoc, returnedNextLevelSubDocOptional.get());
            }
        }
    }

    private void checkDocMetadataHasValues(Map<String, List<DocumentWorkerFieldValue>> expectedMetadata,
                                           Multimap<String, String> returnedMetadata){
        for(Map.Entry<String, List<DocumentWorkerFieldValue>> expectedEntry: expectedMetadata.entrySet()){
            String expectedKey = expectedEntry.getKey();
            List<DocumentWorkerFieldValue> expectedValues = expectedEntry.getValue();

            for(DocumentWorkerFieldValue expectedValue: expectedValues){
                if(expectedValue.encoding !=null && expectedValue.encoding != DocumentWorkerFieldEncoding.utf8){
                    continue;
                }
                Collection<String> returnedValues = returnedMetadata.get(expectedKey);
                Assert.assertNotNull("Expecting key to be present on returned metadata.", returnedValues);
                Assert.assertTrue("Expecting values for key to be present on returned metadata.", !returnedValues.isEmpty());

                Assert.assertTrue("Expecting returned values to contain the expected value.",
                        returnedValues.contains(expectedValue.data));
                Assert.assertEquals("Expecting number of values to match between expected and returned.",
                        expectedValues.size(), returnedValues.size());
            }
        }
    }

    /**
     * Test to check that context generated by the composite document worker converter class is as expected.
     */
    @Test
    public void generatePolicyWorkerContextTest() throws CodecException {
        Document providedDocument = new Document();
        String providedDocReference = UUID.randomUUID().toString();
        providedDocument.setReference(providedDocReference);
        Multimap<String, String> providedMetadata = ArrayListMultimap.create();
        //add some metadata to the document, that we don't expect to be in the returned context
        String providedMetadataKey_1 = "1" + UUID.randomUUID().toString();
        String providedMetadataKey_2 = "2" + UUID.randomUUID().toString();
        String providedMetadataKey_3 = "3" + UUID.randomUUID().toString();
        providedMetadata.put(providedMetadataKey_1, UUID.randomUUID().toString());
        providedMetadata.put(providedMetadataKey_2, UUID.randomUUID().toString());
        providedMetadata.put(providedMetadataKey_3, UUID.randomUUID().toString());
        //add the policy worker tracking fields that we expect to be retained in the context
        String collectionSequencesValue = "2";
        String collectonSequencesCompletedValue = "1";
        String policyWorkerFailureValue = UUID.randomUUID().toString();
        String collectionSequence_1_Value = UUID.randomUUID().toString();
        providedMetadata.put(ApiStrings.POLICYWORKER_COLLECTION_SEQUENCE, collectionSequencesValue);
        providedMetadata.put(ApiStrings.POLICYWORKER_COLLECTION_SEQUENCES_COMPLETED, collectonSequencesCompletedValue);
        providedMetadata.put(PolicyWorkerConstants.POLICYWORKER_FAILURE, policyWorkerFailureValue);
        providedMetadata.put(DocumentFields.getTagExecutedPoliciesPerCollectionSequence(Long.parseLong(collectionSequencesValue)),
                collectionSequence_1_Value);

        providedDocument.setMetadata(providedMetadata);
        //add some metadata reference fields to the document, that we don't expected to be in the returned context
        addMetadataReferences(providedDocument);
        //add some sub-documents to the document, which should not be in the returned context
        addSubDocs(providedDocument);
        //add policy processing record entries to the document, which should not be in the returned context
        addDocumentProcessingRecord(providedDocument);

        String providedWorkflowId = UUID.randomUUID().toString();
        TaskData providedTaskData = new TaskData();
        providedTaskData.setWorkflowId(providedWorkflowId);
        Collection<Long> providedPoliciesToExecute = Arrays.asList(1L, 2L, 3L, 4L);
        providedTaskData.setPoliciesToExecute(providedPoliciesToExecute);
        boolean providedExecutePolicy = true;
        providedTaskData.setExecutePolicyOnClassifiedDocuments(providedExecutePolicy);
        String providedOutputPartialReference = UUID.randomUUID().toString();
        providedTaskData.setOutputPartialReference(providedOutputPartialReference);
        String providedProjectId = UUID.randomUUID().toString();
        providedTaskData.setProjectId(providedProjectId);
        List<String> providedCollectionSequences = Arrays.asList("1", "2", "3", "4");
        providedTaskData.setCollectionSequence(providedCollectionSequences);
        providedTaskData.setDocument(providedDocument);

        CompositeDocumentWorkerConverter converter = new CompositeDocumentWorkerConverter();
        Codec testCodec = new JsonCodec();
        byte[] returnedContext = converter.generatePolicyWorkerContext(providedTaskData, testCodec);
        TaskData returnedTaskData = testCodec.deserialise(returnedContext, TaskData.class);

        Assert.assertNotNull("Deserialized task data should not be null.", returnedTaskData);
        Assert.assertEquals("Workflow ID on returned task data should match value passed to context generation.",
                providedTaskData.getWorkflowId(), returnedTaskData.getWorkflowId());
        Assert.assertTrue("Collection Sequences IDs on returned task data should contain all values passed to context generation.",
                returnedTaskData.getCollectionSequences().containsAll(providedTaskData.getCollectionSequences()));
        Assert.assertTrue("Collection Sequences IDs on returned task data should only be values passed to context generation.",
                providedTaskData.getCollectionSequences().containsAll(returnedTaskData.getCollectionSequences()));

        Assert.assertEquals("Output partial reference on returned task data should match value passed to context generation.",
                providedTaskData.getOutputPartialReference(), returnedTaskData.getOutputPartialReference());
        Assert.assertTrue("Policies to execute on returned task data should contain all values passed to context generation.",
                returnedTaskData.getPoliciesToExecute().containsAll(providedTaskData.getPoliciesToExecute()));
        Assert.assertTrue("Policies to execute on returned task data should only be values passed to context generation.",
                providedTaskData.getPoliciesToExecute().containsAll(returnedTaskData.getPoliciesToExecute()));
        Assert.assertEquals("Project ID on returned task data should match value passed to context generation.",
                providedTaskData.getProjectId(), returnedTaskData.getProjectId());

        Document returnedDocument = returnedTaskData.getDocument();
        Assert.assertNotNull("Returned document from context generation should not be null.", returnedDocument);
        Assert.assertEquals("Reference on returned document should match value passed to context generation.",
                providedDocReference, returnedDocument.getReference());
        Assert.assertTrue("There should be no sub-documents on the document returned from context generation.",
                returnedDocument.getSubDocuments().isEmpty());
        Assert.assertTrue("There should be no documents on the document returned from context generation.",
                returnedDocument.getDocuments().isEmpty());
        Assert.assertTrue("There should be no metadata references on the document returned from context generation.",
                returnedDocument.getMetadataReferences().isEmpty());
        Assert.assertNull("Policy data processing record should be null on the document returned from context generation.",
                returnedDocument.getPolicyDataProcessingRecord());

        Multimap<String, String> returnedMetadata = returnedDocument.getMetadata();
        checkMetadataKeyEmpty(returnedMetadata, providedMetadataKey_1);
        checkMetadataKeyEmpty(returnedMetadata, providedMetadataKey_2);
        checkMetadataKeyEmpty(returnedMetadata, providedMetadataKey_3);

        //check that temporary fields are present on returned document
        checkMetadataKeyHasExpectedValue(returnedMetadata, ApiStrings.POLICYWORKER_COLLECTION_SEQUENCE, collectionSequencesValue);
        checkMetadataKeyHasExpectedValue(returnedMetadata, ApiStrings.POLICYWORKER_COLLECTION_SEQUENCES_COMPLETED, collectonSequencesCompletedValue);
        checkMetadataKeyHasExpectedValue(returnedMetadata, PolicyWorkerConstants.POLICYWORKER_FAILURE, policyWorkerFailureValue);
        checkMetadataKeyHasExpectedValue(returnedMetadata,
                DocumentFields.getTagExecutedPoliciesPerCollectionSequence(Long.parseLong(collectionSequencesValue)),
                collectionSequence_1_Value);

    }

    private void compareTaskDataExceptDocument(TaskData expectedTaskData, TaskData returnedTaskData){
        Assert.assertEquals("Expecting workflow IDs to match.",
                expectedTaskData.getWorkflowId(), returnedTaskData.getWorkflowId());
        Assert.assertEquals("Expecting output partial reference values to match.",
                expectedTaskData.getOutputPartialReference(), returnedTaskData.getOutputPartialReference());
        Assert.assertEquals("Expecting project IDs to match.", expectedTaskData.getProjectId(), returnedTaskData.getProjectId());
        Assert.assertEquals("Expecting isExecutePolicy value to match.",
                expectedTaskData.isExecutePolicyOnClassifiedDocument(), returnedTaskData.isExecutePolicyOnClassifiedDocument());
        Assert.assertTrue("Expecting all returned collection sequences to be matches for those on expected sequences.",
                expectedTaskData.getCollectionSequences().containsAll(returnedTaskData.getCollectionSequences()));
        Assert.assertTrue("Expecting all expected collection sequences to be matches for those on returned sequences.",
                returnedTaskData.getCollectionSequences().containsAll(expectedTaskData.getCollectionSequences()));
        Assert.assertTrue("Expecting all returned policies to execute to be matches for those on expected policies to execute.",
                expectedTaskData.getPoliciesToExecute().containsAll(returnedTaskData.getPoliciesToExecute()));
        Assert.assertTrue("Expecting all expected policies to execute to be matches for those on returned policies to execute.",
                returnedTaskData.getPoliciesToExecute().containsAll(expectedTaskData.getPoliciesToExecute()));
    }

    private void checkMetadataKeyHasExpectedValue(Multimap<String, String> metadata, String key, String value){
        Collection<String> metadataValues = metadata.get(key);
        Assert.assertEquals("Expecting one metadata value returned for key"+key, 1, metadataValues.size());
        Assert.assertTrue("Value should be as expected for key " + key, metadataValues.contains(value));
    }

    private void checkMetadataKeyEmpty(Multimap<String, String> metadata, String key){
        Collection<String> metadataValues = metadata.get(key);
        Assert.assertTrue("No metadata values should exist for the key: " + key, metadataValues.isEmpty());
    }

    private void addDocumentProcessingRecord(Document documentToUpdate){
        DocumentProcessingRecord providedProcessingRecord = new DocumentProcessingRecord();
        Map<String, FieldChangeRecord> providedMetadataChanges = new HashMap<>();
        FieldChangeRecord providedFieldChangeRecord_1 = new FieldChangeRecord();
        providedFieldChangeRecord_1.changeType = FieldChangeType.added;
        FieldValue providedFieldValue_1 = new FieldValue();
        providedFieldValue_1.value = UUID.randomUUID().toString();
        providedFieldChangeRecord_1.changeValues = Arrays.asList(providedFieldValue_1);
        providedMetadataChanges.put(UUID.randomUUID().toString(), providedFieldChangeRecord_1);

        providedProcessingRecord.metadataChanges = providedMetadataChanges;

        FieldChangeRecord providedReferenceFieldChangeRecord = new FieldChangeRecord();
        providedReferenceFieldChangeRecord.changeType = FieldChangeType.updated;
        FieldValue providedReferenceFieldValue = new FieldValue();
        providedReferenceFieldValue.value = UUID.randomUUID().toString();
        providedReferenceFieldChangeRecord.changeValues = Arrays.asList(providedReferenceFieldValue);
        providedProcessingRecord.referenceChange = providedReferenceFieldChangeRecord;
        documentToUpdate.setPolicyDataProcessingRecord(providedProcessingRecord);
    }

    private void addMetadataReferences(Document documentToUpdate){
        Multimap<String, ReferencedData> providedMetadataReferences = ArrayListMultimap.create();
        String providedMetadataRefKey_1 = UUID.randomUUID().toString();
        ReferencedData providedMetadataRefValue_1 = ReferencedData.getReferencedData(UUID.randomUUID().toString());
        String providedMetadataRefKey_2 = UUID.randomUUID().toString();
        ReferencedData providedMetadataRefValue_2 = ReferencedData.getReferencedData(UUID.randomUUID().toString());
        providedMetadataReferences.put(providedMetadataRefKey_1, providedMetadataRefValue_1);
        providedMetadataReferences.put(providedMetadataRefKey_2, providedMetadataRefValue_2);
        documentToUpdate.setMetadataReferences(providedMetadataReferences);
    }

    private void addSubDocs(Document documentToUpdate){
        Document providedSubDoc_1 = new Document();
        providedSubDoc_1.setReference(UUID.randomUUID().toString());
        providedSubDoc_1.getMetadata().put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        providedSubDoc_1.getMetadataReferences()
                .put(UUID.randomUUID().toString(), ReferencedData.getReferencedData(UUID.randomUUID().toString()));
        Document providedSubDocChild = new Document();
        providedSubDocChild.setReference(UUID.randomUUID().toString());
        providedSubDocChild.getMetadata().put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        providedSubDocChild.getMetadataReferences()
                .put(UUID.randomUUID().toString(), ReferencedData.getReferencedData(UUID.randomUUID().toString()));
        providedSubDoc_1.setDocuments(Arrays.asList(providedSubDocChild));

        Document providedSubDoc_2 = new Document();
        providedSubDoc_2.setReference(UUID.randomUUID().toString());
        providedSubDoc_2.getMetadata().put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        providedSubDoc_2.getMetadataReferences()
                .put(UUID.randomUUID().toString(), ReferencedData.getReferencedData(UUID.randomUUID().toString()));

        documentToUpdate.setDocuments(Arrays.asList(providedSubDoc_1, providedSubDoc_2));
    }
}
