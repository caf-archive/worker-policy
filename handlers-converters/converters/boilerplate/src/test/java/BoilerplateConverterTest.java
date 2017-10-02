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
import com.github.cafdataprocessing.worker.policy.converter.qa.ConvertHelper;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterException;
import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.worker.policy.converters.boilerplate.BoilerplateFields;
import com.github.cafdataprocessing.worker.policy.converters.boilerplate.BoilerplateWorkerConverter;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.policyworker.policyboilerplatefields.PolicyBoilerplateFields;
import com.hpe.caf.util.ref.DataSource;
import com.hpe.caf.util.ref.DataSourceException;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.boilerplateshared.BoilerplateWorkerConstants;
import com.hpe.caf.worker.boilerplateshared.response.BoilerplateMatch;
import com.hpe.caf.worker.boilerplateshared.response.BoilerplateResult;
import com.hpe.caf.worker.boilerplateshared.response.BoilerplateWorkerResponse;
import com.hpe.caf.worker.boilerplateshared.response.SignatureExtractStatus;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Tests for the Boilerplate Converter class.
 */
@RunWith(MockitoJUnitRunner.class)
public class BoilerplateConverterTest {
    @Mock
    DataStore dataStore;
    @Mock
    DataSource dataSource;

    private void checkDataFieldsUnchanged(Multimap<String, String> originalDocMetadata, DocumentInterface document){
        Multimap<String, String> docFields = document.getMetadata();
        for(String fieldName: originalDocMetadata.keySet()){
            Collection<String> returnedFields = docFields.get(fieldName);
            Collection<String> originalFields = originalDocMetadata.get(fieldName);

            //check that each original field value exists in returned field values
            Assert.assertEquals("Expecting same number of " + fieldName + " fields.", originalFields.size(), returnedFields.size());
            for(String originalField: originalFields){
                Assert.assertTrue("Expecting returned fields to have value of original field", returnedFields.contains(originalField));
            }
        }
    }

    private class ModifiedDataModel{
        public String originalData;
        public String updatedData;
        public String fieldName;

        public ModifiedDataModel(String fieldName, String originalData, String updatedData){
            this.fieldName = fieldName;
            this.originalData = originalData;
            this.updatedData = updatedData;
        }
    }

    private void checkDataFieldsUpdated(Collection<ModifiedDataModel> expectedDataCollection, DocumentInterface document) throws IOException, DataSourceException {
        Multimap<String, String> docFields = document.getMetadata();
        Multimap<String, ReferencedData> docRefData = document.getMetadataReferences();

        for(ModifiedDataModel expectedData: expectedDataCollection) {
            String fieldName = expectedData.fieldName;
            //check that the field is not on metadata
            Collection<String> metadataEntries = docFields.get(fieldName);
            Assert.assertEquals("Expecting metadata to have no entries for field " + fieldName, 0, metadataEntries.size());

            //check the field is on metadata references
            Collection<ReferencedData> metadataReferenceEntries = docRefData.get(fieldName);
            Assert.assertTrue("Expecting at least one metadata reference field for " + fieldName, !metadataReferenceEntries.isEmpty());

            String expectedDataValueAsString = expectedData.updatedData;
            boolean foundUpdatedContentMatch = false;
            //old data should not be present on document
            String oldDataValue = expectedData.originalData;
            boolean foundOldContentMatch = false;

            for(ReferencedData metadataReferenceEntry : metadataReferenceEntries ){
                InputStream returnedDataValueAsStream = metadataReferenceEntry.acquire(this.dataSource);
                String returnedDataValueAsString = IOUtils.toString(returnedDataValueAsStream);
                if(expectedDataValueAsString.equals(returnedDataValueAsString)){
                    foundUpdatedContentMatch = true;
                }
                if(oldDataValue.equals(returnedDataValueAsString)){
                    foundOldContentMatch = true;
                }
            }
            Assert.assertTrue("Expecting to find modified data on document for field "+fieldName, foundUpdatedContentMatch);
            if(oldDataValue!=expectedDataValueAsString){
                Assert.assertTrue("Original data should not be present on document after being redacted/removed.", !foundOldContentMatch);
            }
        }
    }

    private Multimap<String, String> createMetadata(String fieldName, String... dataValues){
        Multimap<String, String> docMetadata = ArrayListMultimap.create();
        for(String dataValue: dataValues){
            docMetadata.put(fieldName, dataValue);
        }
        return docMetadata;
    }

    private void addBoilerplateMatches(BoilerplateResult bpResult, BoilerplateMatch... bpMatches){
        Collection<BoilerplateMatch> boilerplateMatches = new ArrayList<>();
        for(BoilerplateMatch bpMatch: bpMatches){
            boilerplateMatches.add(bpMatch);
        }
        bpResult.setMatches(boilerplateMatches);
    }

    private Collection<ReferencedData> getBoilerplateData(String fieldName, String... dataValues){
        Collection<ReferencedData> bpData = new ArrayList<>();
        for(String dataValue:dataValues){
            ReferencedData bpRefData1 = ReferencedData.getWrappedData(fieldName, dataValue.getBytes());
            bpData.add(bpRefData1);
        }
        return bpData;
    }

    private BoilerplateMatch createBoilerplateMatch(Long id, String value){
        Long boilerplateMatchId = id;
        String boilerplateMatchValue = value;
        BoilerplateMatch boilerplateMatch = new BoilerplateMatch();
        boilerplateMatch.setBoilerplateId(boilerplateMatchId);
        boilerplateMatch.setValue(boilerplateMatchValue);
        return boilerplateMatch;
    }

    private void checkMatchFields(Collection<BoilerplateMatch> bpMatches, DocumentInterface document) throws DataSourceException, IOException {
        Multimap<String, String> docFields = document.getMetadata();
        String expectedMatchIdFieldName = BoilerplateFields.BOILERPLATE_MATCH_ID;
        Collection<String> boilerplateMatchIdFields = docFields.get(expectedMatchIdFieldName);
        //look for boilerplate match fields on document
        for(BoilerplateMatch bpMatch: bpMatches){
            String bpId = bpMatch.getBoilerplateId().toString();

            String boilerplateMatchIdFieldValue = boilerplateMatchIdFields.stream().filter(i -> i.equals(bpId)).findFirst().orElse(null);

            Assert.assertNotNull("Expecting match ID field on document with this Boilerplate ID as the value",
                    boilerplateMatchIdFieldValue);

            Collection<String> boilerplateFields = docFields.get(BoilerplateFields.getMatchValueFieldName(bpId));
            //look for a match value field that has this boilerplate match value
            String bpValue = bpMatch.getValue();
            String bpFieldValue = boilerplateFields.stream().filter(f -> f.equals(bpValue)).findFirst().orElse(null);
            Assert.assertNotNull("Expecting match ID field value on document.", bpFieldValue);
        }
    }

    private static DocumentInterface createDocument(String reference) {
        DocumentInterface document = new Document();
        document.setReference(reference);
        return document;
    }

    /*
        DoNothing illustrated by no modified data being returned on BoilerplateWorkerResponse. The fields matches occurred on should be unchanged.
     */
    @Test
    public void checkTwoBoilerplateMatchesSingleFieldWithDoNothing() throws CodecException,
            InvalidTaskException, DataSourceException, IOException, PolicyWorkerConverterException
    {
        ReferencedData documentReference = ReferencedData.getReferencedData(UUID.randomUUID().toString());

        //create worker response
        BoilerplateWorkerResponse bpResponse = new BoilerplateWorkerResponse();
        //create boilerplate task results
        Map<String, BoilerplateResult> taskResults = new HashMap<>();
        String bpResultFieldName1 = "CONTENT";
        BoilerplateResult bpResult1 = new BoilerplateResult();

        BoilerplateMatch boilerplateMatch1 = createBoilerplateMatch(1L, "HP Ltd");
        BoilerplateMatch boilerplateMatch2 = createBoilerplateMatch(2L, "7Up");

        Collection<BoilerplateMatch> boilerplateMatches = new ArrayList<>();
        boilerplateMatches.add(boilerplateMatch1);
        boilerplateMatches.add(boilerplateMatch2);

        bpResult1.setMatches(boilerplateMatches);
        taskResults.put(bpResultFieldName1, bpResult1);
        bpResponse.setTaskResults(taskResults);

        //prepare document
        Multimap<String, String> originalDocMetadata = ArrayListMultimap.create();
        originalDocMetadata.put("CONTENT", "Old test HP Ltd value 1");
        originalDocMetadata.put("CONTENT", "Old test value HP Ltd 2");
        originalDocMetadata.put("CONTENT", "Old test value 3");

        DocumentInterface doc = ConvertHelper.RunConvert(bpResponse, documentReference, this.dataStore, TaskStatus.RESULT_SUCCESS,
                new BoilerplateWorkerConverter(), "BoilerplateWorker", originalDocMetadata, null);

        Assert.assertNotNull("The converted response should not be null", doc);

        //check for match fields on document
        checkMatchFields(boilerplateMatches, doc);
        //check content fields are as they were
        checkDataFieldsUnchanged(originalDocMetadata, doc);
    }


    @Test
    public void testEmailKeyContent() throws CodecException, InvalidTaskException, PolicyWorkerConverterException
    {
        ReferencedData documentReference = ReferencedData.getReferencedData(UUID.randomUUID().toString());

        ReferencedData primaryReference = ReferencedData.getReferencedData(UUID.randomUUID().toString());
        ReferencedData secondaryReference = ReferencedData.getReferencedData(UUID.randomUUID().toString());
        ReferencedData tertiaryReference = ReferencedData.getReferencedData(UUID.randomUUID().toString());

        //create worker response
        BoilerplateWorkerResponse response = new BoilerplateWorkerResponse();
        //create boilerplate task results
        Map<String, BoilerplateResult> taskResults = new HashMap<>();
        String resultField = "CONTENT";
        BoilerplateResult boilerplateResult = new BoilerplateResult();
        boilerplateResult.setGroupedMatches(HashMultimap.create());
        boilerplateResult.getGroupedMatches().put(BoilerplateWorkerConstants.PRIMARY_CONTENT, primaryReference);
        boilerplateResult.getGroupedMatches().put(BoilerplateWorkerConstants.SECONDARY_CONTENT, secondaryReference);
        boilerplateResult.getGroupedMatches().put(BoilerplateWorkerConstants.TERTIARY_CONTENT, tertiaryReference);

        taskResults.put(resultField,boilerplateResult);
        response.setTaskResults(taskResults);

        //prepare document
        Multimap<String, String> originalDocMetadata = ArrayListMultimap.create();
        String primaryFieldName = "MY_PRIMARY_FIELD";
        String secondaryFieldName = "MY_SECONDARY_FIELD";
        originalDocMetadata.put(PolicyBoilerplateFields.POLICY_BOILERPLATE_PRIMARY_FIELDNAME, primaryFieldName);
        originalDocMetadata.put(PolicyBoilerplateFields.POLICY_BOILERPLATE_SECONDARY_FIELDNAME, secondaryFieldName);

        DocumentInterface doc = ConvertHelper.RunConvert(response, documentReference, this.dataStore, TaskStatus.RESULT_SUCCESS,
                new BoilerplateWorkerConverter(), "BoilerplateWorker", originalDocMetadata, null);

        Multimap<String, String> documentMetadata = doc.getMetadata();
        Multimap<String, ReferencedData> documentReferenceMetadata = doc.getMetadataReferences();
        //Check if key content fields have been added with correct names.
        Assert.assertTrue("Document meta data should contain Primary Content Field Name", documentReferenceMetadata.containsKey(primaryFieldName));
        Assert.assertTrue("Document meta data should contain Secondary Content Field Name", documentReferenceMetadata.containsKey(secondaryFieldName));
        Assert.assertTrue("Document meta data should contain Tertiary Content Field Name", documentReferenceMetadata.containsKey(BoilerplateWorkerConstants.TERTIARY_CONTENT));

        //Check if key content fields have the correct values.
        Assert.assertEquals("Primary content should match", primaryReference.getReference(), documentReferenceMetadata.get(primaryFieldName).stream().findFirst().get().getReference());
        Assert.assertEquals("Secondary content should match", secondaryReference.getReference(), documentReferenceMetadata.get(secondaryFieldName).stream().findFirst().get().getReference());
        Assert.assertEquals("Primary content should match", tertiaryReference.getReference(), documentReferenceMetadata.get(BoilerplateWorkerConstants.TERTIARY_CONTENT).stream().findFirst().get().getReference());

        //Check if temporary policy fields have been removed.
        Assert.assertTrue("Document meta data should not contain the primary content field name's field name",
                !documentMetadata.containsKey(PolicyBoilerplateFields.POLICY_BOILERPLATE_PRIMARY_FIELDNAME));
        Assert.assertTrue("Document meta data should not contain the secondary content field name's field name",
                !documentMetadata.containsKey(PolicyBoilerplateFields.POLICY_BOILERPLATE_SECONDARY_FIELDNAME));
        Assert.assertTrue("Document meta data should not contain the tertiary content field name's field name",
                !documentMetadata.containsKey(PolicyBoilerplateFields.POLICY_BOILERPLATE_TERTIARY_FIELDNAME));
    }


    @Test
    public void testEmailSignatureDetection() throws CodecException, InvalidTaskException, PolicyWorkerConverterException
    {
        ReferencedData documentReference = ReferencedData.getReferencedData(UUID.randomUUID().toString());

        ReferencedData firstEmailSignatureReference = ReferencedData.getReferencedData(UUID.randomUUID().toString());
        ReferencedData secondEmailSignatureReference = ReferencedData.getReferencedData(UUID.randomUUID().toString());
        ReferencedData thirdEmailSignatureReference = ReferencedData.getReferencedData(UUID.randomUUID().toString());

        //create worker response
        BoilerplateWorkerResponse response = new BoilerplateWorkerResponse();
        //create boilerplate task results
        Map<String, BoilerplateResult> taskResults = new HashMap<>();
        String resultField = "CONTENT";
        BoilerplateResult boilerplateResult = new BoilerplateResult();
        boilerplateResult.setGroupedMatches(HashMultimap.create());
        boilerplateResult.getGroupedMatches().put(BoilerplateWorkerConstants.EXTRACTED_SIGNATURES, firstEmailSignatureReference);
        boilerplateResult.getGroupedMatches().put(BoilerplateWorkerConstants.EXTRACTED_SIGNATURES, secondEmailSignatureReference);
        boilerplateResult.getGroupedMatches().put(BoilerplateWorkerConstants.EXTRACTED_SIGNATURES, thirdEmailSignatureReference);
        boilerplateResult.setSignatureExtractStatus(SignatureExtractStatus.SIGNATURES_EXTRACTED);

        taskResults.put(resultField, boilerplateResult);
        response.setTaskResults(taskResults);

        //prepare document
        Multimap<String, String> originalDocMetadata = ArrayListMultimap.create();
        String fieldName = "MY_EXTRACTED_EMAIL_SIGNATURES_FIELD";
        originalDocMetadata.put(PolicyBoilerplateFields.POLICY_BOILERPLATE_EMAIL_SIGNATURES_FIELDNAME, fieldName);

        DocumentInterface doc = ConvertHelper.RunConvert(response, documentReference, this.dataStore, TaskStatus.RESULT_SUCCESS,
                new BoilerplateWorkerConverter(), "BoilerplateWorker", originalDocMetadata, null);

        Multimap<String, ReferencedData> documentReferenceMetadata = doc.getMetadataReferences();
        //Check if extracted email signature field has been added with the correct name.
        Assert.assertTrue("Document meta data should contain Extracted Email Signatures Field Name", documentReferenceMetadata.containsKey(fieldName));

        //Check if extracted email signature field has the correct values.
        Collection<ReferencedData> extractedSignaturesFieldName = documentReferenceMetadata.get(fieldName);
        Assert.assertEquals("First content should match", firstEmailSignatureReference.getReference(), extractedSignaturesFieldName.stream().filter(referencedData -> referencedData.getReference().equals(firstEmailSignatureReference.getReference())).findFirst().get().getReference());
        Assert.assertEquals("Second content should match", secondEmailSignatureReference.getReference(), extractedSignaturesFieldName.stream().filter(referencedData -> referencedData.getReference().equals(secondEmailSignatureReference.getReference())).findFirst().get().getReference());
        Assert.assertEquals("Third content should match", thirdEmailSignatureReference.getReference(), extractedSignaturesFieldName.stream().filter(referencedData -> referencedData.getReference().equals(thirdEmailSignatureReference.getReference())).findFirst().get().getReference());

        //Check if temporary policy field have been removed.
        Assert.assertTrue("Document meta data should not contain the Extracted Email Signature Fields",
                !documentReferenceMetadata.containsKey(PolicyBoilerplateFields.POLICY_BOILERPLATE_EMAIL_SIGNATURES_FIELDNAME));
    }


    @Test
    public void checkMultipleFieldsModifiedDataVaryingNumberMatches() throws CodecException, InvalidTaskException, IOException, DataSourceException, PolicyWorkerConverterException
    {
        ReferencedData documentReference = ReferencedData.getReferencedData(UUID.randomUUID().toString());
        //create worker response
        BoilerplateWorkerResponse bpResponse = new BoilerplateWorkerResponse();
        //create boilerplate task results
        Map<String, BoilerplateResult> taskResults = new HashMap<>();
        //create first boilerplate result
        String bpResultFieldName1 = "CONTENT";
        BoilerplateResult bpResult1 = new BoilerplateResult();
        BoilerplateMatch boilerplate1Match1 = createBoilerplateMatch(1L, "HP Ltd");
        BoilerplateMatch boilerplate1Match2 = createBoilerplateMatch(2L, "7Up");
        addBoilerplateMatches(bpResult1, boilerplate1Match1, boilerplate1Match2);

        String newData1Value1 = "Test value 1";
        String newData1Value2 = "Test value 2";
        String newData1Value3 = "Test value 3";
        Collection<ReferencedData> bpData1 = getBoilerplateData(bpResultFieldName1, newData1Value1, newData1Value2, newData1Value3);
        bpResult1.setData(bpData1);
        taskResults.put(bpResultFieldName1, bpResult1);

        String bpResultFieldName2 = "PROP_BODY";
        BoilerplateResult bpResult2 = new BoilerplateResult();
        BoilerplateMatch boilerplate2Match1 = createBoilerplateMatch(3L, "PO BOX");
        addBoilerplateMatches(bpResult2, boilerplate2Match1);
        String newData2Value1 = "This is the new value";
        Collection<ReferencedData> bpData2 = getBoilerplateData(bpResultFieldName1, newData2Value1);
        bpResult2.setData(bpData2);
        taskResults.put(bpResultFieldName2, bpResult2);
        bpResponse.setTaskResults(taskResults);

        //prepare document
        String oldData1Value1 = "Test HP Ltd value 1";
        String oldData1Value2 = "Test value HP Ltd 2";
        String oldData2Value1 = "This is the PO Box number";
        Multimap<String, String> originalDocMetadata = createMetadata(bpResultFieldName1, oldData1Value1, oldData1Value2, newData1Value3);
        originalDocMetadata.put(bpResultFieldName2, oldData2Value1);

        DocumentInterface returnedDocument = ConvertHelper.RunConvert(bpResponse, documentReference, this.dataStore, TaskStatus.RESULT_SUCCESS,
                new BoilerplateWorkerConverter(), "BoilerplateWorker", originalDocMetadata, null);

        checkMatchFields(bpResult1.getMatches(), returnedDocument);
        checkMatchFields(bpResult2.getMatches(), returnedDocument);

        Collection<ModifiedDataModel> expectedData = new ArrayList<>();
        expectedData.add(new ModifiedDataModel(bpResultFieldName1, oldData1Value1, newData1Value1));
        expectedData.add(new ModifiedDataModel(bpResultFieldName1, oldData1Value2, newData1Value2));
        expectedData.add(new ModifiedDataModel(bpResultFieldName1, newData1Value3, newData1Value3));
        expectedData.add(new ModifiedDataModel(bpResultFieldName2, oldData2Value1, newData2Value1));

        checkDataFieldsUpdated(expectedData, returnedDocument);
    }

    @Test
    public void checkTwoFieldsMatchesRemovedData() throws CodecException, InvalidTaskException, IOException, DataSourceException, PolicyWorkerConverterException
    {
        ReferencedData documentReference = ReferencedData.getReferencedData(UUID.randomUUID().toString());

        //create worker response
        BoilerplateWorkerResponse bpResponse = new BoilerplateWorkerResponse();
        //create boilerplate task results
        Map<String, BoilerplateResult> taskResults = new HashMap<>();
        String bpResultFieldName1 = "CONTENT";
        BoilerplateResult bpResult1 = new BoilerplateResult();

        BoilerplateMatch boilerplateMatch1 = createBoilerplateMatch(1L, "HP Ltd");
        BoilerplateMatch boilerplateMatch2 = createBoilerplateMatch(2L, "7Up");
        addBoilerplateMatches(bpResult1, boilerplateMatch1, boilerplateMatch2);

        String newDataValue1 = "Test value 1";
        String newDataValue2 = "Test value 2";
        String newDataValue3 = "Test value 3";
        Collection<ReferencedData> bpData1 = getBoilerplateData(bpResultFieldName1, newDataValue1, newDataValue2, newDataValue3);
        bpResult1.setData(bpData1);

        taskResults.put(bpResultFieldName1, bpResult1);
        bpResponse.setTaskResults(taskResults);

        //prepare document
        Multimap<String, String> originalDocMetadata = ArrayListMultimap.create();
        String oldDataValue1 = "Test HP Ltd value 1";
        String oldDataValue2 = "Test value HP Ltd 2";
        originalDocMetadata.put(bpResultFieldName1, oldDataValue1);
        originalDocMetadata.put(bpResultFieldName1, oldDataValue2);
        originalDocMetadata.put(bpResultFieldName1, newDataValue3);

        DocumentInterface doc = ConvertHelper.RunConvert(bpResponse, documentReference, this.dataStore, TaskStatus.RESULT_SUCCESS,
                new BoilerplateWorkerConverter(), "BoilerplateWorker", originalDocMetadata, null);

        checkMatchFields(bpResult1.getMatches(), doc);

        Collection<ModifiedDataModel> expectedData = new ArrayList<>();
        expectedData.add(new ModifiedDataModel(bpResultFieldName1, oldDataValue1, newDataValue1));
        expectedData.add(new ModifiedDataModel(bpResultFieldName1, oldDataValue2, newDataValue2));
        expectedData.add(new ModifiedDataModel(bpResultFieldName1, newDataValue3, newDataValue3));

        checkDataFieldsUpdated(expectedData, doc);
    }
}
