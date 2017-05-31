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
package com.github.cafdataprocessing.worker.policy.handlersconverterstesting;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.github.cafdataprocessing.corepolicy.common.ClassificationApi;
import com.github.cafdataprocessing.corepolicy.common.PolicyApi;
import com.github.cafdataprocessing.corepolicy.common.WorkflowApi;
import com.github.cafdataprocessing.corepolicy.common.dto.*;
import com.github.cafdataprocessing.corepolicy.common.dto.conditions.*;
import com.github.cafdataprocessing.corepolicy.domainModels.FieldAction;
import com.github.cafdataprocessing.corepolicy.multimap.utils.CaseInsensitiveMultimap;
import com.github.cafdataprocessing.corepolicy.policy.MetadataPolicy.MetadataPolicy;
import com.github.cafdataprocessing.worker.policy.converters.classification.ClassificationWorkerConverterFields;
import com.github.cafdataprocessing.worker.policy.handlers.shared.document.SharedDocument;
import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.worker.policy.testing.shared.BaseTestsHelper;
import com.github.cafdataprocessing.worker.policy.testing.shared.IntegrationTestBase;
import com.github.cafdataprocessing.worker.policy.testing.shared.PolicyTestHelper;
import com.github.cafdataprocessing.worker.policy.testing.shared.TestDocumentHelper;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.util.rabbitmq.Event;
import com.hpe.caf.util.rabbitmq.QueueConsumer;
import com.hpe.caf.util.rabbitmq.QueuePublisher;
import com.hpe.caf.worker.datastore.fs.FileSystemDataStore;
import com.hpe.caf.worker.datastore.fs.FileSystemDataStoreConfiguration;
import com.rabbitmq.client.Channel;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

/**
 * Tests to verify that processing workflows perform as expected.
 */
public class DataProcessingIT extends BaseTestsHelper{
    private static final Logger LOGGER = LoggerFactory.getLogger(DataProcessingIT.class);

    /**
     * Queue that generic queue policies will output to
     */
    final private String queueOutput = "output";

    @Override
    protected Connection getConnectionToClearDown() {
        return null;
    }

    @Test
    public void externalClassificationTest() throws IOException, CodecException, TimeoutException, InterruptedException {
        ClassificationApi classificationApi = getClassificationApi();
        PolicyApi policyApi = IntegrationTestBase.genericApplicationContext.getBean(PolicyApi.class);

        long externalSequenceId;
        long externalPolicyId;
        String externalPolicyName;
        long externalConditionId;
        long externalCollectionId;
        String documentName = BaseTestsHelper.getUniqueString("MyDocument");
        {
            //set up the sequence on the external classification worker
            MetadataPolicy metadataPolicy = new MetadataPolicy();
            metadataPolicy.setFieldActions(new ArrayList<>());
            FieldAction fieldAction = new FieldAction();
            fieldAction.setAction(FieldAction.Action.ADD_FIELD_VALUE);
            fieldAction.setFieldName("EXTERNAL_TEST");
            fieldAction.setFieldValue("1");
            metadataPolicy.getFieldActions().add(fieldAction);
            Policy taggingPolicy = new Policy();
            taggingPolicy.name = BaseTestsHelper.getUniqueString("Tag the document");
            taggingPolicy.details = mapper.readTree(mapper.writeValueAsString(metadataPolicy));
            taggingPolicy.typeId = policyApi.retrievePolicyTypeByName("MetadataPolicy").id;
            taggingPolicy = policyApi.create(taggingPolicy);
            externalPolicyId = taggingPolicy.id;
            externalPolicyName = taggingPolicy.name;

            //Set up classification sequence
            ExistsCondition condition = PolicyTestHelper.createExistsCondition("afield", "exists_condition");

            DocumentCollection collection = PolicyTestHelper.createDocumentCollection(classificationApi, "external_classification_collection",
                    condition, taggingPolicy.id, "external_classification_collection");
            externalCollectionId = collection.id;
            externalConditionId = collection.condition.id;

            // Define the sequence that will exist on the external classification worker
            CollectionSequence externalWorkerSequence = new CollectionSequence();
            externalWorkerSequence.name = BaseTestsHelper.getUniqueString("External_classification_sequence");
            externalWorkerSequence.description = "Checks if field exists";
            externalWorkerSequence.collectionSequenceEntries = new ArrayList<>();

            CollectionSequenceEntry entry = PolicyTestHelper.createCollectionSequenceEntry(collection.id);

            externalWorkerSequence.collectionSequenceEntries.add(entry);
            externalWorkerSequence = classificationApi.create(externalWorkerSequence);
            externalSequenceId = externalWorkerSequence.id;
        }


        //Set up workflow sequence
        Policy genericQueuePolicy = PolicyTestHelper.createGenericQueuePolicy(policyApi, queueOutput);
        Policy classificationPolicy = PolicyTestHelper.createElasticSearchClassificationPolicy(policyApi, externalSequenceId);
        StringCondition stringCondition = PolicyTestHelper.createStringCondition("Title", StringOperatorType.CONTAINS, documentName);

        DocumentCollection sendToExternalCollection = PolicyTestHelper.createDocumentCollection(classificationApi, "send to external classification",
                stringCondition, classificationPolicy.id);

        BooleanCondition booleanCondition = new BooleanCondition();
        booleanCondition.operator = BooleanOperator.AND;

        DocumentCollection genericQueueCollection = PolicyTestHelper.createDocumentCollection(classificationApi, "send to generic queue",
                booleanCondition, genericQueuePolicy.id);

        CollectionSequence workflowSequence = new CollectionSequence();
        workflowSequence.name = BaseTestsHelper.getUniqueString("Workflow");
        workflowSequence.fullConditionEvaluation = true;
        CollectionSequenceEntry sequenceEntry = PolicyTestHelper.createCollectionSequenceEntry((short) 100, sendToExternalCollection.id);
        CollectionSequenceEntry sequenceEntry2 = PolicyTestHelper.createCollectionSequenceEntry((short) 200, genericQueueCollection.id);

        workflowSequence.collectionSequenceEntries = Arrays.asList(sequenceEntry, sequenceEntry2);
        workflowSequence = classificationApi.create(workflowSequence);

        //create task
        String taskId = UUID.randomUUID().toString();
        String taskClassifier = "PolicyWorker";
        Multimap<String, String> metadata = ArrayListMultimap.create();
        metadata.put("afield", String.valueOf(1L));
        metadata.put("Title", documentName);
        String collectionSequenceId = workflowSequence.id.toString();
        List<String> collectionSequenceIds = new ArrayList<>();
        collectionSequenceIds.add(collectionSequenceId);
        Document document = TestDocumentHelper.createDocument(metadata);

        TaskMessage classifyMessage = createPolicyWorkerTaskMessage(taskId, taskClassifier, document, collectionSequenceIds, true);

        //send task to input queue
        Channel inputChannel = BaseTestsHelper.rabbitConnection.createChannel();
        BlockingQueue<Event<QueuePublisher>> pubEvents = createRabbitPublisher(inputChannel);

        //set up consumer for results queue
        Channel resultsChannel = BaseTestsHelper.rabbitConnection.createChannel();
        BlockingQueue<Event<QueueConsumer>> conEvents = new LinkedBlockingQueue<>();
        ConsumerCreationResult consumerResult = createRabbitConsumer(resultsChannel, conEvents, queueOutput, 1, taskId);

        try {
            final TaskMessage resultWrapper = publishTaskAndAwaitThisMessagesResponse(pubEvents, consumerResult, classifyMessage);

            byte[] returnedTaskData = resultWrapper.getTaskData();
            //check that task has status of result success
            checkTaskMessageReturnedTaskStatus(resultWrapper, taskId, TaskStatus.NEW_TASK);
            //check task data
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            SharedDocument taskResponse = mapper.readValue(returnedTaskData, SharedDocument.class);
            Assert.assertNotNull(taskResponse);
            CaseInsensitiveMultimap<String> metaData = new CaseInsensitiveMultimap<>();
            taskResponse.getMetadata().forEach(e -> metaData.put(e.getKey(), e.getValue()));
            Optional<String> value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_MATCHED_COLLECTION).stream().findFirst();
            Assert.assertTrue("Document should have POLICY_MATCHED_COLLECTION in meta data", value.isPresent());
            Assert.assertEquals("Should have matched the classification collection", String.valueOf(externalCollectionId), value.get());

            value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_POLICYID_FIELD).stream().findFirst();
            Assert.assertTrue("Document should have POLICY_MATCHED_POLICYID in meta data", value.isPresent());
            Assert.assertEquals("Should have executed Tagging Policy", String.valueOf(externalPolicyId), value.get());

            value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_POLICYNAME_FIELD).stream().findFirst();
            Assert.assertTrue("Document should have POLICY_MATCHED_POLICYNAME in meta data", value.isPresent());
            Assert.assertEquals("Should have executed Tagging Policy", externalPolicyName, value.get());

            value = metaData.get(ClassificationWorkerConverterFields.getMatchedConditionField(externalCollectionId)).stream().findFirst();
            Assert.assertTrue("Document should have " + ClassificationWorkerConverterFields.getMatchedConditionField(externalConditionId)
                    + " in meta data", value.isPresent());
            Assert.assertEquals("Should have matched condition", String.valueOf(externalConditionId), value.get());
        } finally {
            closeRabbitConnections(resultsChannel, consumerResult);
        }
    }

    /**
     * Invokes a Workflow that sends a document to the elasticsearch based Classification Worker for Classification and checks the result.
     *
     * @throws IOException
     * @throws CodecException
     * @throws TimeoutException
     * @throws InterruptedException
     */
    @Test
    public void elasticSearchClassificationWorkflowIdTest() throws IOException, CodecException, TimeoutException, InterruptedException {
        String prefix = "elastic_workflow";
        ClassificationApi classificationApi = getClassificationApi();
        PolicyApi policyApi = genericApplicationContext.getBean(PolicyApi.class);
        WorkflowApi workflowApi = genericApplicationContext.getBean(WorkflowApi.class);

        String documentName = getUniqueString("MyDocument");

        Policy taggingPolicy = PolicyTestHelper.createMetadataPolicy(policyApi);

        //Set up external classification sequence
        ExistsCondition condition = PolicyTestHelper.createExistsCondition("afield", "exists_condition");

        DocumentCollection collection = PolicyTestHelper.createDocumentCollection(classificationApi, prefix + "_classification_collection",
                condition, taggingPolicy.id, prefix + "_classification_collection");

        CollectionSequence externalClassificationSequence = PolicyTestHelper.createCollectionSequenceWithOneEntry(classificationApi,
                collection.id, getUniqueString(prefix + "_classification_sequence"), "Checks if field exists");

        //Set up external workflow
        SequenceWorkflow externalWorkflow = PolicyTestHelper.createWorkflowWithOneEntry(workflowApi,
                externalClassificationSequence.id, getUniqueString(prefix + "_workflow"));

        //Set up workflow sequence
        Policy genericQueuePolicy = PolicyTestHelper.createGenericQueuePolicy(policyApi, queueOutput);
        Policy externalClassificationPolicy;
        externalClassificationPolicy = PolicyTestHelper.createElasticSearchClassificationWorkflowIdPolicy(policyApi, externalWorkflow.id);

        StringCondition stringCondition = PolicyTestHelper.createStringCondition("Title", StringOperatorType.CONTAINS, documentName);

        DocumentCollection externalCollection = PolicyTestHelper.createDocumentCollection(classificationApi, "send to external classification",
                stringCondition, externalClassificationPolicy.id);

        BooleanCondition booleanCondition = new BooleanCondition();
        booleanCondition.operator = BooleanOperator.AND;

        DocumentCollection genericQueueCollection = PolicyTestHelper.createDocumentCollection(classificationApi, "send to generic queue",
                booleanCondition, genericQueuePolicy.id);

        CollectionSequence workflowSequence = new CollectionSequence();
        workflowSequence.name = getUniqueString("Workflow");
        workflowSequence.fullConditionEvaluation = true;
        CollectionSequenceEntry sequenceEntry = PolicyTestHelper.createCollectionSequenceEntry((short) 100, externalCollection.id);
        CollectionSequenceEntry sequenceEntry2 = PolicyTestHelper.createCollectionSequenceEntry((short) 200, genericQueueCollection.id);

        workflowSequence.collectionSequenceEntries = Arrays.asList(sequenceEntry, sequenceEntry2);
        workflowSequence = classificationApi.create(workflowSequence);

        //create task
        String taskId = UUID.randomUUID().toString();
        String taskClassifier = "PolicyWorker";
        Multimap<String, String> metadata = ArrayListMultimap.create();
        metadata.put("afield", String.valueOf(1L));
        metadata.put("Title", documentName);
        String collectionSequenceId = workflowSequence.id.toString();
        List<String> collectionSequenceIds = new ArrayList<>();
        collectionSequenceIds.add(collectionSequenceId);
        Document document = TestDocumentHelper.createDocument(metadata);

        TaskMessage classifyMessage = createPolicyWorkerTaskMessage(taskId, taskClassifier, document, collectionSequenceIds, true);

        //send task to input queue
        Channel inputChannel = rabbitConnection.createChannel();
        BlockingQueue<Event<QueuePublisher>> pubEvents = createRabbitPublisher(inputChannel);

        //set up consumer for results queue
        Channel resultsChannel = rabbitConnection.createChannel();
        BlockingQueue<Event<QueueConsumer>> conEvents = new LinkedBlockingQueue<>();
        ConsumerCreationResult consumerResult = createRabbitConsumer(resultsChannel, conEvents, queueOutput, 1, taskId);

        try {
            final TaskMessage resultWrapper = publishTaskAndAwaitThisMessagesResponse(pubEvents, consumerResult, classifyMessage);
            final Codec codec = new JsonCodec();

            byte[] returnedTaskData = resultWrapper.getTaskData();
            //check that task has status of result success
            checkTaskMessageReturnedTaskStatus(resultWrapper, taskId, TaskStatus.NEW_TASK);
            //check task data
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            SharedDocument taskResponse = mapper.readValue(returnedTaskData, SharedDocument.class);
            Assert.assertNotNull(taskResponse);
            CaseInsensitiveMultimap<String> metaData = new CaseInsensitiveMultimap<>();
            taskResponse.getMetadata().forEach(e -> metaData.put(e.getKey(), e.getValue()));
            Optional<String> value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_MATCHED_COLLECTION).stream().findFirst();
            Assert.assertTrue("Document should have POLICY_MATCHED_COLLECTION in meta data", value.isPresent());
            Assert.assertEquals("Should have matched the classification collection", String.valueOf(collection.id), value.get());

            value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_POLICYID_FIELD).stream().findFirst();
            Assert.assertTrue("Document should have POLICY_MATCHED_POLICYID in meta data", value.isPresent());
            Assert.assertEquals("Should have executed Tagging Policy", String.valueOf(taggingPolicy.id), value.get());

            value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_POLICYNAME_FIELD).stream().findFirst();
            Assert.assertTrue("Document should have POLICY_MATCHED_POLICYNAME in meta data", value.isPresent());
            Assert.assertEquals("Should have executed Tagging Policy", taggingPolicy.name, value.get());

            value = metaData.get(ClassificationWorkerConverterFields.getMatchedConditionField(collection.id)).stream().findFirst();
            Assert.assertTrue("Document should have " + ClassificationWorkerConverterFields.getMatchedConditionField(condition.id) + " in meta data", value.isPresent());
            Assert.assertEquals("Should have matched condition", String.valueOf(condition.id), value.get());
        } finally {
            closeRabbitConnections(resultsChannel, consumerResult);
        }
    }

    @Test
    public void testBinaryHashWorkflow() throws IOException, TimeoutException, CodecException, InterruptedException, DataStoreException
    {

        String binaryhashPolicyTypeJSON = "{}";

        Long workflowId = createBinaryHashWorkflow(binaryhashPolicyTypeJSON);

        String queueOutput = "output";
        String documentName = UUID.randomUUID().toString();

        //  Create task
        String taskId = UUID.randomUUID().toString();
        String taskClassifier = "PolicyWorker";
        Multimap<String, String> metadata = ArrayListMultimap.create();
        metadata.put("afield", String.valueOf(1L));
        metadata.put("Title", documentName);

        try {
            InputStream exampleFileStream = getResourceAsStream("example-files/BinaryHashTest.txt");
            Assert.assertNotNull(exampleFileStream);

            String storageRef = storeDocument(exampleFileStream);
            metadata.put("storageReference", storageRef);
        } catch (Exception ex) {
            String test = "";
        }

        Document document = TestDocumentHelper.createDocument(metadata);

        TaskMessage classifyMessage = createPolicyWorkerTaskMessage(taskId, taskClassifier, document, workflowId, true);

        //  Send task to input queue
        Channel inputChannel = rabbitConnection.createChannel();
        BlockingQueue<Event<QueuePublisher>> pubEvents = createRabbitPublisher(inputChannel);

        //  Set up consumer for results queue
        Channel resultsChannel = rabbitConnection.createChannel();
        BlockingQueue<Event<QueueConsumer>> conEvents = new LinkedBlockingQueue<>();
        ConsumerCreationResult consumerResult = createRabbitConsumer(resultsChannel, conEvents, queueOutput, 1, taskId);

        try {
            final TaskMessage resultWrapper = publishTaskAndAwaitThisMessagesResponse(pubEvents, consumerResult, classifyMessage);
            final Codec codec = new JsonCodec();

            byte[] returnedTaskData = resultWrapper.getTaskData();

            //  Check that task has status of result success
            checkTaskMessageReturnedTaskStatus(resultWrapper, taskId, TaskStatus.NEW_TASK);

            //  Check task data
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            SharedDocument taskResponse = mapper.readValue(returnedTaskData, SharedDocument.class);

            assertExpectedBinaryHashMetadataAgainstTaskResponse(taskResponse, document.getReference());

        } finally {
            closeRabbitConnections(resultsChannel, consumerResult);
        }
    }


    private Long createBinaryHashWorkflow(String binaryHashPolicyTypeJson) throws IOException
    {
        PolicyApi policyApi = genericApplicationContext.getBean(PolicyApi.class);
        ClassificationApi classificationApi = getClassificationApi();

        //  Create BinaryHash policy using the supplied field names
        Policy binaryHashPolicy = new Policy();
        binaryHashPolicy.name = "binaryhash policy";
        binaryHashPolicy.typeId = policyApi.retrievePolicyTypeByName("BinaryHashPolicyType").id;
        binaryHashPolicy.details = mapper.readTree(binaryHashPolicyTypeJson);
        binaryHashPolicy = policyApi.create(binaryHashPolicy);

        DocumentCollection binaryhashCollection = PolicyTestHelper.createDocumentCollection(classificationApi, getUniqueString("binaryhash_collection"),
                null, binaryHashPolicy.id);

        CollectionSequence binaryhashCollectionSequence = new CollectionSequence();
        binaryhashCollectionSequence.name = getUniqueString("Send to binaryhash");
        binaryhashCollectionSequence.defaultCollectionId = binaryhashCollection.id;
        binaryhashCollectionSequence = classificationApi.create(binaryhashCollectionSequence);

        String queueOutput = "output";
        Policy genericQueuePolicy = PolicyTestHelper.createGenericQueuePolicy(policyApi, queueOutput);
        DocumentCollection genericQueueCollection = PolicyTestHelper.createDocumentCollection(classificationApi, "send to generic queue",
                null, genericQueuePolicy.id);

        CollectionSequence genericQueueSequence = new CollectionSequence();
        genericQueueSequence.name = getUniqueString("send to output");
        genericQueueSequence.defaultCollectionId = genericQueueCollection.id;
        genericQueueSequence = classificationApi.create(genericQueueSequence);

        SequenceWorkflow sequenceWorkflow = new SequenceWorkflow();
        sequenceWorkflow.name = getUniqueString("BinaryHash Workflow");
        sequenceWorkflow.sequenceWorkflowEntries = new ArrayList<>();

        SequenceWorkflowEntry sequenceWorkflowEntry = new SequenceWorkflowEntry();
        sequenceWorkflowEntry.collectionSequenceId = binaryhashCollectionSequence.id;
        sequenceWorkflow.sequenceWorkflowEntries.add(sequenceWorkflowEntry);

        sequenceWorkflowEntry = new SequenceWorkflowEntry();
        sequenceWorkflowEntry.collectionSequenceId = genericQueueSequence.id;
        sequenceWorkflow.sequenceWorkflowEntries.add(sequenceWorkflowEntry);

        sequenceWorkflow = getWorkflowApi().create(sequenceWorkflow);

        return sequenceWorkflow.id;
    }

    private void assertExpectedBinaryHashMetadataAgainstTaskResponse(SharedDocument taskResponse, String docReference)
    {
        Assert.assertNotNull(taskResponse);

        // Validate response returned with what is expected.
        Assert.assertEquals("Document references should match.", docReference, taskResponse.getReference());

        Assert.assertTrue("Document should have PROCESSING_worker_binaryhash_VERSION field added: ",
                taskResponse.getMetadata().stream().filter(e -> e.getKey().equalsIgnoreCase("PROCESSING_worker-binaryhash_VERSION")).findFirst().isPresent());

        Assert.assertTrue("Document should have BINARY_HASH_SHA1 field added: ",
                taskResponse.getMetadata().stream().filter(e -> e.getKey().equalsIgnoreCase("BINARY_HASH_SHA1")).findFirst().isPresent());
        Assert.assertTrue("BINARY_HASH_SHA1 field should match.", taskResponse.getMetadata().stream().filter(e -> e.getKey().equalsIgnoreCase("BINARY_HASH_SHA1")).findFirst().get().getValue().equals("8118f05565eb6a6217e352862c296556b51a0362"));
    }

    private String storeDocument(InputStream contentToStore) throws DataStoreException
    {
        DataStore store = getFsDataStore();
        return store.store(contentToStore, properties.getDataStorePartialReference());
    }

    private DataStore getFsDataStore() throws DataStoreException
    {
        FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(System.getenv("CAF_INTEGRATIONTESTS_FS_PATH"));
        return new FileSystemDataStore(conf);
    }

    @Test
    public void testLangDetectDocumentWorkerEnglishText() throws IOException, CodecException, TimeoutException, InterruptedException
    {
        String langDetectWorkerDefinition
                = "{"
                + "\"workerName\": \"worker-languagedetection\""
                + "}";
        Policy documentWorkerPolicy = createAdvancedPolicy("Document Policy Type", "DocumentWorkerHandler", langDetectWorkerDefinition);

        Policy genericQueuePolicy = PolicyTestHelper.createGenericQueuePolicy(getPolicyApi(), queueOutput);

        ClassificationApi classificationApi = getClassificationApi();
        ExistsCondition existsCondition = PolicyTestHelper.createExistsCondition("CONTENT", "content_condition");

        DocumentCollection documentWorkerCollection = PolicyTestHelper.createDocumentCollection(classificationApi, "send to document worker",
                existsCondition, documentWorkerPolicy.id);

        DocumentCollection genericQueueCollection = PolicyTestHelper.createDocumentCollection(classificationApi, "send to generic output queue",
                existsCondition, genericQueuePolicy.id);
        CollectionSequence testColSequence = new CollectionSequence();
        testColSequence.name = getUniqueString("testDocumentWorker");
        testColSequence.description = "Checks data from Document Worker";
        testColSequence.collectionSequenceEntries = new ArrayList<>();

        CollectionSequenceEntry entry = PolicyTestHelper.createCollectionSequenceEntry(documentWorkerCollection.id);
        testColSequence.collectionSequenceEntries.add(entry);
        entry = PolicyTestHelper.createCollectionSequenceEntry(genericQueueCollection.id);
        testColSequence.collectionSequenceEntries.add(entry);

        testColSequence = classificationApi.create(testColSequence);

        Multimap<String, String> metadata = ArrayListMultimap.create();
        InputStream englishFileContentStream = getResourceAsStream("example-files/English.txt");
        String englishContent = IOUtils.toString(englishFileContentStream);
        metadata.put("CONTENT", englishContent);
        Document document = TestDocumentHelper.createDocument(metadata);

        List<String> collectionSequenceIds = Arrays.asList(testColSequence.id.toString());

        String taskId = UUID.randomUUID().toString();
        String taskClassifier = "PolicyWorker";

        TaskMessage classifyMessage = createPolicyWorkerTaskMessage(taskId, taskClassifier, document, collectionSequenceIds, true);

        //send task to input queue
        Channel inputChannel = rabbitConnection.createChannel();
        BlockingQueue<Event<QueuePublisher>> pubEvents = createRabbitPublisher(inputChannel);

        //set up consumer for results queue
        Channel resultsChannel = rabbitConnection.createChannel();
        BlockingQueue<Event<QueueConsumer>> conEvents = new LinkedBlockingQueue<>();
        ConsumerCreationResult consumerResult = createRabbitConsumer(resultsChannel, conEvents, queueOutput, 1, taskId);

        try {
            final TaskMessage resultWrapper = publishTaskAndAwaitThisMessagesResponse(pubEvents, consumerResult, classifyMessage);
            final Codec codec = new JsonCodec();

            byte[] returnedTaskData = resultWrapper.getTaskData();

            //check task data
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            SharedDocument taskResponse = mapper.readValue(returnedTaskData, SharedDocument.class);
            Assert.assertNotNull(taskResponse);

            CaseInsensitiveMultimap<String> returnedMetadata = new CaseInsensitiveMultimap<>();
            taskResponse.getMetadata().forEach(e -> returnedMetadata.put(e.getKey(), e.getValue()));

            Collection<String> detectedLanguage1NameField = returnedMetadata.get("DetectedLanguage1_Name");
            Assert.assertTrue("Detected 1st language name field was output.", !detectedLanguage1NameField.isEmpty());
            String detectedLanguageField = detectedLanguage1NameField.iterator().next();
            Assert.assertEquals("Expecting 1st language name returned to be English.", "ENGLISH", detectedLanguageField);

            Collection<String> detectedLanguage2NameField = returnedMetadata.get("DetectedLanguage2_Name");
            Assert.assertTrue("Detected 2nd language name field was output.", !detectedLanguage2NameField.isEmpty());
            detectedLanguageField = detectedLanguage2NameField.iterator().next();
            Assert.assertEquals("Expecting 2nd language name returned to be Unknown.", "Unknown", detectedLanguageField);

            Collection<String> detectedLanguage3NameField = returnedMetadata.get("DetectedLanguage3_Name");
            Assert.assertTrue("Detected 3rd language name field was output.", !detectedLanguage3NameField.isEmpty());
            detectedLanguageField = detectedLanguage3NameField.iterator().next();
            Assert.assertEquals("Expecting 3rd language name returned to be Unknown.", "Unknown", detectedLanguageField);

            Collection<String> detectedLanguage1CodeField = returnedMetadata.get("DetectedLanguage1_Code");
            Assert.assertTrue("Detected 1st language field was output.", !detectedLanguage1CodeField.isEmpty());
            detectedLanguageField = detectedLanguage1CodeField.iterator().next();
            Assert.assertEquals("Expecting 1st language code returned to be en.", "en", detectedLanguageField);

            Collection<String> detectedLanguage2CodeField = returnedMetadata.get("DetectedLanguage2_Code");
            Assert.assertTrue("Detected 2nd language field was output.", !detectedLanguage2CodeField.isEmpty());
            detectedLanguageField = detectedLanguage2CodeField.iterator().next();
            Assert.assertEquals("Expecting 2nd language code returned to be un.", "un", detectedLanguageField);

            Collection<String> detectedLanguage3CodeField = returnedMetadata.get("DetectedLanguage3_Code");
            Assert.assertTrue("Detected 3rd language field was output.", !detectedLanguage3CodeField.isEmpty());
            detectedLanguageField = detectedLanguage3CodeField.iterator().next();
            Assert.assertEquals("Expecting 3rd language code returned to be un.", "un", detectedLanguageField);

            Collection<String> detectedLanguage1ConfidenceField = returnedMetadata.get("DetectedLanguage1_ConfidencePercentage");
            Assert.assertTrue("Detected 1st language confidence field was output.", !detectedLanguage1ConfidenceField.isEmpty());
            detectedLanguageField = detectedLanguage1ConfidenceField.iterator().next();
            Assert.assertEquals("Expecting 1st language confidence field returned to be 99.", "99", detectedLanguageField);

            Collection<String> detectedLanguage2ConfidenceField = returnedMetadata.get("DetectedLanguage2_ConfidencePercentage");
            Assert.assertTrue("Detected 2nd language confidence field was output.", !detectedLanguage2ConfidenceField.isEmpty());
            detectedLanguageField = detectedLanguage2ConfidenceField.iterator().next();
            Assert.assertEquals("Expecting 2nd language confidence field returned to be 0.", "0", detectedLanguageField);

            Collection<String> detectedLanguage3ConfidenceField = returnedMetadata.get("DetectedLanguage3_ConfidencePercentage");
            Assert.assertTrue("Detected 3rd language confidence field was output.", !detectedLanguage3ConfidenceField.isEmpty());
            detectedLanguageField = detectedLanguage3ConfidenceField.iterator().next();
            Assert.assertEquals("Expecting 3rd language confidence field returned to be 0.", "0", detectedLanguageField);

            Collection<String> detectedLanguageReliableResultField = returnedMetadata.get("DetectedLanguages_ReliableResult");
            Assert.assertTrue("Detected language reliable result field was output.", !detectedLanguageReliableResultField.isEmpty());
            detectedLanguageField = detectedLanguageReliableResultField.iterator().next();
            Assert.assertEquals("Expecting language reliable result field returned to be true.", "true", detectedLanguageField);
        } finally {
            closeRabbitConnections(resultsChannel, consumerResult);
        }
    }



    private Policy createAdvancedPolicy(String policyName, String policyTypeName, String policyDefinition) throws IOException
    {

        Policy textExtractPolicy = new Policy();
        textExtractPolicy.name = policyName;

        PolicyType policyType = getPolicyApi().retrievePolicyTypeByName(policyTypeName);
        Assert.assertNotNull("Policytype must exist:", policyType);

        textExtractPolicy.typeId = policyType.id;
        textExtractPolicy.details = mapper.readTree(policyDefinition);
        textExtractPolicy = getPolicyApi().create(textExtractPolicy);

        Assert.assertNotEquals(0L, (long) (textExtractPolicy.id));

        return textExtractPolicy;
    }
}
