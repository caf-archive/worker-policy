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
package com.github.cafdataprocessing.worker.policy.testing.tests;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.worker.policy.testing.shared.BaseTestsHelper;
import com.github.cafdataprocessing.worker.policy.testing.shared.LogTestName;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.corepolicy.common.*;
import com.github.cafdataprocessing.corepolicy.common.dto.*;
import com.github.cafdataprocessing.corepolicy.common.dto.DocumentCollection;
import com.github.cafdataprocessing.corepolicy.common.dto.conditions.*;
import com.github.cafdataprocessing.corepolicy.common.dto.conditions.Condition;
import com.github.cafdataprocessing.corepolicy.domainModels.FieldAction;
import com.github.cafdataprocessing.corepolicy.policy.TagPolicy.TagPolicy;
import com.github.cafdataprocessing.corepolicy.repositories.RepositoryConnectionProvider;
import com.github.cafdataprocessing.corepolicy.repositories.RepositoryType;
import com.github.cafdataprocessing.worker.policy.shared.*;
import com.github.cafdataprocessing.worker.policy.testing.shared.TestDocumentHelper;
import com.hpe.caf.api.CipherException;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.util.rabbitmq.*;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.datastore.fs.FileSystemDataStore;
import com.hpe.caf.worker.datastore.fs.FileSystemDataStoreConfiguration;
import com.rabbitmq.client.Channel;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General integration tests for Policy Worker
 */
@RunWith(MockitoJUnitRunner.class)
public class PolicyWorkerIT extends BaseTestsHelper
{

    private static final Logger LOG = LoggerFactory.getLogger(PolicyWorkerIT.class);

    private CollectionSequence collectionSequence;
    private DocumentCollection collection;
    private LinkedList<Long> policyIds;

    String examplePolicyTypeJsonWithFieldActions = "{\n"
        + "    \"title\": \"Example Custom Policy Type\", \n"
        + "    \"properties\": {\n"
        + "        \"fieldActions\": {\n"
        + "            \"type\": \"array\",\n"
        + "            \"items\": {\n"
        + "                \"title\": \"Field Action\",\n"
        + "                \"type\": \"object\",\n"
        + "                \"properties\": {\n"
        + "                    \"name\": {\n"
        + "                        \"description\": \"The name of the field to perform the action on.\",\n"
        + "                        \"type\": \"string\",\n"
        + "                        \"minLength\": 1\n"
        + "                    },\n"
        + "                    \"action\": {\n"
        + "                        \"description\": \"The type of action to perform on the field.\",\n"
        + "                        \"type\": \"string\",\n"
        + "                        \"enum\": [\n"
        + "                            \"ADD_FIELD_VALUE\"\n"
        + "                        ]\n"
        + "                    },\n"
        + "                    \"value\": {\n"
        + "                        \"description\": \"The value to use for the field action.\",\n"
        + "                        \"type\": \"string\"\n"
        + "                    }\n"
        + "                },\n"
        + "                \"required\": [\"name\", \"action\"]\n"
        + "            }\n"
        + "        }\n"
        + "    }\n"
        + "}";

    @Before
    public void SetupDefaultPoliciesAndCollections() throws QueueException, IOException, TimeoutException
    {
        policyIds = SetupPolicies();
        collection = SetupCollection(policyIds);
        collectionSequence = SetupCollectionSequence(collection);
    }

    // log out the test name before and after each test.
    @Rule
    public LogTestName logTestName = new LogTestName();
    
    @Test
    public void ClassifyInvalidSequenceId() throws CodecException, IOException, TimeoutException, InterruptedException
    {
        final String taskId = UUID.randomUUID().toString();
        String taskClassifier = PolicyWorkerConstants.WORKER_NAME;
        String collectionSequenceId = "999999999"; //generating a sequence id that we aren't expecting to exist
        Boolean executePolicy = false;
        List<String> collectionSequenceIds = new ArrayList<>();
        collectionSequenceIds.add(collectionSequenceId);
        Multimap<String, String> metadata = ArrayListMultimap.create();
        metadata.put("afield", String.valueOf(1L));
        Document document = TestDocumentHelper.createDocument(metadata);

        TaskMessage classifyMessage = createPolicyWorkerTaskMessage(taskId, taskClassifier, document,
                                                                                   collectionSequenceIds, executePolicy);

        Channel inputChannel = rabbitConnection.createChannel();
        Channel resultsChannel = rabbitConnection.createChannel();

        //send task to input queue
        BlockingQueue<Event<QueuePublisher>> pubEvents = createRabbitPublisher(inputChannel);

        //set up results queue consumer
        BlockingQueue<Event<QueueConsumer>> conEvents = new LinkedBlockingQueue<>();
        ConsumerCreationResult consumerResult = createRabbitConsumer(resultsChannel, conEvents, RESULT_QUEUENAME, 1, taskId);

        try {
            final TaskMessage resultWrapper = publishTaskAndAwaitThisMessagesResponse(pubEvents, consumerResult, classifyMessage);

            byte[] returnedTaskData = resultWrapper.getTaskData();
            checkTaskMessageReturnedTaskStatus(resultWrapper, taskId, TaskStatus.INVALID_TASK);
        } finally {
            closeRabbitConnections(resultsChannel, consumerResult);
        }
    }

    @Test
    public void ExecuteInvalidPolicyIdTest() throws CodecException, IOException, TimeoutException, InterruptedException
    {
        //create task
        final String taskId = UUID.randomUUID().toString();
        String taskClassifier = PolicyWorkerConstants.WORKER_NAME;
        Multimap<String, String> metadata = ArrayListMultimap.create();
        metadata.put("afield", String.valueOf(1L));
        Collection<Long> invalidPolicyIds = new ArrayList<>();
        invalidPolicyIds.add(99999999L);
        String collectionSequenceId = collectionSequence.id.toString();
        List<String> collectionSequenceIds = new ArrayList<>();
        collectionSequenceIds.add(collectionSequenceId);
        Document document = TestDocumentHelper.createDocument(metadata);

        TaskMessage executeMessage = createPolicyWorkerTaskMessage(taskId, taskClassifier, document, null, collectionSequenceIds, invalidPolicyIds, false, true );

        //send task to input queue
        Channel inputChannel = rabbitConnection.createChannel();
        BlockingQueue<Event<QueuePublisher>> pubEvents = createRabbitPublisher(inputChannel);

        //set up consumer for results queue
        Channel resultsChannel = rabbitConnection.createChannel();
        BlockingQueue<Event<QueueConsumer>> conEvents = new LinkedBlockingQueue<>();
        ConsumerCreationResult consumerResult = createRabbitConsumer(resultsChannel, conEvents, RESULT_QUEUENAME, 1, taskId);

        try {
            final TaskMessage resultWrapper = publishTaskAndAwaitThisMessagesResponse(pubEvents, consumerResult, executeMessage);

            byte[] returnedTaskData = resultWrapper.getTaskData();
            //check that task has status of exception
            checkTaskMessageReturnedException(resultWrapper, taskId);
        } finally {
            closeRabbitConnections(resultsChannel, consumerResult);
        }
    }

    @Test
    public void ExecuteHandlerNotRegisteredTest() throws CodecException, IOException, TimeoutException, InterruptedException
    {
        //create task
        final String taskId = UUID.randomUUID().toString();
        String taskClassifier = PolicyWorkerConstants.WORKER_NAME;
        Multimap<String, String> metadata = ArrayListMultimap.create();
        metadata.put("afield", String.valueOf(1L));

        Collection<Long> noHandlerPolicyIds = SetupPoliciesWithNoHandlersAvailable();
        collectionSequence = SetupCollectionSequenceWithPolicies(noHandlerPolicyIds);

        String collectionSequenceId = collectionSequence.id.toString();
        List<String> collectionSequenceIds = new ArrayList<>();
        collectionSequenceIds.add(collectionSequenceId);

        Document document = TestDocumentHelper.createDocument(metadata);

        TaskMessage executeMessage = createPolicyWorkerTaskMessage(taskId, taskClassifier, document, null, collectionSequenceIds, noHandlerPolicyIds, false, true );

        //send task to input queue
        Channel inputChannel = rabbitConnection.createChannel();
        BlockingQueue<Event<QueuePublisher>> pubEvents = createRabbitPublisher(inputChannel);

        //set up consumer for results queue
        Channel resultsChannel = rabbitConnection.createChannel();
        BlockingQueue<Event<QueueConsumer>> conEvents = new LinkedBlockingQueue<>();
        ConsumerCreationResult consumerResult = createRabbitConsumer(resultsChannel, conEvents, RESULT_QUEUENAME, 1, taskId);

        try {
            final TaskMessage resultWrapper = publishTaskAndAwaitThisMessagesResponse(pubEvents, consumerResult, executeMessage);

            byte[] returnedTaskData = resultWrapper.getTaskData();
            //should report result success as no handler is not a failure condition and is just ignored
            checkTaskMessageReturned(resultWrapper, taskId);
        } finally {
            closeRabbitConnections(resultsChannel, consumerResult);
        }
    }


    /*
        Tests classification of a single document by PolicyWorker runs as expected. Puts task onto Input queue of PolicyWorker and reads result of task from Results queue.
     */
    @Test
    public void ClassifyDocumentTest() throws WorkerException, IOException, QueueException, TimeoutException, InterruptedException, CodecException
    {
        final String taskId = UUID.randomUUID().toString();
        String taskClassifier = PolicyWorkerConstants.WORKER_NAME;
        String collectionSequenceId = collectionSequence.id.toString();
        Boolean executePolicy = false;
        List<String> collectionSequenceIds = new ArrayList<>();
        collectionSequenceIds.add(collectionSequenceId);
        Multimap<String, String> metadata = ArrayListMultimap.create();
        metadata.put("afield", String.valueOf(1L));
        Document document = TestDocumentHelper.createDocument(metadata);
        document.setReference(null);

        TaskMessage classifyMessage = createPolicyWorkerTaskMessage(taskId, taskClassifier, document,
                                                                                   collectionSequenceIds, executePolicy);

        Channel inputChannel = rabbitConnection.createChannel();
        Channel resultsChannel = rabbitConnection.createChannel();

        //send task to input queue
        BlockingQueue<Event<QueuePublisher>> pubEvents = createRabbitPublisher(inputChannel);

        //set up results queue consumer
        BlockingQueue<Event<QueueConsumer>> conEvents = new LinkedBlockingQueue<>();
        ConsumerCreationResult consumerResult = createRabbitConsumer(resultsChannel, conEvents, RESULT_QUEUENAME, 1, taskId);

        try {
            final TaskMessage resultWrapper = publishTaskAndAwaitThisMessagesResponse(pubEvents, consumerResult, classifyMessage);

            byte[] returnedTaskData = resultWrapper.getTaskData();
            checkTaskMessageReturned(resultWrapper, taskId);
            //check task data
            Codec codec = new JsonCodec();
            
            TaskResponse taskResponse = codec.deserialise(returnedTaskData, TaskResponse.class);
            Assert.assertNotNull(taskResponse);
            Collection<com.github.cafdataprocessing.worker.policy.shared.ClassifyDocumentResult> classifyResultCollection = taskResponse.getClassifiedDocuments();
            Assert.assertNotNull(classifyResultCollection);
            //should only be one result returned
            Assert.assertEquals(1, classifyResultCollection.size());
            //get this result and check the properties on it are as expected
            com.github.cafdataprocessing.worker.policy.shared.ClassifyDocumentResult classifyResult = classifyResultCollection.iterator().next();
            //check no incomplete collections returned
            Collection<Long> incomplete = classifyResult.getIncompleteCollections();
            Assert.assertEquals(0, incomplete.size());

            //check reference is that of document we sent
            String resultReference = classifyResult.getReference();
            Assert.assertEquals(document.getReference(), resultReference);

            checkClassifyResult(classifyResult, this.policyIds, this.collection);

        } finally {
            closeRabbitConnections(resultsChannel, consumerResult);
        }
    }

    /*
        Tests classification of a single document by PolicyWorker using a WorkflowId runs as expected. Puts task onto Input queue of PolicyWorker and reads result of task from Results queue.
     */
    @Test
    public void ClassifyDocumentUsingWorkflowIdTest() throws WorkerException, IOException, QueueException, TimeoutException, InterruptedException, CodecException
    {
        final String taskId = UUID.randomUUID().toString();
        String taskClassifier = PolicyWorkerConstants.WORKER_NAME;
        Boolean executePolicy = false;
        Multimap<String, String> metadata = ArrayListMultimap.create();
        metadata.put("afield", String.valueOf(1L));
        Document document = TestDocumentHelper.createDocument(metadata);
        document.setReference(null);

        //create workflow
        WorkflowApi workflowApi = getWorkflowApi();
        SequenceWorkflow sequenceWorkflow = new SequenceWorkflow();
        sequenceWorkflow.name = "ClassifyDocumentUsingWorkflowIdTest_" + UUID.randomUUID();
        SequenceWorkflowEntry workflowEntry = new SequenceWorkflowEntry();
        workflowEntry.collectionSequenceId = this.collectionSequence.id;
        List<SequenceWorkflowEntry> workflowEntries = new ArrayList<>();
        workflowEntries.add(workflowEntry);
        sequenceWorkflow.sequenceWorkflowEntries = workflowEntries;
        sequenceWorkflow = workflowApi.create(sequenceWorkflow);

        TaskMessage classifyMessage = createPolicyWorkerTaskMessage(taskId, taskClassifier, document,
                                                                                   sequenceWorkflow.id, executePolicy);

        Channel inputChannel = rabbitConnection.createChannel();
        Channel resultsChannel = rabbitConnection.createChannel();

        //send task to input queue
        BlockingQueue<Event<QueuePublisher>> pubEvents = createRabbitPublisher(inputChannel);

        //set up results queue consumer
        BlockingQueue<Event<QueueConsumer>> conEvents = new LinkedBlockingQueue<>();
        ConsumerCreationResult consumerResult = createRabbitConsumer(resultsChannel, conEvents, RESULT_QUEUENAME, 1, taskId);

        try {
            final TaskMessage resultWrapper = publishTaskAndAwaitThisMessagesResponse(pubEvents, consumerResult, classifyMessage);

            byte[] returnedTaskData = resultWrapper.getTaskData();
            checkTaskMessageReturned(resultWrapper, taskId);
            //check task data
            Codec codec = new JsonCodec();
            TaskResponse taskResponse = codec.deserialise(returnedTaskData, TaskResponse.class);
            Assert.assertNotNull(taskResponse);
            Collection<com.github.cafdataprocessing.worker.policy.shared.ClassifyDocumentResult> classifyResultCollection = taskResponse.getClassifiedDocuments();
            Assert.assertNotNull(classifyResultCollection);
            //should only be one result returned
            Assert.assertEquals(1, classifyResultCollection.size());
            //get this result and check the properties on it are as expected
            com.github.cafdataprocessing.worker.policy.shared.ClassifyDocumentResult classifyResult = classifyResultCollection.iterator().next();
            //check no incomplete collections returned
            Collection<Long> incomplete = classifyResult.getIncompleteCollections();
            Assert.assertEquals(0, incomplete.size());

            //check reference is that of document we sent
            String resultReference = classifyResult.getReference();
            Assert.assertEquals(document.getReference(), resultReference);

            checkClassifyResult(classifyResult, this.policyIds, this.collection);

        } finally {
            closeRabbitConnections(resultsChannel, consumerResult);
        }
    }

    @Test
    public void SendResultFailureToWorkerTest() throws CodecException, IOException, InterruptedException, TimeoutException
    {
        final String taskId = UUID.randomUUID().toString();
        String taskClassifier = PolicyWorkerConstants.WORKER_NAME;
        String collectionSequenceId = Long.toString(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE));
        Boolean executePolicy = false;
        List<String> collectionSequenceIds = new ArrayList<>();
        collectionSequenceIds.add(collectionSequenceId);
        Multimap<String, String> metadata = ArrayListMultimap.create();
        metadata.put("afield", String.valueOf(1L));
        Document document = TestDocumentHelper.createDocument(metadata);
        document.setReference(null);

        TaskMessage classifyMessage = createPolicyWorkerTaskMessage(taskId, taskClassifier, document,
                                                                                          collectionSequenceIds, executePolicy);

        Channel inputChannel = rabbitConnection.createChannel();
        Channel resultsChannel = rabbitConnection.createChannel();

        //send task to input queue
        BlockingQueue<Event<QueuePublisher>> pubEvents = createRabbitPublisher(inputChannel);

        //set up results queue consumer
        BlockingQueue<Event<QueueConsumer>> conEvents = new LinkedBlockingQueue<>();
        ConsumerCreationResult consumerResult = createRabbitConsumer(resultsChannel, conEvents, RESULT_QUEUENAME, 1, taskId);

        try {
            final TaskMessage resultWrapper = publishTaskAndAwaitThisMessagesResponse(pubEvents, consumerResult, classifyMessage);
            
            //expecting an invalid collection sequence Id to no longer return TaskFailure -> this was due to a bug, it actually
            // should be returning INVALID_TASK as of workerframework 1.5.0+ 
            checkTaskMessageReturnedTaskStatus(resultWrapper, taskId, TaskStatus.INVALID_TASK);

            // TASK_FAILURE task data is actually a TaskData object
            // INVALID_TASK actually returns a string with the exception present instead.
            final String invalidTaskInfo = new String(resultWrapper.getTaskData(), StandardCharsets.UTF_8);
            
            assertTrue("InvalidTask task data should contain error with 'invalid field value' failure information.", invalidTaskInfo.contains("Invalid field value") );
        } finally {
            closeRabbitConnections(resultsChannel, consumerResult);
        }
    }

    /*
        Tests classification of a single document by PolicyWorker runs as expected, storing document content in Filesystem storage. Puts task onto Input queue of PolicyWorker and reads result of task from Results queue.
     */
    @Test
    public void ClassifyDocumentWithStoredContentFsStoreTest() throws WorkerException, IOException, QueueException, TimeoutException, InterruptedException, CodecException, ConfigurationException, CipherException, DataStoreException
    {
        skipIfStoreFSDisabled("Tests running without File System Storage mode. This test requires File System Storage mode so this test must be skipped", true);
        classifyDocumentWithStoredContent(getFsDataStore());
    }

    private DataStore getFsDataStore() throws DataStoreException
    {
        FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(System.getenv("CAF_INTEGRATIONTESTS_FS_PATH"));
        return new FileSystemDataStore(conf);
    }

    private void classifyDocumentWithStoredContent(DataStore store) throws DataStoreException, CodecException, IOException, InterruptedException, TimeoutException
    {
        // check for field added by policy handler.
        StringCondition condition = new StringCondition();
        condition.name = "Check for presence of field TestThisFieldAppliedByMetadataPolicy";
        condition.field = "content";
        condition.operator = StringOperatorType.CONTAINS;
        condition.value = "true";

        ClassificationApi classificationApi = genericApplicationContext.getBean(ClassificationApi.class);

        // second condition, now checks that value is added
        DocumentCollection collection2 = getDocumentCollection(classificationApi, condition, new ArrayList<>());
        CollectionSequence collectionSequence2 = SetupCollectionSequence(classificationApi, collection2.id, true);

        final String taskId = UUID.randomUUID().toString();
        String taskClassifier = PolicyWorkerConstants.WORKER_NAME;
        String collectionSequenceId = collectionSequence2.id.toString();
        Boolean executePolicy = false;
        List<String> collectionSequenceIds = new ArrayList<>();
        collectionSequenceIds.add(collectionSequenceId);
        Multimap<String, String> metadata = ArrayListMultimap.create();
        metadata.put("content", String.valueOf(1L));

        String storedDataLocation = store.store(new ByteArrayInputStream(condition.value.getBytes()),
                properties.getDataStorePartialReference());

        Document document = TestDocumentHelper.createDocument(metadata);
        ArrayListMultimap<String, ReferencedData> referenceMultimap = ArrayListMultimap.create();
        referenceMultimap.put(condition.field, ReferencedData.getReferencedData(storedDataLocation));
        document.setMetadataReferences(referenceMultimap);

        TaskMessage classifyMessage = createPolicyWorkerTaskMessage(taskId, taskClassifier, document,
                                                                                   collectionSequenceIds, executePolicy);

        Channel inputChannel = rabbitConnection.createChannel();
        Channel resultsChannel = rabbitConnection.createChannel();

        //send task to input queue
        BlockingQueue<Event<QueuePublisher>> pubEvents = createRabbitPublisher(inputChannel);

        //set up results queue consumer
        BlockingQueue<Event<QueueConsumer>> conEvents = new LinkedBlockingQueue<>();
        ConsumerCreationResult consumerResult = createRabbitConsumer(resultsChannel, conEvents, RESULT_QUEUENAME, 1, taskId);

        try {
            final TaskMessage resultWrapper = publishTaskAndAwaitThisMessagesResponse(pubEvents, consumerResult, classifyMessage);

            byte[] returnedTaskData = resultWrapper.getTaskData();
            
            Codec codec = new JsonCodec();
            checkTaskMessageReturned(resultWrapper, taskId);
            //check task data
            TaskResponse taskResponse = codec.deserialise(returnedTaskData, TaskResponse.class);
            Assert.assertNotNull(taskResponse);
            Collection<com.github.cafdataprocessing.worker.policy.shared.ClassifyDocumentResult> classifyResultCollection = taskResponse.getClassifiedDocuments();
            Assert.assertNotNull(classifyResultCollection);
            //should only be one result returned
            Assert.assertEquals(1, classifyResultCollection.size());
            //get this result and check the properties on it are as expected
            com.github.cafdataprocessing.worker.policy.shared.ClassifyDocumentResult classifyResult = classifyResultCollection.iterator().next();
            //check no incomplete collections returned
            Collection<Long> incomplete = classifyResult.getIncompleteCollections();
            Assert.assertEquals(0, incomplete.size());

            //check reference is that of document we sent
            String resultReference = classifyResult.getReference();
            Assert.assertEquals(document.getReference(), resultReference);

            checkClassifyResult(classifyResult, new ArrayList<>(), collection2);

        } finally {
            closeRabbitConnections(resultsChannel, consumerResult);
        }
    }

    private void skipIfStoreFSDisabled(String message, boolean checkEnabled)
    {
        if (System.getProperty("store-fs-enabled") != null) {
            Boolean checkIfFSEnabled = Boolean.parseBoolean(System.getProperty("store-fs-enabled"));

            Assume.assumeTrue(message, checkEnabled ? checkIfFSEnabled : !checkIfFSEnabled);
        } else {
            Assert.fail("store-fs-enabled is not set");
        }
    }


    /*
        Tests classification and execution of a single document by PolicyWorker runs as expected. Puts task onto Input queue of PolicyWorker and reads result of task from Results queue.
     */
    @Test
    public void ClassifyAndExecuteDocumentTest() throws WorkerException, IOException, QueueException, TimeoutException, InterruptedException, CodecException
    {
        final String taskId = UUID.randomUUID().toString();
        String taskClassifier = PolicyWorkerConstants.WORKER_NAME;
        String collectionSequenceId = collectionSequence.id.toString();
        Boolean executePolicy = true;
        List<String> collectionSequenceIds = new ArrayList<>();
        collectionSequenceIds.add(collectionSequenceId);
        Multimap<String, String> metadata = ArrayListMultimap.create();
        metadata.put("afield", String.valueOf(1L));
        Document document = TestDocumentHelper.createDocument(metadata);

        TaskMessage classifyMessage = createPolicyWorkerTaskMessage(taskId, taskClassifier, document,
                                                                                   collectionSequenceIds, executePolicy);

        Channel inputChannel = rabbitConnection.createChannel();
        Channel resultsChannel = rabbitConnection.createChannel();

        //send task to input queue
        BlockingQueue<Event<QueuePublisher>> pubEvents = createRabbitPublisher(inputChannel);

        //set up results queue consumer
        BlockingQueue<Event<QueueConsumer>> conEvents = new LinkedBlockingQueue<>();
        ConsumerCreationResult consumerResult = createRabbitConsumer(resultsChannel, conEvents, RESULT_QUEUENAME, 1, taskId);

        try {
            final TaskMessage resultWrapper = publishTaskAndAwaitThisMessagesResponse(pubEvents, consumerResult, classifyMessage);
            
            byte[] returnedTaskData = resultWrapper.getTaskData();
            checkTaskMessageReturned(resultWrapper, taskId);
            //check task data
            Codec codec = new JsonCodec();
            TaskResponse taskResponse = codec.deserialise(returnedTaskData, TaskResponse.class);
            Assert.assertNotNull(taskResponse);
            Collection<com.github.cafdataprocessing.worker.policy.shared.ClassifyDocumentResult> classifyResultCollection = taskResponse.getClassifiedDocuments();
            Assert.assertNotNull(classifyResultCollection);
            //should only be one result returned
            Assert.assertEquals(1, classifyResultCollection.size());
            //get this result and check the properties on it are as expected
            com.github.cafdataprocessing.worker.policy.shared.ClassifyDocumentResult classifyResult = classifyResultCollection.iterator().next();
            //check no incomplete collections returned
            Collection<Long> incomplete = classifyResult.getIncompleteCollections();
            Assert.assertEquals(0, incomplete.size());

            //check reference is that of document we sent
            String resultReference = classifyResult.getReference();
            Assert.assertEquals(document.getReference(), resultReference);

            checkClassifyResult(classifyResult, this.policyIds, this.collection);
        } finally {
            closeRabbitConnections(resultsChannel, consumerResult);
        }
    }

    @Test
    public void ExecutePolicyOnDocumentTest() throws CodecException, IOException, TimeoutException, InterruptedException
    {
        //create task
        final String taskId = UUID.randomUUID().toString();
        String taskClassifier = PolicyWorkerConstants.WORKER_NAME;
        Multimap<String, String> metadata = ArrayListMultimap.create();
        metadata.put("afield", String.valueOf(1L));
        String collectionSequenceId = collectionSequence.id.toString();
        List<String> collectionSequenceIds = new ArrayList<>();
        collectionSequenceIds.add(collectionSequenceId);
        Document document = TestDocumentHelper.createDocument(metadata);

        TaskMessage executeMessage = createPolicyWorkerTaskMessage(taskId, taskClassifier, document, null, collectionSequenceIds, policyIds, false, true );

        //send task to input queue
        Channel inputChannel = rabbitConnection.createChannel();
        BlockingQueue<Event<QueuePublisher>> pubEvents = createRabbitPublisher(inputChannel);

        //set up consumer for results queue
        Channel resultsChannel = rabbitConnection.createChannel();
        BlockingQueue<Event<QueueConsumer>> conEvents = new LinkedBlockingQueue<>();
        ConsumerCreationResult consumerResult = createRabbitConsumer(resultsChannel, conEvents, RESULT_QUEUENAME, 1, taskId);

        try {
            final TaskMessage resultWrapper = publishTaskAndAwaitThisMessagesResponse(pubEvents, consumerResult, executeMessage);
            
            Codec codec = new JsonCodec();
            byte[] returnedTaskData = resultWrapper.getTaskData();
            //check that task has status of result success
            checkTaskMessageReturned(resultWrapper, taskId);
            //check task data
            TaskResponse taskResponse = codec.deserialise(returnedTaskData, TaskResponse.class);
            Assert.assertNotNull(taskResponse);
            Collection<com.github.cafdataprocessing.worker.policy.shared.ClassifyDocumentResult> classifyResultCollection = taskResponse.getClassifiedDocuments();
            Assert.assertNotNull(classifyResultCollection);
            //should be no classify results returned
            Assert.assertEquals(0, classifyResultCollection.size());

        } finally {
            closeRabbitConnections(resultsChannel, consumerResult);
        }
    }

    /**
     * *
     * Code should create a set of conditions, in separate sequences, which match on first entry and put execute policy, this will just
     * return normal match. Next element in the chain should contain conditions which check that the first one has executed and added a
     * field, but when its policies executes it performs a check internal to the worker, for the temporary fields. And adds a property -
     * CheckTempFieldsWorkerFoundMetadata_ true/false depending on whether they are present. We finally check the state of this in the
     * last collection sequence.
     */
    @Test
    public void ClassifyAndExecuteDocumentAndCheckNoTemporaryFields() throws WorkerException, IOException, QueueException, TimeoutException, InterruptedException, CodecException
    {
        Assume.assumeTrue("ClassifyAndExecuteDocumentAndCheckNoTemporaryFields not ran as testing handler projects are not enabled.",
                properties.getRunHandlerTests());

        // Setup our list of policytypes / policies for each handler we require!
        PolicyApi policyApi = genericApplicationContext.getBean(PolicyApi.class);

        Policy customIndexPolicy = createCustomPolicyType(policyApi,
                                                          getUniqueString("TestIndexingPolicy"),
                                                          "TestHandlers-IndexPolicy",
                                                          "CustomPolicy for testing indexing policy and metadata removal.",
                                                          examplePolicyTypeJsonWithFieldActions,
                                                          "IndexPolicy test policy",
                                                          "TestThisFieldAppliedByIndexPolicy",
                                                          "true",
                                                          200);

        Assert.assertNotNull("We need index policy created", customIndexPolicy);

        Policy customCheckTempFieldsTagPolicy = createCustomPolicyType(policyApi,
                                                                       getUniqueString("TestCheckForTempContextInfoPolicy"),
                                                                       "TestHandlers-CheckForTempContextInfo",
                                                                       "CustomPolicy for checking if our temporary metadata for context info is passed to worker policy handlers.",
                                                                       examplePolicyTypeJsonWithFieldActions,
                                                                       "CheckForTempContextInfo test policy",
                                                                       "TestThisFieldAddedToShowItRan",
                                                                       "true",
                                                                       300);

        ClassificationApi classificationApi = genericApplicationContext.getBean(ClassificationApi.class);

        // simple check for our fixed field
        NumberCondition numberCondition = new NumberCondition();
        numberCondition.name = "afield condition value 1";
        numberCondition.field = "afield";
        numberCondition.operator = NumberOperatorType.EQ;
        numberCondition.value = 1L;

        // first sequence checks for afield and runs indexing policy to our our expected value.
        DocumentCollection collection1 = getDocumentCollection(classificationApi, numberCondition, Arrays.asList(customIndexPolicy.id));
        CollectionSequence collectionSequence1 = SetupCollectionSequence(classificationApi, collection1.id, true);

        // check for field added by policy handler.
        StringCondition condition = new StringCondition();
        condition.name = "Check for presence of field TestThisFieldAppliedByIndexPolicy";
        condition.field = "TestThisFieldAppliedByIndexPolicy";
        condition.operator = StringOperatorType.IS;
        condition.value = "true";

        // second condition, now checks that value is added
        DocumentCollection collection2 = getDocumentCollection(classificationApi, condition, Arrays.asList(customCheckTempFieldsTagPolicy.id));
        CollectionSequence collectionSequence2 = SetupCollectionSequence(classificationApi, collection2.id, true);

        // boolean condition check for policy handler field, and NOT any of our policy temp fields.
        BooleanCondition booleanConditionRoot = new BooleanCondition();
        booleanConditionRoot.name = "Check for TestThisFieldAppliedByIndexPolicy and No Temporary fields";
        booleanConditionRoot.operator = BooleanOperator.AND;

        // In order to check if any temp fields were supplied to the workers, we have a test worker
        // which runs as part of second collection.  It will have marked a property - check that it is FALSE.
        StringCondition findTempFieldsCondition = new StringCondition();
        findTempFieldsCondition.name = "FindTempFieldsMarker";
        findTempFieldsCondition.field = "CheckTempFieldsWorkerFoundMetadata_" + collectionSequence2.id;
        findTempFieldsCondition.value = Boolean.toString(false);
        findTempFieldsCondition.operator = StringOperatorType.IS;

        // add all these together.
        booleanConditionRoot.children = Arrays.asList(numberCondition, condition, findTempFieldsCondition);

        // finally a third sequence that checks for field AND that we also have no temporary fields!!!
        DocumentCollection collection3 = getDocumentCollection(classificationApi, booleanConditionRoot, Arrays.asList(customIndexPolicy.id));
        CollectionSequence collectionSequence3 = SetupCollectionSequence(classificationApi, collection3.id, true);


        /*
         * Finished Conditions setup -
         * Finally execute the test and check for each of our matched collections in the result!
         */
        final String taskId = UUID.randomUUID().toString();
        String taskClassifier = PolicyWorkerConstants.WORKER_NAME;

        Boolean executePolicy = true;
        List<String> collectionSequenceIds = new ArrayList<>();
        collectionSequenceIds.add(collectionSequence1.id.toString());
        collectionSequenceIds.add(collectionSequence2.id.toString());
        collectionSequenceIds.add(collectionSequence3.id.toString());

        for (String id : collectionSequenceIds) {
            Collection<CollectionSequence> checkIt = classificationApi.retrieveCollectionSequences(Arrays.asList(Long.parseLong(id)));

            if (checkIt == null || checkIt.size() == 0) {
                throw new RuntimeException("Where is my sequence at");
            }
        }

        Multimap<String, String> metadata = ArrayListMultimap.create();
        metadata.put("afield", String.valueOf(1L));
        Document document = TestDocumentHelper.createDocument(metadata);

        TaskMessage classifyMessage = createPolicyWorkerTaskMessage(taskId, taskClassifier, document,
                                                                                   collectionSequenceIds, executePolicy);

        Channel inputChannel = rabbitConnection.createChannel();
        Channel resultsChannel = rabbitConnection.createChannel();

        //send task to input queue
        BlockingQueue<Event<QueuePublisher>> pubEvents = createRabbitPublisher(inputChannel);

        //set up results queue consumer
        BlockingQueue<Event<QueueConsumer>> conEvents = new LinkedBlockingQueue<>();
        ConsumerCreationResult consumerResult = createRabbitConsumer(resultsChannel, conEvents, RESULT_QUEUENAME, 1, taskId);

        try {
            final TaskMessage resultWrapper = publishTaskAndAwaitThisMessagesResponse(pubEvents, consumerResult, classifyMessage);
            
            Codec codec = new JsonCodec();
           
            byte[] returnedTaskData = resultWrapper.getTaskData();
            checkTaskMessageReturned(resultWrapper, taskId);
            //check task data
            TaskResponse taskResponse = codec.deserialise(returnedTaskData, TaskResponse.class);
            Assert.assertNotNull(taskResponse);
            Collection<com.github.cafdataprocessing.worker.policy.shared.ClassifyDocumentResult> classifyResultCollection = taskResponse.getClassifiedDocuments();
            Assert.assertNotNull(classifyResultCollection);
            //should only be one result returned
            Assert.assertEquals(classifyResultCollection.size(), 3);

            Iterator<com.github.cafdataprocessing.worker.policy.shared.ClassifyDocumentResult> iter = classifyResultCollection.iterator();
            com.github.cafdataprocessing.worker.policy.shared.ClassifyDocumentResult classifyResult = null;

            // Check first sequence result matches
            {
                //get this result and check the properties on it are as expected
                classifyResult = iter.next();

                //check no incomplete collections returned
                Collection<Long> incomplete = classifyResult.getIncompleteCollections();
                Assert.assertEquals("Should be no incomplete collection", 0, incomplete.size());

                Assert.assertEquals("Should have a matched collection", 1, classifyResult.getMatchedCollections().size());
                Assert.assertEquals("Should match collection 1", collection1.id, classifyResult.getMatchedCollections().stream().findFirst().get().getId());
                checkClassifyResult(classifyResult, Arrays.asList(customIndexPolicy.id), collection1);

                String resultReference = classifyResult.getReference();
                Assert.assertEquals(resultReference, document.getReference());
            }

            // Check second sequence result matches.
            {
                // get the next result item, and check it...
                classifyResult = iter.next();

                Collection<Long> incomplete = classifyResult.getIncompleteCollections();
                Assert.assertEquals("Should be no incomplete collection", 0, incomplete.size());

                Assert.assertEquals("Should have a matched collection", 1, classifyResult.getMatchedCollections().size());
                Assert.assertEquals("Should match collection 2", collection2.id, classifyResult.getMatchedCollections().stream().findFirst().get().getId());
                checkClassifyResult(classifyResult, Arrays.asList(customCheckTempFieldsTagPolicy.id), collection2);

                //check reference is that of document we sent
                String resultReference = classifyResult.getReference();
                Assert.assertEquals(resultReference, document.getReference());
            }

            // Check third sequence result matches.
            {
                // get the next result item, and check it...
                classifyResult = iter.next();

                Collection<Long> incomplete = classifyResult.getIncompleteCollections();
                Assert.assertEquals("Should be no incomplete collection", 0, incomplete.size());

                Assert.assertEquals("Should have a matched collection", 1, classifyResult.getMatchedCollections().size());
                Assert.assertEquals("Should match collection", collection3.id, classifyResult.getMatchedCollections().stream().findFirst().get().getId());
                checkClassifyResult(classifyResult, Arrays.asList(customIndexPolicy.id), collection3);

                //check reference is that of document we sent
                String resultReference = classifyResult.getReference();
                Assert.assertEquals(resultReference, document.getReference());
            }
        } finally {
            closeRabbitConnections(resultsChannel, consumerResult);
        }
    }

    private Policy createCustomPolicyType(PolicyApi policyApi, String ptName, String ptShortName, String ptDesc, String ptJsonDefinition, String polName, String fieldName, String fieldValue, Integer polPriority)
        throws IOException
    {

        PolicyType policyType = new PolicyType();
        policyType.name = getUniqueString(ptName);
        policyType.shortName = ptShortName;
        policyType.description = ptDesc;

        TagPolicy examplePolicy = new TagPolicy();

        ObjectMapper mapper = new ObjectMapper();
        policyType.definition = mapper.readTree(ptJsonDefinition);
        policyType = policyApi.create(policyType);

        Policy policy = new Policy();
        policy.name = polName;

        if (!Strings.isNullOrEmpty(fieldName)) {

            FieldAction fieldAction = new FieldAction();
            fieldAction.setFieldName(fieldName);
            fieldAction.setAction(FieldAction.Action.ADD_FIELD_VALUE);
            fieldAction.setFieldValue(fieldValue);
            examplePolicy.setFieldActions(Arrays.asList(fieldAction));
        }

        policy.details = mapper.valueToTree(examplePolicy);
        policy.typeId = policyType.id;
        policy.priority = polPriority;

        return policyApi.create(policy);
    }

    private DocumentCollection getDocumentCollection(ClassificationApi classificationApi, Condition anyCondition, Collection<Long> overidePolicyIds)
    {
        DocumentCollection collection1 = new DocumentCollection();
        collection1.name = "Collection to drive custom policy";

        collection1.policyIds = new HashSet<>();
        // collection1.policyIds.add(customPolicy.id);
        collection1.policyIds.addAll(overidePolicyIds);
        collection1.condition = anyCondition;

        collection1 = classificationApi.create(collection1);
        return collection1;
    }

    @Test
    public void ClassifyDocumentNoWorkerInputConverterTest() throws WorkerException, IOException, QueueException, TimeoutException, InterruptedException, CodecException
    {
        final String taskId = UUID.randomUUID().toString();
        String taskClassifier = "aaaaaaaaaaaaa"; //Invalid InputConverter, expecting task to be rejected
        String collectionSequenceId = collectionSequence.id.toString();
        Boolean executePolicy = false;
        List<String> collectionSequenceIds = new ArrayList<>();
        collectionSequenceIds.add(collectionSequenceId);
        Multimap<String, String> metadata = ArrayListMultimap.create();
        metadata.put("afield", String.valueOf(1L));
        Document document = TestDocumentHelper.createDocument(metadata);

        TaskMessage classifyMessage = createPolicyWorkerTaskMessage(taskId, taskClassifier, document,
                                                                                   collectionSequenceIds, executePolicy);

        Channel inputChannel = rabbitConnection.createChannel();
        Channel resultsChannel = rabbitConnection.createChannel();

        //send task to input queue
        BlockingQueue<Event<QueuePublisher>> pubEvents = createRabbitPublisher(inputChannel);

        //set up results queue consumer
        BlockingQueue<Event<QueueConsumer>> conEvents = new LinkedBlockingQueue<>();
        ConsumerCreationResult consumerResult = createRabbitConsumer(resultsChannel, conEvents, RESULT_QUEUENAME, 1, taskId);

        try {
            final TaskMessage resultWrapper = publishTaskAndAwaitThisMessagesResponse(pubEvents, consumerResult, classifyMessage);

            byte[] returnedTaskData = resultWrapper.getTaskData();

            //check that task has status of Invalid_task.
            checkTaskMessageReturnedTaskStatus(resultWrapper, taskId, TaskStatus.INVALID_TASK);
        } finally {
            closeRabbitConnections(resultsChannel, consumerResult);
        }
    }

    @Test
    public void HealthCheckTest() throws IOException, ParseException
    {
        final URL url = new URL(properties.getWorkerHealthcheckAddress());
        HttpURLConnection huc = (HttpURLConnection) url.openConnection();
        int responseCode = huc.getResponseCode();

        //check that correct response code returned
        Assert.assertEquals(200, responseCode);

        //check the response body received
        BufferedReader reader = new BufferedReader(new InputStreamReader(huc.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String output;
        while ((output = reader.readLine()) != null) {
            builder.append(output);
        }
        String responseAsString = builder.toString();
        JSONObject healthCheckResult = (JSONObject) new JSONParser().parse(responseAsString);
        //expecting health status for worker
        JSONObject workerHealthObject = (JSONObject) healthCheckResult.get("worker");
        Boolean workerHealthStatus = (Boolean) workerHealthObject.get("healthy");
        Assert.assertEquals(true, workerHealthStatus);
    }

    @Override
    protected Connection getConnectionToClearDown()
    {
        RepositoryConnectionProvider repositoryConnectionProvider = getGenericApplicationContext().getBean(RepositoryConnectionProvider.class);
        return repositoryConnectionProvider.getConnection(RepositoryType.CONDITION_ENGINE);
    }

    private Connection getPolicyConnectionToClearDown()
    {
        RepositoryConnectionProvider repositoryConnectionProvider = getGenericApplicationContext().getBean(RepositoryConnectionProvider.class);
        return repositoryConnectionProvider.getConnection(RepositoryType.POLICY);
    }

    @Override
    protected void cleanupDatabase()
    {
        String projectId = getProjectId();
        try {
            try (Connection connection = getConnectionToClearDown()) {
                callClearDownStoredProc(connection, projectId);
            }
            try (Connection connection = getPolicyConnectionToClearDown()) {
                callClearDownStoredProc(connection, projectId);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void callClearDownStoredProc(Connection connection, String projectId) throws SQLException
    {
        if (connection != null) {
            ReleaseHistory version = new ReleaseHistory(VersionNumber.getCurrentVersion());
            CallableStatement callableStatement = connection.prepareCall(String.format("CALL sp_clear_down_tables_v%d_%d(?)", version.majorVersion, version.minorVersion));
            callableStatement.setString("projectId", projectId);
            callableStatement.execute();
        }
    }
}
