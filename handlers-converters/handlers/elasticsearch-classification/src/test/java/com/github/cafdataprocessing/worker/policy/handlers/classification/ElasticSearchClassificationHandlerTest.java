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
package com.github.cafdataprocessing.worker.policy.handlers.classification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cafdataprocessing.corepolicy.common.CorePolicyApplicationContext;
import com.github.cafdataprocessing.worker.policy.WorkerResponseHolder;
import com.github.cafdataprocessing.worker.policy.WorkerTaskResponsePolicyHandler;
import com.github.cafdataprocessing.worker.policy.common.DataStoreAwareInputStream;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.DocumentImpl;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.util.ref.ReferencedData;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.io.IOException;
import java.util.*;

/**
 * Unit tests for the ElasticSearchClassificationHandler
 */
@RunWith(Enclosed.class)
public class ElasticSearchClassificationHandlerTest {
    /**
     * Tests for ElasticSearchClassificationHandler that are parameterized (running multiple times with different inputs)
     */
    @RunWith(Parameterized.class)
    public static class ElasticSearchClassificationHandlerParamsTests {
        //private variables declared to enable use of Parameterized JUnit test
        private Long classificationSequenceId;
        private Long workflowId;
        private String queueName;

        private static CorePolicyApplicationContext applicationContext;

        public ElasticSearchClassificationHandlerParamsTests(Long classificationSequenceId, Long workflowId, String queueName)
        {
            this.classificationSequenceId = classificationSequenceId;
            this.workflowId = workflowId;
            this.queueName = queueName;
        }

        @BeforeClass
        public static void Setup(){
            applicationContext = ApplicationContextSetup.Create();
        }

        private static ElasticSearchClassificationWorkerProperties getProperties()
        {
            AnnotationConfigApplicationContext propertiesApplicationContext = new AnnotationConfigApplicationContext();
            propertiesApplicationContext.register(PropertySourcesPlaceholderConfigurer.class);
            RootBeanDefinition beanDefinition = new RootBeanDefinition();
            beanDefinition.setBeanClass(ElasticSearchClassificationWorkerProperties.class);
            propertiesApplicationContext.registerBeanDefinition("ElasticSearchClassificationWorkerProperties", beanDefinition);
            propertiesApplicationContext.refresh();
            return propertiesApplicationContext.getBean(ElasticSearchClassificationWorkerProperties.class);
        }

        @org.junit.runners.Parameterized.Parameters
        public static Collection<Object[]> handlePolicyParams()
        {
            //defining an array of "collectionSequenceId, "workflowId" and "queueName" properties that should be passed to sets of tests.
            return Arrays.asList(new Object[][]{
                    {1L, null, "TestQueue1"}, //should have collection sequence ID set
                    {1L, null, null}, //should have collection sequence ID set and default queue value
                    {2L, null, "TestQueue1"}, //should have collection sequence ID set
                    {null, 1L, "TestWorkflowQueue"}, // should only have workflowID set
                    {null, 50L, "TestWorkflowQueue"}, // should only have workflowID set
                    {1L, 1L, "TestQueue1"} //testing that if workflow ID and collection sequence passed that only workflow ID set
            });
        }

        @Test
        public void testHandlePolicy() {
            Document testDoc = new DocumentImpl();
            String testDocRef = UUID.randomUUID().toString();
            testDoc.setReference(testDocRef);
            testDoc.getStreams().put("ref1", new DataStoreAwareInputStream(ReferencedData.getReferencedData("ref2"), null));

            Policy testPolicy = new Policy();
            testPolicy.id = 1L;
            //set policy definition
            ObjectMapper mapper = new ObjectMapper();
            ClassificationPolicyDefinition polDef = new ClassificationPolicyDefinition();
            if(classificationSequenceId!=null) {
                polDef.classificationSequenceId = classificationSequenceId;
            }
            if(workflowId!=null){
                polDef.workflowId = workflowId;
            }
            polDef.queueName = queueName;
            testPolicy.details = mapper.valueToTree(polDef);
            Long testColSeqId = 1L;

            WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
            TaskData taskData = new TaskData();
            com.github.cafdataprocessing.worker.policy.shared.Document externalDoc = new com.github.cafdataprocessing.worker.policy.shared.Document();
            externalDoc.setDocuments(new ArrayList<>());
            String externalDocRef = UUID.randomUUID().toString();
            externalDoc.setReference(externalDocRef);
                        taskData.setOutputPartialReference("testOutputPartialReferences");
            taskData.setDocument(externalDoc);
            workerResponseHolder.setTaskData(taskData);

            ElasticSearchClassificationHandler handler = new ElasticSearchClassificationHandler();
            handler.setApplicationContext(applicationContext);
            handler.handle(testDoc, testPolicy, testColSeqId);

            checkHandleWorkerResponse(testDocRef);
        }

        private void checkHandleWorkerResponse(String testDocRef)
        {
            WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
            WorkerTaskResponsePolicyHandler.WorkerHandlerResponse response = workerResponseHolder.getChainWorkerResponse();
            Assert.assertNotNull(response);
            TaskStatus taskStatus = response.getTaskStatus();

            Assert.assertEquals("Check that worker response is NEW_TASK", TaskStatus.NEW_TASK, taskStatus);
            Assert.assertEquals("Expecting message type to be PolicyWorker", "PolicyWorker", response.getMessageType());
            ElasticSearchClassificationWorkerProperties loadedProperties = getProperties();
            //if a null queue name was passed on policy definition then the queue specified in properties file will have been used
            String expectedQueueName = loadedProperties.getTaskQueueName();
            if(queueName!=null)
            {
                expectedQueueName = queueName;
            }
            Assert.assertEquals("Expecting destination queue to be the same as that passed on policy definition.", expectedQueueName, response.getQueueReference());
            TaskData task = (TaskData) response.getData();
            Assert.assertEquals("Expecting input file on task to be document reference we set", testDocRef, task.getDocument().getReference());
            List<String> colSeqs = task.getCollectionSequences();

            if(this.classificationSequenceId == null || this.workflowId != null){
                Assert.assertEquals("Expecting collection sequence ID to not be set on the task.", 0, colSeqs.size());
            }
            else{
                Assert.assertEquals("Expecting collection sequences returned to be of size 1", 1, colSeqs.size());
                Assert.assertEquals("Expecting collection sequence returned to be the one we passed in",
                        (long)classificationSequenceId, Long.parseLong(colSeqs.get(0)));
            }

            String workflowIdReturned = task.getWorkflowId();
            if(this.workflowId!=null) {
                Assert.assertEquals("Expecting workflow ID set on task to be the one we passed in", (long) workflowId,
                        Long.parseLong(workflowIdReturned));
            }
            else{
                Assert.assertEquals("Expecting workflow ID to not be set on the task.", null,
                        workflowIdReturned);
            }
        }
    }

    /**
     * Test for ElasticSearchClassificationHandler that are not paramterized and thus only need to run once.
     */
    public static class NoParamsTests {
        private static CorePolicyApplicationContext applicationContext;

        @BeforeClass
        public static void Setup(){
            applicationContext = ApplicationContextSetup.Create();
        }

        @Test
        public void testLoadPolicyDefFromFile() throws IOException {
            ElasticSearchClassificationHandler handler = new ElasticSearchClassificationHandler();
            PolicyType policyType = handler.getPolicyType();
            JsonNode policyDef = new ObjectMapper().readTree(this.getClass().getResource("/elastic-search-classification-policy-definition.json"));
            Assert.assertEquals("Should have loaded policy definition from file", policyDef, policyType.definition);
        }

        /**
         * Tests that metadata references on sub-documents are propagated to the document sent in the worker task message
         */
        @Test
        public void checkSubDocMetadataReferencesPassed(){
            Document testDoc = new DocumentImpl();
            String testDocRef = UUID.randomUUID().toString();
            testDoc.setReference(testDocRef);
            Multimap<String, ReferencedData> metadataReferences = ArrayListMultimap.create();
            String rootRefKey = "ref1";
            ReferencedData rootRefValue = ReferencedData.getReferencedData("ref2");
            metadataReferences.put(rootRefKey, rootRefValue);
            testDoc.getStreams().put(rootRefKey, new DataStoreAwareInputStream(rootRefValue, null));
            // add sub-documents
            Document firstSubDoc = new DocumentImpl();
            firstSubDoc.setReference(UUID.randomUUID().toString());
            String firstSubRefKey = UUID.randomUUID().toString();
            ReferencedData firstSubRefValue = ReferencedData.getReferencedData(UUID.randomUUID().toString());
            firstSubDoc.getStreams().put(firstSubRefKey,
                    new DataStoreAwareInputStream(firstSubRefValue, null));
            Document secondSubDoc = new DocumentImpl();
            secondSubDoc.setReference(UUID.randomUUID().toString());
            String secondSubRefKey = UUID.randomUUID().toString();
            ReferencedData secondSubRefValue = ReferencedData.getReferencedData(UUID.randomUUID().toString());
            secondSubDoc.getStreams().put(secondSubRefKey,
                    new DataStoreAwareInputStream(secondSubRefValue, null));
            firstSubDoc.getDocuments().add(secondSubDoc);
            testDoc.getDocuments().add(firstSubDoc);

            Policy testPolicy = new Policy();
            testPolicy.id = 1L;
            //set policy definition
            ObjectMapper mapper = new ObjectMapper();
            ClassificationPolicyDefinition polDef = new ClassificationPolicyDefinition();
            polDef.classificationSequenceId = 1L;
            polDef.queueName = "test";
            testPolicy.details = mapper.valueToTree(polDef);
            Long testColSeqId = 1L;

            WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
            TaskData taskData = new TaskData();
            com.github.cafdataprocessing.worker.policy.shared.Document externalDoc = new com.github.cafdataprocessing.worker.policy.shared.Document();
            externalDoc.setDocuments(new ArrayList<>());
            String externalDocRef = UUID.randomUUID().toString();
            externalDoc.setReference(externalDocRef);
            taskData.setOutputPartialReference("testOutputPartialReferences");
            taskData.setDocument(externalDoc);
            workerResponseHolder.setTaskData(taskData);

            ElasticSearchClassificationHandler handler = new ElasticSearchClassificationHandler();
            handler.setApplicationContext(applicationContext);
            handler.handle(testDoc, testPolicy, testColSeqId);

            workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
            WorkerTaskResponsePolicyHandler.WorkerHandlerResponse response = workerResponseHolder.getChainWorkerResponse();
            Assert.assertNotNull(response);
            TaskData task = (TaskData) response.getData();
            com.github.cafdataprocessing.worker.policy.shared.Document handlerOutputDoc = task.getDocument();
            //check for metadata reference on root document
            Assert.assertTrue("Expecting root document " +
                    "to have metadata reference.",
                    handlerOutputDoc.getMetadataReferences().containsKey(rootRefKey));
            Collection<ReferencedData> outputRootRefValues = handlerOutputDoc.getMetadataReferences().get(rootRefKey);
            Assert.assertTrue("Expecting root document to have metadata reference values.", !outputRootRefValues.isEmpty());
            ReferencedData outputRootRefValue = outputRootRefValues.iterator().next();
            Assert.assertEquals("Expecting root document metadata reference to have expected value.",
                    rootRefValue.getReference(),
                    outputRootRefValue.getReference());

            Assert.assertTrue("Sub-document should have been output.", !handlerOutputDoc.getDocuments().isEmpty());
            com.github.cafdataprocessing.worker.policy.shared.Document outputFirstLevelSubDoc =
                    handlerOutputDoc.getDocuments().iterator().next();
            Assert.assertTrue("Metadata references on first level sub-doc should not be empty.",
                    !outputFirstLevelSubDoc.getMetadataReferences().isEmpty());
            Collection<ReferencedData> outputFirstMetaRefs = outputFirstLevelSubDoc.getMetadataReferences().get(firstSubRefKey);
            Assert.assertTrue("Metadata reference values for first sub doc expected key should not be empty.",
                    !outputFirstMetaRefs.isEmpty());
            ReferencedData outputFirstMetaRefValue = outputFirstMetaRefs.iterator().next();
            Assert.assertEquals("Output metadata ref value for first sub doc should match expected value.",
                    firstSubRefValue.getReference(),
                    outputFirstMetaRefValue.getReference());

            Assert.assertTrue("Second level sub-document should have been output.", !outputFirstLevelSubDoc.getDocuments().isEmpty());
            com.github.cafdataprocessing.worker.policy.shared.Document outputSecondLevelSubDoc =
                    outputFirstLevelSubDoc.getDocuments().iterator().next();
            Assert.assertTrue("Metadata references on second level sub-doc should not be empty.",
                    !outputSecondLevelSubDoc.getMetadataReferences().isEmpty());
            Collection<ReferencedData> outputSecondMetaRefs = outputSecondLevelSubDoc.getMetadataReferences().get(secondSubRefKey);
            Assert.assertTrue("Metadata reference values for second sub doc expected key should not be empty.",
                    !outputSecondMetaRefs.isEmpty());
            ReferencedData outputSecondMetaRefValue = outputSecondMetaRefs.iterator().next();
            Assert.assertEquals("Output metadata ref value for second sub doc should match expected value.",
                    secondSubRefValue.getReference(),
                    outputSecondMetaRefValue.getReference());
        }

        @Test
        public void checkForExceptionWithNoRef(){
            Document testDoc = new DocumentImpl();
            String testDocRef = UUID.randomUUID().toString();
            testDoc.setReference(testDocRef);
            testDoc.getStreams().put("ref1",
                    new DataStoreAwareInputStream(ReferencedData.getReferencedData("ref2"), null));

            Policy testPolicy = new Policy();
            testPolicy.id = 1L;
            //set policy definition
            ObjectMapper mapper = new ObjectMapper();
            ClassificationPolicyDefinition polDef = new ClassificationPolicyDefinition();
            polDef.classificationSequenceId = 1L;
            polDef.queueName = "test";
            testPolicy.details = mapper.valueToTree(polDef);
            Long testColSeqId = 1L;

            WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
            TaskData taskData = new TaskData();
            com.github.cafdataprocessing.worker.policy.shared.Document externalDoc = new com.github.cafdataprocessing.worker.policy.shared.Document();
            externalDoc.setDocuments(new ArrayList<>());
            String externalDocRef = UUID.randomUUID().toString();
            externalDoc.setReference(externalDocRef);
            taskData.setDocument(externalDoc);
            workerResponseHolder.setTaskData(taskData);

            ElasticSearchClassificationHandler handler = new ElasticSearchClassificationHandler();
            handler.setApplicationContext(applicationContext);
            boolean exceptionThrown = false;
            try {
                handler.handle(testDoc, testPolicy, testColSeqId);
            }
            catch (RuntimeException e){
                exceptionThrown = true;
            }
            Assert.assertTrue("Runtime should have thrown",exceptionThrown);
        }
    }

}
