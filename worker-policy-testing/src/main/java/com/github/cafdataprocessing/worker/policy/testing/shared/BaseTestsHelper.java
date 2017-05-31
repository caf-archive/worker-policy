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
package com.github.cafdataprocessing.worker.policy.testing.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.cafdataprocessing.corepolicy.common.ClassificationApi;
import com.github.cafdataprocessing.corepolicy.common.ConversionConfiguration;
import com.github.cafdataprocessing.corepolicy.common.PolicyApi;
import com.github.cafdataprocessing.corepolicy.common.dto.*;
import com.github.cafdataprocessing.corepolicy.common.dto.conditions.Condition;
import com.github.cafdataprocessing.corepolicy.common.dto.conditions.NumberCondition;
import com.github.cafdataprocessing.corepolicy.common.dto.conditions.NumberOperatorType;
import com.github.cafdataprocessing.corepolicy.common.shared.CorePolicyObjectMapper;
import com.github.cafdataprocessing.corepolicy.domainModels.FieldAction;
import com.github.cafdataprocessing.corepolicy.policy.MetadataPolicy.MetadataPolicy;
import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.google.common.base.Strings;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.util.rabbitmq.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static com.github.cafdataprocessing.worker.policy.testing.shared.TestQueueConsumerImpl.getTaskMessageFromDelivery;
import static org.junit.Assert.assertTrue;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author getty
 */
public abstract class BaseTestsHelper extends IntegrationTestBase
{

    private final static Logger logger = LoggerFactory.getLogger(BaseTestsHelper.class);

    protected final CorePolicyObjectMapper mapper = new CorePolicyObjectMapper();
    protected static String INPUT_QUEUENAME = "Input";
    protected static String RESULT_QUEUENAME = "Results";
    protected static String CLASSIFICATION_QUEUENAME = "classificationqueue";
    protected static String ELASTIC_CLASSIFICATION_QUEUENAME = "elasticclassificationqueue";

    //number of seconds allowed to wait for result to be delivered to results queue consumer
    protected static int RESULT_TIMEOUTSECONDS;
    protected static String RABBIT_HOST = "localhost";
    protected static String RABBIT_USER = "guest";
    protected static String RABBIT_PASS = "guest";
    protected static int RABBIT_PORT = 5672;

    protected static com.rabbitmq.client.Connection rabbitConnection;

    protected final AnnotationConfigApplicationContext testingPropertiesApplicationContext;
    protected final PolicyWorkerTestingProperties properties;

    EventPoller<QueuePublisher> publisher;
    DefaultRabbitConsumer consumer;
    TestQueueConsumerImpl consumerImpl;

    public BaseTestsHelper()
    {

        testingPropertiesApplicationContext = new AnnotationConfigApplicationContext();
        testingPropertiesApplicationContext.register(ConversionConfiguration.class);
        testingPropertiesApplicationContext.register(PropertySourcesPlaceholderConfigurer.class);
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(PolicyWorkerTestingProperties.class);
        testingPropertiesApplicationContext.registerBeanDefinition("PolicyWorkerTestingProperties", beanDefinition);
        testingPropertiesApplicationContext.refresh();

        properties = testingPropertiesApplicationContext.getBean(PolicyWorkerTestingProperties.class);
    }

    public static void RabbitConnectionSetup() throws TimeoutException, IOException
    {
        rabbitConnection = RabbitHelper.getRabbitConnection(RABBIT_HOST, RABBIT_PORT, RABBIT_USER, RABBIT_PASS);
    }

    @Before
    public void SetupRabbitQueues() throws IOException, TimeoutException {
        if(rabbitConnection==null) {
            INPUT_QUEUENAME = properties.getInputQueueName();
            RESULT_QUEUENAME = properties.getResultQueueName();
            CLASSIFICATION_QUEUENAME = properties.getExternalClassificationQueue();
            ELASTIC_CLASSIFICATION_QUEUENAME = properties.getElasticClassificationQueue();
            RESULT_TIMEOUTSECONDS = properties.getResultTimeoutSeconds();
            RABBIT_HOST = properties.getRabbitHost();
            RABBIT_USER = properties.getRabbitUser();
            RABBIT_PASS = properties.getRabbitPass();
            RABBIT_PORT = properties.getRabbitPort();
            RabbitConnectionSetup();

            // the queues might not exist yet, if not do so now
            Channel inputChannel = rabbitConnection.createChannel();
            Channel resultsChannel = rabbitConnection.createChannel();

            RabbitUtil.declareWorkerQueue(inputChannel, INPUT_QUEUENAME);
            RabbitUtil.declareWorkerQueue(resultsChannel, RESULT_QUEUENAME);

            CloseChannel(inputChannel);
            CloseChannel(resultsChannel);
        }

        ClearRabbitQueues();
    }

    public static void ClearRabbitQueues() throws IOException
    {
        //clear input and result queues before each test
        PurgeQueue(INPUT_QUEUENAME);
        PurgeQueue(RESULT_QUEUENAME);
    }

    @AfterClass
    public static void StaticCleanup() throws IOException
    {
        //clear queues after tests ran
        ClearRabbitQueues();
        rabbitConnection.close();
        rabbitConnection = null;
    }

    public static void PurgeQueue(String queueName) throws IOException
    {
        Channel channel = rabbitConnection.createChannel();
        PurgeQueue(channel, queueName);
        CloseChannel(channel);
    }

    public static void PurgeQueue(Channel channel, String queueName) throws IOException
    {
        channel.queuePurge(queueName);
    }

    public static void CloseChannel(Channel channel) throws IOException {
        try {
            channel.close();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    protected void closeRabbitConnections(Channel resultsChannel, ConsumerCreationResult consumerResult)
    {
        try {
            consumer.shutdown();
            publisher.shutdown();
            try {
                resultsChannel.basicCancel(consumerResult.getConsumeId());
            } catch (IOException ex) {
                getLogger().warn("Failed to cancel results channel: ", ex);
            }
            try {
                resultsChannel.close();
            } catch (IOException | TimeoutException ex) {
                getLogger().warn("Failed to close results channel: ", ex);
            }
        } catch (Exception ex) {
            // dont let normal shutdown type issues cause a failure in the tests, this can happen in debugging, and we dont
            // want the shutdown of the queue being the recorded reason for the failure.
        }
    }

    protected CollectionSequence SetupCollectionSequence(ClassificationApi classificationApi, Long collectionId,
                                                         boolean returnCollectionSequenceInstead)
    {
        CollectionSequence collectionSequence = new CollectionSequence();
        collectionSequence.name = getUniqueString("ClassifyDocumentApiIT::setup_");
        collectionSequence.description = "Used in ClassifyDocumentApiIT tests.";
        collectionSequence.collectionSequenceEntries = new ArrayList<>();
        CollectionSequenceEntry collectionSequenceEntry = new CollectionSequenceEntry();
        collectionSequenceEntry.collectionIds = new HashSet<>(Arrays.asList(collectionId));
        collectionSequenceEntry.stopOnMatch = false;
        collectionSequenceEntry.order = 400;
        collectionSequence.collectionSequenceEntries.add(collectionSequenceEntry);

        collectionSequence = classificationApi.create(collectionSequence);

        if (returnCollectionSequenceInstead) {
            return collectionSequence;
        }

        throw new RuntimeException("This method is not used anymore!!");
    }

    protected DocumentCollection SetupCollection(Collection<Long> overridePolicyIds)
    {
        ClassificationApi classificationApi = genericApplicationContext.getBean(ClassificationApi.class);
        NumberCondition numberCondition = new NumberCondition();
        numberCondition.name = "afield condition 1";
        numberCondition.field = "afield";
        numberCondition.operator = NumberOperatorType.EQ;
        numberCondition.value = 1L;

        DocumentCollection collection1 = new DocumentCollection();
        collection1.name = "Collection 1";

        collection1.policyIds = new HashSet<>();
        if (overridePolicyIds != null) {
            collection1.policyIds.addAll(overridePolicyIds);
        }
        collection1.condition = numberCondition;

        collection1 = classificationApi.create(collection1);

        return collection1;

    }

    protected CollectionSequence SetupCollectionSequence(DocumentCollection collection)
    {
        ClassificationApi classificationApi = genericApplicationContext.getBean(ClassificationApi.class);
        return SetupCollectionSequence(classificationApi, collection.id, true);
    }

    protected CollectionSequence SetupCollectionSequenceWithPolicies(final Collection<Long> defaultPolicyIds)
    {
        return SetupCollectionSequenceWithPolicies(defaultPolicyIds, null);
    }

    protected CollectionSequence SetupCollectionSequenceWithPolicies(final Collection<Long> defaultPolicyIds,
                                                                     final Collection<Long> overridePolicyIds)
    {
        ClassificationApi classificationApi = genericApplicationContext.getBean(ClassificationApi.class);
        NumberCondition numberCondition = new NumberCondition();
        numberCondition.name = "afield condition 1";
        numberCondition.field = "afield";
        numberCondition.operator = NumberOperatorType.EQ;
        numberCondition.value = 1L;

        DocumentCollection collection1 = new DocumentCollection();
        collection1.name = "Collection 1";

        collection1.policyIds = new HashSet<>();
        if (overridePolicyIds == null) {
            collection1.policyIds.addAll(defaultPolicyIds);
        } else {
            collection1.policyIds.addAll(overridePolicyIds);
        }
        collection1.condition = numberCondition;

        collection1 = classificationApi.create(collection1);

        return SetupCollectionSequence(classificationApi, collection1.id, true);
    }

    protected Collection<Long> SetupPoliciesWithNoHandlersAvailable() throws IOException
    {
        PolicyApi policyApi = genericApplicationContext.getBean(PolicyApi.class);
        //register new policy type that will have no handlers associated
        MetadataPolicy metadataPolicy = new MetadataPolicy();
        metadataPolicy.setFieldActions(new ArrayList<>());
        FieldAction fieldAction = new FieldAction();
        fieldAction.setAction(FieldAction.Action.ADD_FIELD_VALUE);
        fieldAction.setFieldName("EXTERNAL_TEST");
        fieldAction.setFieldValue("1");
        metadataPolicy.getFieldActions().add(fieldAction);

        PolicyType policyType = createCustomPolicyType(policyApi, getUniqueString("NoHandler"), null);

        Policy policy = new Policy();
        policy.name = "Policy";
        policy.details = mapper.valueToTree(metadataPolicy);
        policy.typeId = policyType.id;
        policy.priority = 100;
        policy = policyApi.create(policy);

        ArrayList<Long> noHandlers = new ArrayList<>();
        noHandlers.add(policy.id);
        return noHandlers;
    }

    protected PolicyType createCustomPolicyType(PolicyApi policyApi, final String uniqueName, final String uniqueShortName)
    {
        PolicyType policyType = new PolicyType();
        policyType.conflictResolutionMode = ConflictResolutionMode.PRIORITY;
        policyType.name = uniqueName;
        policyType.shortName = Strings.isNullOrEmpty(uniqueShortName) ? getUniqueString("") : uniqueShortName;
        JsonNodeFactory nodeFactory = new JsonNodeFactory(false);
        policyType.definition = nodeFactory.objectNode();
        policyType = policyApi.create(policyType);
        return policyType;
    }

    public static String getUniqueString(String strPrefix)
    {
        return strPrefix + UUID.randomUUID().toString();
    }

    protected LinkedList<Long> SetupPolicies() throws IOException
    {
        PolicyApi policyApi = genericApplicationContext.getBean(PolicyApi.class);

        MetadataPolicy metadataPolicy = new MetadataPolicy();
        metadataPolicy.setFieldActions(new ArrayList<>());
        FieldAction fieldAction = new FieldAction();
        fieldAction.setAction(FieldAction.Action.ADD_FIELD_VALUE);
        fieldAction.setFieldName("EXTERNAL_TEST");
        fieldAction.setFieldValue("1");
        metadataPolicy.getFieldActions().add(fieldAction);

        PolicyType policyType = policyApi.retrievePolicyTypeByName("MetadataPolicy");
        Policy policy = new Policy();
        policy.name = "Policy";
        policy.details = mapper.valueToTree(metadataPolicy);
        policy.typeId = policyType.id;
        policy.priority = 100;
        policy = policyApi.create(policy);

        LinkedList<Long> policyIds = new LinkedList<>();
        policyIds.add(policy.id);

        return policyIds;
    }

    protected DocumentCollection getDocumentCollection(long colId) throws Exception
    {
        Collection<DocumentCollection> collections = getClassificationApi().retrieveCollections(Arrays.asList(colId), true, false);
        if (collections.isEmpty()) {
            throw new Exception("Failed to find document collection id: " + colId);
        }

        return collections.stream().filter(u -> u.id.equals(colId)).findFirst().get();
    }

    protected PolicyType createCustomExternalClassificationPolicyType(PolicyApi policyApi, String ptName, String ptShortName,
                                                                      String ptDesc, String ptJsonDefinition)
        throws IOException
    {

        PolicyType policyType = new PolicyType();
        policyType.name = getUniqueString(ptName);
        policyType.shortName = ptShortName;
        policyType.description = ptDesc;
        policyType.definition = mapper.readTree(ptJsonDefinition);

        return policyApi.create(policyType);
    }

    protected Policy createCustomExternalClassificationPolicy(PolicyApi policyApi, PolicyType policyType, CollectionSequence colSeq,
                                                              String polName, Integer polPriority, String queueName) throws IOException
    {

        String policyDef = "{\"classificationSequenceId\":" + colSeq.id + ",\"queueName\":\"" + queueName + "\"}";
        ObjectMapper mapper = new ObjectMapper();
        Policy policy = new Policy();
        policy.name = polName;
        policy.details = mapper.readTree(policyDef);
        policy.typeId = policyType.id;
        policy.priority = polPriority;

        return policyApi.create(policy);
    }

    protected DocumentCollection getWorkflowDocumentCollection(ClassificationApi classificationApi, Condition anyCondition)
    {
        DocumentCollection collection1 = new DocumentCollection();
        collection1.name = "Collection to drive workflow custom policy";
        collection1.condition = anyCondition;
        collection1 = classificationApi.create(collection1);
        return collection1;
    }

    protected DocumentCollection addPoliciesToDocumentCollection(ClassificationApi classificationApi, DocumentCollection collection,
                                                                 Collection<Long> policyIds)
    {
        collection.policyIds = new HashSet<>();
        collection.policyIds.addAll(policyIds);
        DocumentCollection collection1 = classificationApi.update(collection);
        return collection1;
    }

    protected void checkClassifyResult(com.github.cafdataprocessing.worker.policy.shared.ClassifyDocumentResult classifyResult,
                                       Collection<Long> policyIds, DocumentCollection collection)
    {
        //check no incomplete collections returned
        Collection<Long> incomplete = classifyResult.getIncompleteCollections();
        Assert.assertEquals(0, incomplete.size());

        //check expected policy was resolved
        Collection<Long> resolvedPolicies = classifyResult.getResolvedPolicies();
        Assert.assertArrayEquals(policyIds.toArray(), resolvedPolicies.toArray());

        //check that the collection we expected is the one matched
        Collection<com.github.cafdataprocessing.worker.policy.shared.MatchedCollection> matchedCollections = classifyResult.getMatchedCollections();
        Assert.assertEquals(1, matchedCollections.size());
        com.github.cafdataprocessing.worker.policy.shared.MatchedCollection matchedCollection = matchedCollections.iterator().next();

        Assert.assertEquals(collection.id, matchedCollection.getId());
        Assert.assertEquals(collection.name, matchedCollection.getName());
        //check the condition on the collection
        Collection<com.github.cafdataprocessing.worker.policy.shared.MatchedCondition> matchedConditions = matchedCollection.getMatchedConditions();
        assertTrue("Number of matched conditions should be positive ", matchedConditions.size() > 0);
        com.github.cafdataprocessing.worker.policy.shared.MatchedCondition matchedCondition = matchedConditions.stream().filter(u -> u.getId().equals(
            collection.condition.id)).findFirst().get();
        // compare the 2 top root condition matches at least, everything below should be fine
        // in our present test scenario.
        Assert.assertEquals(collection.condition.id, matchedCondition.getId());
        Assert.assertEquals(collection.condition.name, matchedCondition.getName());

        //check the policy on the collection
        Collection<com.github.cafdataprocessing.worker.policy.shared.CollectionPolicy> matchedPolicies = matchedCollection.getPolicies();
        Assert.assertEquals(collection.policyIds.size(), matchedPolicies.size());
        if (!matchedPolicies.isEmpty()) {
            com.github.cafdataprocessing.worker.policy.shared.CollectionPolicy matchedPolicy = matchedPolicies.iterator().next();
            Assert.assertEquals(collection.policyIds.iterator().next(), matchedPolicy.getId());
        }
    }

    /*
        Checks the task message passed in has the taskId set to expectedTaskId, that TaskStatus is RESULT_SUCCESS
     */
    protected void checkTaskMessageReturned(TaskMessage resultWrapper, String expectedTaskId)
    {
        checkTaskMessageReturnedTaskStatus(resultWrapper, expectedTaskId, TaskStatus.RESULT_SUCCESS);
    }

    /*
        Checks the task message passed in has the taskId set to expectedTaskId, that TaskStatus is RESULT_EXCEPTION
     */
    protected void checkTaskMessageReturnedException(TaskMessage resultWrapper, String expectedTaskId)
    {
        checkTaskMessageReturnedTaskStatus(resultWrapper, expectedTaskId, TaskStatus.RESULT_EXCEPTION);
    }

    /*
        Checks the task message passed in has the taskId set to expectedTaskId, that TaskStatus is RESULT_FAILURE
     */
    protected void checkTaskMessageReturnedTaskStatus(TaskMessage resultWrapper, String expectedTaskId, TaskStatus expectedStatus)
    {
        //check that this has the task ID we specified when adding to Input Queue
        Assert.assertEquals(expectedTaskId, resultWrapper.getTaskId());
        TaskStatus status = resultWrapper.getTaskStatus();
        //check that task status is x
        Assert.assertEquals(expectedStatus, status);
    }

    protected TaskMessage publishTaskAndAwaitThisMessagesResponse(final BlockingQueue<Event<QueuePublisher>> pubEvents,
                                                                  final ConsumerCreationResult consumerResult,
                                                                  final TaskMessage classifyMessage, final String taskMessageExpected)
        throws InterruptedException, CodecException, JsonProcessingException
    {
        return publishTaskAndAwaitResultAsTaskMessage(pubEvents, consumerResult.getLatch(), classifyMessage, taskMessageExpected);
    }

    protected TaskMessage publishTaskAndAwaitThisMessagesResponse(BlockingQueue<Event<QueuePublisher>> pubEvents,
                                                                  ConsumerCreationResult consumerResult, TaskMessage classifyMessage)
        throws InterruptedException, CodecException, JsonProcessingException
    {
        return publishTaskAndAwaitThisMessagesResponse(pubEvents, consumerResult, classifyMessage, null);
    }

    /*
        Utility method to publish a task to input queue and attempt to read the expected taskId back as a response from results queue.
     */
    protected TaskMessage publishTaskAndAwaitResultAsTaskMessage(BlockingQueue<Event<QueuePublisher>> pubEvents, CountDownLatch latch,
                                                                 TaskMessage message, String expectedTaskMessage)
        throws InterruptedException, JsonProcessingException, CodecException
    {
        //add message to input queue
        //pubEvents.add(new PublishQueueEvent(PublishEventType.PUBLISH, 0L, mapper.writeValueAsBytes(message), INPUT_QUEUENAME));
        pubEvents.add(new PublisherPublishEvent(mapper.writeValueAsBytes(message)));

        Delivery deliveredResult = null;
        final String awaitThisMessage = Strings.isNullOrEmpty(expectedTaskMessage) ? message.getTaskId() : expectedTaskMessage;

        //give some time for task to be processed and result consumed (result consumer should update latch on consumption)
        final boolean timeout = !latch.await(RESULT_TIMEOUTSECONDS, TimeUnit.SECONDS);

        return getResultAsTaskMessage(awaitThisMessage, timeout);

    }

    /**
     * Utility method to get a specific message from the queue without any waiting.
     *
     * @param getThisMessage
     * @return
     */
    protected TaskMessage getResultAsTaskMessage(String getThisMessage)
    {
        return getResultAsTaskMessage(getThisMessage, false);
    }

    /**
     * Utility method to get a specific message from the queue without any waiting.
     *
     * @param awaitThisMessage
     * @param timeout
     * @return
     */
    private TaskMessage getResultAsTaskMessage(String awaitThisMessage, boolean timeout)
    {
        Delivery deliveredResult;
        deliveredResult = consumerImpl.getDelivery(awaitThisMessage);

        if (timeout && deliveredResult == null) {
            // timeout happened while waiting on the given message, make final check for delivery then give up.
            logger.error("Timeout happened while waiting for the response to taskId: " + awaitThisMessage);
            throw new RuntimeException("Timeout happened while waiting for the response to taskId: " + awaitThisMessage);
        }

        // check if we haven't got a result, try relatching and waiting again -> we should only return from here with correct response or timeout.
        if (deliveredResult == null) {
            // the latch has fired, so the message queue filter has received a message the user was interested in, but they then requested
            // an item that wasn't in the queue yet.
            // This can happen if you do something like this: filter by taskId: 123.
            // Request child 123.2*, but the queue can accept 123 / 123.1 and 123.2, so any arriving will wake this code up.
            // We can either go back to latching for another message OR I have opted to throw, and fix the tests to filter better to only
            // the message you want.  So change here would be to use filter: 123.2, and publishAndAwait on the same 123.2 id.
            logger.warn("Failed to locate the message you requested taskId: " + awaitThisMessage);

            debugCurrentQueueItems();

            throw new RuntimeException(
                "Failed to locate the message you requested taskId: " + awaitThisMessage + "\nBut the message queue did receive a valid item matching the filter id.  Please make sure that you aren't allowing more than 1 item in your filter list ( and are latching on count 1) if you want a specific child only:\n"
                + "Ensure that the queue latch=1, and change the queue filter, or increase the latch to the number of children that the workflow accepts into the queue.");
        }

        TaskMessage resultMessage = getTaskMessageFromDelivery(deliveredResult);
        return resultMessage;
    }

    private void debugCurrentQueueItems()
    {
        logger.warn("Message queue currently contains: " + consumerImpl.getDelivery().size());

        // do we have any items on the queue at all, if so, output what we have?
        for (Map.Entry<String, Delivery> deliveredItem : consumerImpl.getDelivery().entrySet()) {
            Delivery tmpDel = deliveredItem.getValue();
            TaskMessage tm = getTaskMessageFromDelivery(tmpDel);
            logger.warn(
                String.format("Queue contains other item: {%s} TrackingInfo jobTaskId: {%s}",
                              tm.getTaskId(), tm.getTracking() == null ? "null" : tm.getTracking().getJobTaskId()));
        }
    }

    /**
     * Create policy worker task message to classify and execute on a document, given a workflow identifiers.
     *
     * @param taskId
     * @param taskClassifier
     * @param document
     * @param workflowId
     * @param executePolicy
     * @return
     * @throws CodecException
     */
    protected TaskMessage createPolicyWorkerTaskMessage(final String taskId, final String taskClassifier, final Document document,
                                                        final Long workflowId, final boolean executePolicy)
        throws CodecException
    {
        // create with job tracking by default.
        TrackingInfo trackingInfo = createTrackingInfo(taskId);

        TaskMessage classifyMessage
            = TestTaskMessageHelper.getClassifyTaskMessage(taskId, taskClassifier, document,
                                                           workflowId.toString(), executePolicy, getProjectId(),
                                                           properties.getDataStorePartialReference(), INPUT_QUEUENAME, trackingInfo);
        return classifyMessage;
    }

    /**
     * Create policy worker task message to classify and execute on a document, given a set of collection sequence identifiers.
     *
     * @param taskId
     * @param taskClassifier
     * @param document
     * @param collectionSequenceIds
     * @param executePolicy
     * @return
     * @throws CodecException
     */
    protected TaskMessage createPolicyWorkerTaskMessage(final String taskId, final String taskClassifier, final Document document,
                                                        final List<String> collectionSequenceIds, final boolean executePolicy)
        throws CodecException
    {
        // create with job tracking by default.
        TrackingInfo trackingInfo = createTrackingInfo(taskId);

        TaskMessage classifyMessage
            = TestTaskMessageHelper.getClassifyTaskMessage(taskId, taskClassifier, document,
                                                           collectionSequenceIds, executePolicy, getProjectId(),
                                                           properties.getDataStorePartialReference(), INPUT_QUEUENAME, trackingInfo);
        return classifyMessage;
    }

    /**
     * Create policy worker task message to classify and execute on a document, given a workflow identifiers.
     *
     * @param taskId
     * @param taskClassifier
     * @param document
     * @param workflowId
     * @param collectionSequenceIds
     * @param policyIds
     * @param executePolicy
     * @param executeOnly
     * @return
     * @throws CodecException
     */
    protected TaskMessage createPolicyWorkerTaskMessage(final String taskId, final String taskClassifier, final Document document,
                                                      final Long workflowId, final List<String> collectionSequenceIds,
                                                      final Collection<Long> policyIds, final boolean executePolicy,
                                                      final boolean executeOnly)
        throws CodecException
    {
        // create with job tracking by default.
        TrackingInfo trackingInfo = createTrackingInfo(taskId);

        if (executeOnly) {
            final TaskMessage taskMessage = TestTaskMessageHelper.getExecuteTaskMessage(taskId, taskClassifier, document, policyIds,
                                                                                     getProjectId(), collectionSequenceIds, INPUT_QUEUENAME, trackingInfo);
            return taskMessage;
        }
        
        final TaskMessage taskMessage
            = TestTaskMessageHelper.getClassifyTaskMessage(taskId, taskClassifier, document,
                                                           workflowId.toString(), executePolicy, getProjectId(),
                                                           properties.getDataStorePartialReference(), INPUT_QUEUENAME, trackingInfo);
        return taskMessage;
    }

    protected TrackingInfo createTrackingInfo(String taskId)
    {
        TrackingInfo trackingInfo = new TrackingInfo();
        trackingInfo.setTrackingPipe(properties.getJobTrackingPipe());
        trackingInfo.setJobTaskId(taskId);
        // if we need to set this, set to rubbish to never remove the tracking info.
        trackingInfo.setTrackTo("xyz");
        return trackingInfo;
    }

    /*
        Utility method to create publisher to put messages onto an input channel. Returns the publish queue for messages to added to
     */
    protected BlockingQueue<Event<QueuePublisher>> createRabbitPublisher(Channel inputChannel)
    {
        BlockingQueue<Event<QueuePublisher>> pubEvents = new LinkedBlockingQueue<>();
        TestQueuePublisherImpl publisherImpl = new TestQueuePublisherImpl(inputChannel, INPUT_QUEUENAME);
        this.publisher = new EventPoller<>(2, pubEvents, publisherImpl);
        new Thread(publisher).start();
        return pubEvents;
    }

    protected class ConsumerCreationResult
    {
        private CountDownLatch latch;
        private String consumeId;

        public ConsumerCreationResult(CountDownLatch latch, String consumeId)
        {
            this.latch = latch;
            this.consumeId = consumeId;
        }

        public CountDownLatch getLatch()
        {
            return latch;
        }

        public String getConsumeId()
        {
            return consumeId;
        }
    }

    protected ConsumerCreationResult createRabbitConsumer(Channel resultsChannel, BlockingQueue<Event<QueueConsumer>> conEvents,
                                                          final int latchCount, final String taskIdFilter)
        throws IOException
    {
        return createRabbitConsumer(resultsChannel, conEvents, RESULT_QUEUENAME, 1, taskIdFilter);
    }

    protected ConsumerCreationResult createRabbitConsumer(Channel resultsChannel, BlockingQueue<Event<QueueConsumer>> conEvents,
                                                          String queueName, int latchCount, final String taskIdFilter)
        throws IOException
    {
        CountDownLatch latch = new CountDownLatch(latchCount);
        RabbitUtil.declareWorkerQueue(resultsChannel, queueName);
        this.consumerImpl = new TestQueueConsumerImpl(latch, conEvents, resultsChannel, taskIdFilter);
        this.consumer = new DefaultRabbitConsumer(conEvents, consumerImpl);
        String consumeId = resultsChannel.basicConsume(queueName, consumer);
        new Thread(consumer).start();
        return new ConsumerCreationResult(latch, consumeId);
    }

    /*
        Utility class used to publish messages onto a RabbitMQ queue
     */
    protected class TestQueuePublisherImpl implements QueuePublisher
    {
        private final Channel channel;
        private final String queueName;

        public TestQueuePublisherImpl(final Channel ch, final String queue)
        {
            this.channel = Objects.requireNonNull(ch);
            this.queueName = Objects.requireNonNull(queue);
        }

        @Override
        public void handlePublish(final byte[] data)
        {
            try {
                channel.basicPublish("", queueName, MessageProperties.TEXT_PLAIN, data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
