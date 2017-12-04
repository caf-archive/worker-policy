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

    private DataStore getFsDataStore() throws DataStoreException
    {
        FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(System.getenv("CAF_INTEGRATIONTESTS_FS_PATH"));
        return new FileSystemDataStore(conf);
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
