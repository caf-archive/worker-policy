package com.github.cafdataprocessing.worker.policy.composite.document.worker.handler;

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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cafdataprocessing.corepolicy.common.CorePolicyApplicationContext;
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.DocumentImpl;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import com.github.cafdataprocessing.worker.policy.WorkerRequestHolder;
import com.github.cafdataprocessing.worker.policy.WorkerResponseHolder;
import com.github.cafdataprocessing.worker.policy.WorkerTaskResponsePolicyHandler;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.document.DocumentWorkerConstants;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerDocumentTask;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;

public class CompositeDocumentWorkerHandlerTest
{

    private CorePolicyApplicationContext applicationContext;
    private CompositeDocumentWorkerHandler handler;

    @Before
    public void setupApplicationContext()
    {
        this.applicationContext = new CorePolicyApplicationContext();

        createBeanDefinition(WorkerResponseHolder.class, "WorkerResponseHolder", "thread");
        createBeanDefinition(WorkerRequestHolder.class, "WorkerRequestHolder", "thread");

        applicationContext.refresh();
        applicationContext.getBean(WorkerRequestHolder.class).clear();
        applicationContext.getBean(WorkerResponseHolder.class).setTaskData(new TaskData());
        handler = new CompositeDocumentWorkerHandler();
        handler.setApplicationContext(applicationContext);
    }

    private void createBeanDefinition(Class<?> beanClass, String beanClassName, String beanScope)
    {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setScope(beanScope);
        beanDefinition.setBeanClass(beanClass);

        applicationContext.registerBeanDefinition(beanClassName, beanDefinition);
    }

    private Document setupDocument(final boolean generateChildren)
    {
        Document document = new DocumentImpl();
        String testDocumentReference = "test";
        document.setReference(testDocumentReference);
        document.getMetadata().put("metadataReferences", "{CONTENT=[com.hpe.caf.util.ref.ReferencedData@4fae403]}");
        document.getMetadata().put("internetmessageid", "<F9118B65E25E54499F4688F179E8F5A620BB2F68@HPE.COM>");
        document.getMetadata().put("conversationtopic", "Hewlett Packard Enterprise reports Q1 FY16 results");
        document.getMetadata().put("caf-mail-conversation-index", "Ac3pCr/g148OQoCCQSCy8dDjwH7QBwAAzLowAAARRGA=");
        document.getMetadata().put("caf-mail-in-reply-to", "<TU4PR84MB0126244971A519B8281F5398EFE60@HPE.COM>");
        document.getMetadata().put("TestWithNullValue", null);

        String childInfo1HashValue = UUID.randomUUID().toString();
        document.getMetadata().put("CHILD_1_INFO_1", childInfo1HashValue);
        String childInfo2HashValue = UUID.randomUUID().toString();
        document.getMetadata().put("CHILD_1_INFO_2", childInfo2HashValue);

        if(generateChildren) {
            for (int i = 0; i < 3; i++) {
                document.getDocuments().add(setupDocument(false));
            }
        }
        return document;
    }

    private Document setupDocument(Multimap<String, ReferencedData> metadataReferences)
    {
        Document document = new DocumentImpl();
        String testDocumentReference = "test";
        document.setReference(testDocumentReference);

        //set up document with metadata
        Multimap<String, String> testFields = HashMultimap.create();
        testFields.put("DOC_FIELD_NAME_CONTENT", "value");
        testFields.put("DOC_FIELD_NAME_1", "value1");
        testFields.put("DOC_FIELD_NAME_2", "value2");
        testFields.put("DOC_FIELD_NAME_3", "value3");
        document.getMetadata().putAll(testFields);

        //set up document with metadata references
        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        com.github.cafdataprocessing.worker.policy.shared.Document taskDataDocument
            = new com.github.cafdataprocessing.worker.policy.shared.Document();

        taskDataDocument.setMetadataReferences(metadataReferences);
        TaskData testTaskData = new TaskData();
        testTaskData.setDocument(taskDataDocument);
        testTaskData.setOutputPartialReference(UUID.randomUUID().toString());
        workerResponseHolder.setTaskData(testTaskData);

        return document;
    }

    @Test
    public void testWorkerHandler() throws IOException
    {
        Document document = setupDocument(false);

        Policy testPolicy = new Policy();
        testPolicy.id = 1L;

        ObjectMapper mapper = new ObjectMapper();
        testPolicy.details = mapper.readTree("{ \"workerName\": \"DocWorkerName\", \"queueName\": \"DocumentInput\"}");

        Long testColSeqId = 1L;

        WorkerResponseHolder responseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        responseHolder.setTaskData(new TaskData());
        responseHolder.getTaskData().setOutputPartialReference(UUID.randomUUID().toString());

        handler.handle(document, testPolicy, testColSeqId);

        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        WorkerTaskResponsePolicyHandler.WorkerHandlerResponse response = workerResponseHolder.getChainWorkerResponse();
        Assert.assertNotNull(response);
        TaskStatus taskStatus = response.getTaskStatus();

        Assert.assertEquals("Check that worker response is NEW_TASK", TaskStatus.NEW_TASK, taskStatus);
        Assert.assertEquals("Expecting message type to be DocumentWorker", DocumentWorkerConstants.WORKER_NAME, response.getMessageType());
        Assert.assertEquals("Expecting destination queue to be the same as that set in properties object", "DocumentInput", response.getQueueReference());
        Assert.assertEquals("Check Api version matches", DocumentWorkerConstants.WORKER_API_VER, response.getApiVersion());

        DocumentWorkerDocumentTask task = (DocumentWorkerDocumentTask) response.getData();
        Assert.assertNotNull(task);

        Map<String, List<DocumentWorkerFieldValue>> taskFields = task.document.fields;
        Assert.assertNotNull(taskFields);

        Assert.assertTrue(taskFields.containsKey("metadataReferences"));
        Assert.assertTrue(taskFields.containsKey("internetmessageid"));
        Assert.assertTrue(taskFields.containsKey("conversationtopic"));
        Assert.assertTrue(taskFields.containsKey("caf-mail-conversation-index"));
        Assert.assertTrue(taskFields.containsKey("caf-mail-in-reply-to"));
        Assert.assertTrue(taskFields.containsKey("TestWithNullValue"));
    }

    @Test
    public void testWorkerHandlerForSubfiles() throws IOException
    {
        Document document = setupDocument(true);

        Policy testPolicy = new Policy();
        testPolicy.id = 1L;

        ObjectMapper mapper = new ObjectMapper();
        testPolicy.details = mapper.readTree("{ \"workerName\": \"DocWorkerName\", \"queueName\": \"DocumentInput\"}");

        Long testColSeqId = 1L;

        WorkerResponseHolder responseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        responseHolder.setTaskData(new TaskData());
        responseHolder.getTaskData().setOutputPartialReference(UUID.randomUUID().toString());

        handler.handle(document, testPolicy, testColSeqId);

        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        WorkerTaskResponsePolicyHandler.WorkerHandlerResponse response = workerResponseHolder.getChainWorkerResponse();
        Assert.assertNotNull(response);
        TaskStatus taskStatus = response.getTaskStatus();

        Assert.assertEquals("Check that worker response is NEW_TASK", TaskStatus.NEW_TASK, taskStatus);
        Assert.assertEquals("Expecting message type to be DocumentWorker", DocumentWorkerConstants.WORKER_NAME, response.getMessageType());
        Assert.assertEquals("Expecting destination queue to be the same as that set in properties object", "DocumentInput", response.getQueueReference());
        Assert.assertEquals("Check Api version matches", DocumentWorkerConstants.WORKER_API_VER, response.getApiVersion());

        DocumentWorkerDocumentTask task = (DocumentWorkerDocumentTask) response.getData();
        Assert.assertNotNull(task);
        DocumentWorkerDocument documentResponse = task.document;
        for (DocumentWorkerDocument doc : documentResponse.subdocuments) {
            Map<String, List<DocumentWorkerFieldValue>> taskFields = doc.fields;
            Assert.assertNotNull(taskFields);

            Assert.assertTrue(taskFields.containsKey("metadataReferences"));
            Assert.assertTrue(taskFields.containsKey("internetmessageid"));
            Assert.assertTrue(taskFields.containsKey("conversationtopic"));
            Assert.assertTrue(taskFields.containsKey("caf-mail-conversation-index"));
            Assert.assertTrue(taskFields.containsKey("caf-mail-in-reply-to"));
            Assert.assertTrue(taskFields.containsKey("TestWithNullValue"));
        }
    }

    @Test
    public void testCustomDataPartialReferenceField() throws IOException
    {
        Document document = setupDocument(false);

        Policy testPolicy = new Policy();
        testPolicy.id = 1L;

        ObjectMapper mapper = new ObjectMapper();
        testPolicy.details = mapper.readTree("{ \"workerName\": \"DocWorkerName\", \"customData\": {\"outputPartialReference\": {\"source\": \"dataStorePartialReference\"}}}");

        final Long testColSeqId = 1L;
        final String outputPartialRef = UUID.randomUUID().toString();

        WorkerResponseHolder responseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        responseHolder.setTaskData(new TaskData());
        responseHolder.getTaskData().setOutputPartialReference(outputPartialRef);

        handler.handle(document, testPolicy, testColSeqId);

        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        WorkerTaskResponsePolicyHandler.WorkerHandlerResponse response = workerResponseHolder.getChainWorkerResponse();
        Assert.assertNotNull(response);

        DocumentWorkerDocumentTask task = (DocumentWorkerDocumentTask) response.getData();
        Assert.assertNotNull(task);

        Assert.assertTrue(task.customData.containsValue(outputPartialRef));
    }

    @Test
    public void testCustomDataProjectIdField() throws IOException
    {
        Document document = setupDocument(false);

        Policy testPolicy = new Policy();
        testPolicy.id = 1L;

        ObjectMapper mapper = new ObjectMapper();
        testPolicy.details = mapper.readTree("{ \"workerName\": \"DocWorkerName\", \"customData\": {\"tenantId\": {\"source\": \"projectId\"}}}");

        final Long testColSeqId = 1L;
        final String outputTenantId = UUID.randomUUID().toString();

        WorkerResponseHolder responseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        responseHolder.setTaskData(new TaskData());
        responseHolder.getTaskData().setProjectId(outputTenantId);

        handler.handle(document, testPolicy, testColSeqId);

        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        WorkerTaskResponsePolicyHandler.WorkerHandlerResponse response = workerResponseHolder.getChainWorkerResponse();
        Assert.assertNotNull(response);

        DocumentWorkerDocumentTask task = (DocumentWorkerDocumentTask) response.getData();
        Assert.assertNotNull(task);

        Assert.assertTrue(task.customData.containsValue(outputTenantId));
    }

    @Test
    public void testCustomDataEscapedJsonObjectField() throws IOException
    {
        Document document = setupDocument(false);

        Policy testPolicy = new Policy();
        testPolicy.id = 1L;

        ObjectMapper mapper = new ObjectMapper();
        testPolicy.details = mapper.readTree(
            "{\n"
            + "  \"workerName\": \"DocWorkerName\",\n"
            + "  \"customData\": {\n"
            + "    \"someSetting\": \"{\\\"object\\\":{\\\"key\\\":\\\"value\\\",\\\"array\\\":[{\\\"null_value\\\":null},{\\\"boolean\\\":true},{\\\"integer\\\":1}]}}\"\n"
            + "  }\n"
            + "}");

        final Long testColSeqId = 1L;

        WorkerResponseHolder responseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        responseHolder.setTaskData(new TaskData());

        handler.handle(document, testPolicy, testColSeqId);

        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        WorkerTaskResponsePolicyHandler.WorkerHandlerResponse response = workerResponseHolder.getChainWorkerResponse();
        Assert.assertNotNull(response);

        DocumentWorkerDocumentTask task = (DocumentWorkerDocumentTask) response.getData();
        Assert.assertNotNull(task);

        Assert.assertTrue(task.customData.containsKey("someSetting"));
        final String escapedJsonObjectValue = task.customData.get("someSetting");
        Assert.assertEquals("{\"object\":{\"key\":\"value\",\"array\":[{\"null_value\":null},{\"boolean\":true},{\"integer\":1}]}}", escapedJsonObjectValue);
    }

    @Test
    public void testCustomDataJsonObjectField() throws IOException
    {
        Document document = setupDocument(false);

        Policy testPolicy = new Policy();
        testPolicy.id = 1L;

        ObjectMapper mapper = new ObjectMapper();
        testPolicy.details = mapper.readTree(
            "{\n"
            + "  \"workerName\": \"DocWorkerName\",\n"
            + "  \"customData\": {  \n"
            + "    \"objectSetting\": {  \n"
            + "      \"source\": \"inlineJson\",\n"
            + "      \"data\": {  \n"
            + "        \"object\": {  \n"
            + "          \"key\":\"value\",\n"
            + "          \"array\": [  \n"
            + "            {  \n"
            + "              \"null_value\": null\n"
            + "            },\n"
            + "            {  \n"
            + "              \"boolean\": true\n"
            + "            },\n"
            + "            {  \n"
            + "              \"integer\": 1\n"
            + "            }\n"
            + "          ]\n"
            + "        }\n"
            + "      }\n"
            + "    },\n"
            + "    \"arraySetting\": {  \n"
            + "      \"source\": \"inlineJson\",\n"
            + "      \"data\": [\n"
            + "        {\n"
            + "          \"boolean\": false\n"
            + "        },\n"
            + "        {\n"
            + "          \"integer\": 42\n"
            + "        },\n"
            + "        {\n"
            + "          \"string_value\": \"hello\"\n"
            + "        }\n"
            + "      ]\n"
            + "    }\n"
            + "  }\n"
            + "}");

        final Long testColSeqId = 1L;

        WorkerResponseHolder responseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        responseHolder.setTaskData(new TaskData());

        handler.handle(document, testPolicy, testColSeqId);

        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        WorkerTaskResponsePolicyHandler.WorkerHandlerResponse response = workerResponseHolder.getChainWorkerResponse();
        Assert.assertNotNull(response);

        DocumentWorkerDocumentTask task = (DocumentWorkerDocumentTask) response.getData();
        Assert.assertNotNull(task);

        Assert.assertTrue(task.customData.containsKey("objectSetting"));
        final String escapedJsonObjectValue = task.customData.get("objectSetting");
        Assert.assertEquals("{\"object\":{\"key\":\"value\",\"array\":[{\"null_value\":null},{\"boolean\":true},{\"integer\":1}]}}", escapedJsonObjectValue);

        Assert.assertTrue(task.customData.containsKey("arraySetting"));
        final String escapedJsonArrayValue = task.customData.get("arraySetting");
        Assert.assertEquals("[{\"boolean\":false},{\"integer\":42},{\"string_value\":\"hello\"}]", escapedJsonArrayValue);
    }

    @Test
    public void testCustomDataFieldEmpty() throws IOException
    {
        Document document = setupDocument(false);

        Policy testPolicy = new Policy();
        testPolicy.id = 1L;

        ObjectMapper mapper = new ObjectMapper();
        testPolicy.details = mapper.readTree("{ \"workerName\": \"DocWorkerName\", \"customData\": {} }");

        final Long testColSeqId = 1L;
        final String outputPartialRef = UUID.randomUUID().toString();

        WorkerResponseHolder responseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        responseHolder.setTaskData(new TaskData());
        responseHolder.getTaskData().setOutputPartialReference(outputPartialRef);

        handler.handle(document, testPolicy, testColSeqId);

        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        WorkerTaskResponsePolicyHandler.WorkerHandlerResponse response = workerResponseHolder.getChainWorkerResponse();
        Assert.assertNotNull(response);

        DocumentWorkerDocumentTask task = (DocumentWorkerDocumentTask) response.getData();
        Assert.assertNotNull(task);

        Assert.assertNull(task.customData);
    }

    @Test
    public void testCustomDataFieldMissing() throws IOException
    {
        Document document = setupDocument(false);

        Policy testPolicy = new Policy();
        testPolicy.id = 1L;

        ObjectMapper mapper = new ObjectMapper();
        testPolicy.details = mapper.readTree("{ \"workerName\": \"DocWorkerName\" }");

        final Long testColSeqId = 1L;
        final String outputPartialRef = UUID.randomUUID().toString();

        WorkerResponseHolder responseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        responseHolder.setTaskData(new TaskData());
        responseHolder.getTaskData().setOutputPartialReference(outputPartialRef);

        handler.handle(document, testPolicy, testColSeqId);

        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        WorkerTaskResponsePolicyHandler.WorkerHandlerResponse response = workerResponseHolder.getChainWorkerResponse();
        Assert.assertNotNull(response);

        DocumentWorkerDocumentTask task = (DocumentWorkerDocumentTask) response.getData();
        Assert.assertNotNull(task);

        Assert.assertNull(task.customData);
    }

    @Test
    public void testWorkerHandlerFieldFilter() throws IOException
    {
        Document document = setupDocument(false);

        Policy testPolicy = new Policy();
        testPolicy.id = 1L;

        ObjectMapper mapper = new ObjectMapper();
        testPolicy.details = mapper.readTree("{ \"workerName\": \"DocWorkerName\", \"fields\": [\"CHILD_*_INFO_*\", \"internetmessageid\", \"conversationtopic\", \"TestWithNullValue\"]}");

        Long testColSeqId = 1L;

        WorkerResponseHolder responseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        responseHolder.setTaskData(new TaskData());
        responseHolder.getTaskData().setOutputPartialReference(UUID.randomUUID().toString());

        handler.handle(document, testPolicy, testColSeqId);

        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        WorkerTaskResponsePolicyHandler.WorkerHandlerResponse response = workerResponseHolder.getChainWorkerResponse();
        Assert.assertNotNull(response);
        TaskStatus taskStatus = response.getTaskStatus();

        Assert.assertEquals("Check that worker response is NEW_TASK", TaskStatus.NEW_TASK, taskStatus);
        Assert.assertEquals("Expecting message type to be DocumentWorker", DocumentWorkerConstants.WORKER_NAME, response.getMessageType());
        Assert.assertEquals("Expecting destination queue to be the same as that set in properties object", "DocWorkerNameInput", response.getQueueReference());
        Assert.assertEquals("Check Api version matches", DocumentWorkerConstants.WORKER_API_VER, response.getApiVersion());

        DocumentWorkerDocumentTask task = (DocumentWorkerDocumentTask) response.getData();
        Assert.assertNotNull(task);

        Map<String, List<DocumentWorkerFieldValue>> taskFields = task.document.fields;
        Assert.assertNotNull(taskFields);

        Assert.assertTrue(!taskFields.containsKey("metadataReferences"));
        Assert.assertTrue(!taskFields.containsKey("caf-mail-conversation-index"));
        Assert.assertTrue(!taskFields.containsKey("caf-mail-in-reply-to"));
        Assert.assertTrue(taskFields.containsKey("TestWithNullValue"));
        Assert.assertTrue(taskFields.containsKey("internetmessageid"));
        Assert.assertTrue(taskFields.containsKey("conversationtopic"));

        Assert.assertTrue(taskFields.containsKey("CHILD_1_INFO_1"));
        Assert.assertTrue(taskFields.containsKey("CHILD_1_INFO_2"));

        List<DocumentWorkerFieldValue> workerDataList_conversationtopic = taskFields.get("conversationtopic");
        Assert.assertTrue(workerDataList_conversationtopic.size() == 1);
        for (DocumentWorkerFieldValue data : workerDataList_conversationtopic) {
            Assert.assertTrue(data.encoding == null);
            Assert.assertEquals(data.data, "Hewlett Packard Enterprise reports Q1 FY16 results");
        }
    }

    @Test
    public void testMetadataReferences() throws IOException
    {
        Policy testPolicy = new Policy();
        testPolicy.id = 1L;

        String customKey1 = "Joe";
        String customKey2 = "Bloggs";
        String customValue = "Test";

        ObjectMapper mapper = new ObjectMapper();
        testPolicy.details = mapper.readTree("{ \"workerName\": \"DocWorkerName\", \"customData\":{\"" + customKey1 + "\":\"" + customValue + "\",\"" + customKey2 + "\":\"" + customValue + "\"}, \"fields\": [\"DOC_REF_FIELD_NAME_1\", \"DOC_REF_FIELD_NAME_2\", \"TEST_WITH_NULL_VALUES\", \"DOC_REF_FIELD_DREDBNAME\", \"DOC_FIELD_NAME_1\"]}");

        Multimap<String, ReferencedData> metadataReferences = ArrayListMultimap.create();
        metadataReferences.put("DOC_REF_FIELD_NAME_1", ReferencedData.getWrappedData("refValue1".getBytes()));
        metadataReferences.put("DOC_REF_FIELD_NAME_1", ReferencedData.getWrappedData("refValue1".getBytes()));
        metadataReferences.put("DOC_REF_FIELD_NAME_2", ReferencedData.getWrappedData("refValue2".getBytes()));
        metadataReferences.put("DOC_REF_FIELD_DREDBNAME", ReferencedData.getReferencedData("testReference"));
        metadataReferences.put("TEST_WITH_NULL_VALUES", ReferencedData.getWrappedData(null));
        Document document = setupDocument(metadataReferences);

        Long testColSeqId = 1L;
        handler.handle(document, testPolicy, testColSeqId);

        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        WorkerTaskResponsePolicyHandler.WorkerHandlerResponse response = workerResponseHolder.getChainWorkerResponse();
        Assert.assertNotNull(response);

        DocumentWorkerDocumentTask task = (DocumentWorkerDocumentTask) response.getData();
        Assert.assertNotNull(task);

        Map<String, List<DocumentWorkerFieldValue>> taskFields = task.document.fields;
        Assert.assertNotNull(taskFields);

        Assert.assertTrue(taskFields.containsKey("DOC_REF_FIELD_NAME_1"));
        Assert.assertTrue(taskFields.containsKey("DOC_REF_FIELD_NAME_2"));
        Assert.assertTrue(taskFields.containsKey("TEST_WITH_NULL_VALUES"));
        Assert.assertTrue(taskFields.containsKey("DOC_REF_FIELD_DREDBNAME"));
        Assert.assertTrue(taskFields.containsKey("DOC_FIELD_NAME_1"));

        List<DocumentWorkerFieldValue> workerDataList_field_name_1 = taskFields.get("DOC_REF_FIELD_NAME_1");
        Assert.assertTrue(workerDataList_field_name_1.size() == 2);
        for (DocumentWorkerFieldValue data : workerDataList_field_name_1) {
            Assert.assertTrue(data.encoding == DocumentWorkerFieldEncoding.base64);
            Assert.assertEquals(new String(parseBase64Binary(data.data), StandardCharsets.UTF_8), "refValue1");
        }

        List<DocumentWorkerFieldValue> workerDataList_DOC_REF_FIELD_DREDBNAME = taskFields.get("DOC_REF_FIELD_DREDBNAME");
        Assert.assertTrue(workerDataList_DOC_REF_FIELD_DREDBNAME.size() == 1);
        for (DocumentWorkerFieldValue data : workerDataList_DOC_REF_FIELD_DREDBNAME) {
            Assert.assertTrue(data.encoding == DocumentWorkerFieldEncoding.storage_ref);
            Assert.assertEquals(data.data, "testReference");
        }

        List<DocumentWorkerFieldValue> workerDataList_TEST_WITH_NULL_VALUES = taskFields.get("TEST_WITH_NULL_VALUES");
        Assert.assertTrue(workerDataList_TEST_WITH_NULL_VALUES.size() == 1);
        for (DocumentWorkerFieldValue data : workerDataList_TEST_WITH_NULL_VALUES) {
            Assert.assertTrue(data.encoding == DocumentWorkerFieldEncoding.base64);
            Assert.assertEquals(data.data, null);
        }

        Map<String, String> map = new HashMap<>();
        map.put(customKey1, customValue);
        map.put(customKey2, customValue);
        Assert.assertEquals(map, task.customData);

    }

    @Test
    public void testLoadPolicyDefFromFile() throws IOException
    {
        CompositeDocumentWorkerHandler localHandler = new CompositeDocumentWorkerHandler();
        PolicyType policyType = localHandler.getPolicyType();
        JsonNode policyDef = new ObjectMapper().readTree(this.getClass().getResource("/composite-document-policy-definition.json"));
        Assert.assertEquals("Should have loaded policy definition from file", policyDef, policyType.definition);
    }
}
