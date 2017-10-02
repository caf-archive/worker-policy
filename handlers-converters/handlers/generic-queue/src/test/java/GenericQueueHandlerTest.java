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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cafdataprocessing.corepolicy.common.CorePolicyApplicationContext;
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.DocumentImpl;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import com.github.cafdataprocessing.corepolicy.common.shared.TemporaryEnvChanger;
import com.github.cafdataprocessing.entity.fields.*;
import com.github.cafdataprocessing.worker.policy.WorkerRequestHolder;
import com.github.cafdataprocessing.worker.policy.WorkerResponseHolder;
import com.github.cafdataprocessing.worker.policy.WorkerTaskResponsePolicyHandler;
import com.github.cafdataprocessing.worker.policy.handlers.generic.GenericQueueHandler;
import com.github.cafdataprocessing.worker.policy.handlers.generic.GenericQueueHandlerProperties;
import com.github.cafdataprocessing.worker.policy.handlers.shared.PolicyQueueDefinition;
import com.github.cafdataprocessing.worker.policy.handlers.shared.document.SharedDocument;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.hpe.caf.api.worker.TaskStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Tests for GenericQueueHandler class
 */
public class GenericQueueHandlerTest {

    private CorePolicyApplicationContext applicationContext;
    private GenericQueueHandler handler;


    @Before
    public void SetupApplicationContext() {
        this.applicationContext = new CorePolicyApplicationContext();
        
        createBeanDefinition(WorkerResponseHolder.class, "WorkerResponseHolder", "thread");
        createBeanDefinition(WorkerRequestHolder.class, "WorkerRequestHolder", "thread");
        
        applicationContext.refresh();
        applicationContext.getBean(WorkerResponseHolder.class).setTaskData(new TaskData());
        handler = new GenericQueueHandler();
        handler.setApplicationContext(applicationContext);
    }

    private void createBeanDefinition(Class<?> beanClass, String beanClassName, String beanScope ) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setScope(beanScope);
        beanDefinition.setBeanClass(beanClass);

        applicationContext.registerBeanDefinition(beanClassName, beanDefinition);
    }
    
    @Test
    public void testGenericHandler() throws IOException {

        Document document = new DocumentImpl();
        String testDocumentReference = "test";
        document.setReference(testDocumentReference);
        Policy testPolicy = new Policy();
        testPolicy.id = 1L;
        ObjectMapper mapper = new ObjectMapper();
        String queueName = "testQueue";
        String messageType = "TestWorker";
        int apiVersion = 2;
        testPolicy.details = mapper.readTree("{\"queueName\": \"" + queueName + "\", \"messageType\":\"" + messageType + "\", \"apiVersion\":" + apiVersion + "}");
        Long testColSeqId = 1L;

        handler.handle(document, testPolicy, testColSeqId);
        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        WorkerTaskResponsePolicyHandler.WorkerHandlerResponse response = workerResponseHolder.getChainWorkerResponse();
        Assert.assertNotNull(response);
        TaskStatus taskStatus = response.getTaskStatus();
        Assert.assertEquals("Check that worker response is NEW_TASK", TaskStatus.NEW_TASK, taskStatus);
        Assert.assertEquals("Expecting message type to be TestWorker", messageType, response.getMessageType());
        Assert.assertEquals("Expecting apiVersion to be 2", apiVersion, response.getApiVersion());
        Assert.assertEquals("Expecting destination queue to be the same as that set in properties object", queueName, response.getQueueReference());

        SharedDocument task = (SharedDocument) response.getData();
        Assert.assertNotNull(task);
        Assert.assertEquals("Expecting source data reference to match ", testDocumentReference, task.getReference());
    }

    @Test
    public void testGenericHandlerDefault() throws IOException {

        Document document = new DocumentImpl();
        String testDocumentReference = "test";
        document.setReference(testDocumentReference);
        Policy testPolicy = new Policy();
        testPolicy.id = 1L;
        ObjectMapper mapper = new ObjectMapper();
        testPolicy.details = mapper.readTree("{}");
        Long testColSeqId = 1L;

        handler.handle(document, testPolicy, testColSeqId);
        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        WorkerTaskResponsePolicyHandler.WorkerHandlerResponse response = workerResponseHolder.getChainWorkerResponse();
        Assert.assertNotNull(response);
        TaskStatus taskStatus = response.getTaskStatus();
        Assert.assertEquals("Check that worker response is NEW_TASK", TaskStatus.NEW_TASK, taskStatus);
        Assert.assertEquals("Expecting message type to be OcrWorker", "Worker", response.getMessageType());
        Assert.assertEquals("Check Api version matches", 1, response.getApiVersion());
        Assert.assertEquals("Expecting destination queue to be the same as that set in properties object", "GenericQueue", response.getQueueReference());

        SharedDocument task = (SharedDocument) response.getData();
        Assert.assertNotNull(task);
        Assert.assertEquals("Expecting source data reference to match ", testDocumentReference, task.getReference());
    }

    /**
     * Test to verify that the DocumentProcessingRecord is persisted from the Policy Worker Shared Document to the SharedDocument
     * @throws IOException
     */
    @Test
    public void testGenericHandlerAddsProcessingRecord() throws IOException {

        Document document = new DocumentImpl();
        String testDocumentReference = "test";
        document.setReference(testDocumentReference);

        Policy testPolicy = new Policy();
        testPolicy.id = 1L;
        ObjectMapper mapper = new ObjectMapper();
        testPolicy.details = mapper.readTree("{}");
        Long testColSeqId = 1L;

        com.github.cafdataprocessing.worker.policy.shared.Document policyWorkerDocument = new com.github.cafdataprocessing.worker.policy.shared.Document();
        DocumentProcessingRecord processingRecord = policyWorkerDocument.createPolicyDataProcessingRecord();
        //add some information onto processing record so we can verify it was persisted
        String addedFieldName = "CONTENT";
        String updatedFieldName = "TEST";
        String updatedFieldValue = "My expected value.";
        String updatedReferenceValue = "Updated reference";

        FieldChangeRecord updatedRecord = new FieldChangeRecord();
        updatedRecord.changeType = FieldChangeType.updated;
        FieldValue expectedFieldValue = new FieldValue();
        expectedFieldValue.valueEncoding = FieldEncoding.utf8;
        expectedFieldValue.value = updatedFieldValue;
        Collection<FieldValue> fieldValues = Arrays.asList(expectedFieldValue);
        updatedRecord.changeValues = fieldValues;

        FieldChangeRecord addedRecord = new FieldChangeRecord();
        addedRecord.changeType = FieldChangeType.added;
        Map<String,FieldChangeRecord> metadataChanges = new HashMap<>();
        metadataChanges.put(addedFieldName, addedRecord);
        metadataChanges.put(updatedFieldName, updatedRecord);
        processingRecord.metadataChanges = metadataChanges;

        FieldChangeRecord referenceRecord = new FieldChangeRecord();
        referenceRecord.changeType = FieldChangeType.updated;
        FieldValue expectedReferenceValue = new FieldValue();
        expectedReferenceValue.valueEncoding = FieldEncoding.utf8;
        expectedReferenceValue.value = updatedReferenceValue;
        Collection<FieldValue> referenceValues = Arrays.asList(expectedReferenceValue);
        referenceRecord.changeValues = referenceValues;
        processingRecord.referenceChange = referenceRecord;

        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        workerResponseHolder.getTaskData().setDocument(policyWorkerDocument);

        handler.handle(document, testPolicy, testColSeqId);
        workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        WorkerTaskResponsePolicyHandler.WorkerHandlerResponse response = workerResponseHolder.getChainWorkerResponse();
        Assert.assertNotNull(response);
        TaskStatus taskStatus = response.getTaskStatus();
        Assert.assertEquals("Check that worker response is NEW_TASK", TaskStatus.NEW_TASK, taskStatus);
        Assert.assertEquals("Expecting message type to be OcrWorker", "Worker", response.getMessageType());
        Assert.assertEquals("Check Api version matches", 1, response.getApiVersion());
        Assert.assertEquals("Expecting destination queue to be the same as that set in properties object", "GenericQueue", response.getQueueReference());

        SharedDocument task = (SharedDocument) response.getData();
        Assert.assertNotNull(task);
        Assert.assertEquals("Expecting source data reference to match ", testDocumentReference, task.getReference());

        DocumentProcessingRecord returnedRecord = task.getDocumentProcessingRecord();
        Assert.assertNotNull("Expecting DocumentProcessingRecord returned to not be null.", returnedRecord);
        Assert.assertEquals("Expecting reference change to be on the DocumentProcessingRecord.", referenceRecord, returnedRecord.referenceChange);
        Assert.assertEquals("Expecting metadata changes on SharedDocument to match those set on PolicyWorkerShared Document.",
                processingRecord.metadataChanges, returnedRecord.metadataChanges);
    }

    @Test
    public void testLoadPolicyDefFromFile() throws IOException {
        GenericQueueHandler handler = new GenericQueueHandler();
        PolicyType policyType = handler.getPolicyType();
        JsonNode policyDef = new ObjectMapper().readTree(this.getClass().getResource("/generic-queue-policy-definition.json"));
        Assert.assertEquals("Should have loaded policy definition from file", policyDef, policyType.definition);
    }
    
     
    @Test
    public void testGenericHandlerUsingGlobalEnvironment() throws IOException {

          // dont set task queue name is in the policy definition
        String taskQueueName = "test-this-env-queue-name";
        
        // ensure we set env back to what it original was, so using tmp changer class
        try (TemporaryEnvChanger valueChanger = new TemporaryEnvChanger("genericqueuehandler.taskqueue", taskQueueName)) {

            PolicyQueueDefinition policyQueueDefinition = new PolicyQueueDefinition();

            GenericQueueHandler textExtractWorkerHandler = new GenericQueueHandler();
            textExtractWorkerHandler.setApplicationContext(applicationContext);

            // used mocked props.
            GenericQueueHandlerProperties handlerProperties = getProperties();
            
            String queueName = textExtractWorkerHandler.getQueueName(policyQueueDefinition, handlerProperties);
            Assert.assertEquals("Check that the queue name returned is the task queue name set", taskQueueName, queueName);

        }
    }
    
    
    private static GenericQueueHandlerProperties getProperties()
    {
        AnnotationConfigApplicationContext propertiesApplicationContext = new AnnotationConfigApplicationContext();
        propertiesApplicationContext.register(PropertySourcesPlaceholderConfigurer.class);
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(GenericQueueHandlerProperties.class);
        propertiesApplicationContext.registerBeanDefinition("GenericQueueHandlerProperties", beanDefinition);
        propertiesApplicationContext.refresh();
        return propertiesApplicationContext.getBean(GenericQueueHandlerProperties.class);
    }

}

