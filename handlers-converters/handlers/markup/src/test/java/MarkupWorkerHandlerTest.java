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
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.corepolicy.common.CorePolicyApplicationContext;
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.DocumentImpl;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import com.github.cafdataprocessing.worker.policy.handlers.markup.MarkupWorkerHandler;
import com.github.cafdataprocessing.worker.policy.WorkerRequestHolder;
import com.github.cafdataprocessing.worker.policy.WorkerResponseHolder;
import com.github.cafdataprocessing.worker.policy.WorkerTaskResponsePolicyHandler;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.markup.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class MarkupWorkerHandlerTest
{
    private CorePolicyApplicationContext applicationContext;
    private MarkupWorkerHandler handler;

    @Before
    public void SetupApplicationContext()
    {
        this.applicationContext = new CorePolicyApplicationContext();

        createBeanDefinition(WorkerResponseHolder.class, "WorkerResponseHolder", "thread");
        createBeanDefinition(WorkerRequestHolder.class, "WorkerRequestHolder", "thread");

        applicationContext.refresh();
        applicationContext.getBean(WorkerRequestHolder.class).clear();
        applicationContext.getBean(WorkerResponseHolder.class).setTaskData(new TaskData());
        handler = new MarkupWorkerHandler();
        handler.setApplicationContext(applicationContext);
    }

    private void createBeanDefinition(Class<?> beanClass, String beanClassName, String beanScope)
    {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setScope(beanScope);
        beanDefinition.setBeanClass(beanClass);

        applicationContext.registerBeanDefinition(beanClassName, beanDefinition);
    }

    @Test
    public void testWorkerHandler() throws IOException
    {
        Document document = new DocumentImpl();
        String testDocumentReference = "test";
        document.setReference(testDocumentReference);
        document.getMetadata().put("metadataReferences", "{CONTENT=[com.hpe.caf.util.ref.ReferencedData@4fae403]}");
        document.getMetadata().put("internetmessageid", "<F9118B65E25E54499F4688F179E8F5A620BB2F68@HPE.COM>");
        document.getMetadata().put("conversationtopic", "Hewlett Packard Enterprise reports Q1 FY16 results");
        document.getMetadata().put("caf-mail-conversation-index", "Ac3pCr/g148OQoCCQSCy8dDjwH7QBwAAzLowAAARRGA=");
        document.getMetadata().put("caf-mail-in-reply-to", "<TU4PR84MB0126244971A519B8281F5398EFE60@HPE.COM>");
        document.getMetadata().put("storageReference", "testReference");

        Policy testPolicy = new Policy();
        testPolicy.id = 1L;

        ObjectMapper mapper = new ObjectMapper();
        testPolicy.details = mapper.readTree("{ \"queueName\": \"MarkupInput\", \"hashConfiguration\": [{ \"name\": \"NORMALIZED_HASH\", \"scope\": \"EMAIL_SPECIFIC\", \"fields\": [{ \"name\": \"to\", \"normalizationType\": \"NAME_ONLY\" },{ \"name\": \"from\", \"normalizationType\": \"NAME_ONLY\" }, { \"name\": \"body\", \"normalizationType\": \"REMOVE_WHITESPACE_AND_LINKS\" }, { \"name\": \"CHILD_INFO_*_HASH\", \"normalizationType\": \"NAME_ONLY\" }], \"hashFunctions\": [\"XXHASH64\"] }, { \"name\": \"VARIANT_HASH\", \"scope\": \"EMAIL_THREAD\", \"fields\": [{ \"name\": \"body\", \"normalizationType\": \"REMOVE_WHITESPACE_AND_LINKS\" }], \"hashFunctions\": [\"XXHASH64\"] }], \"outputFields\": [{ \"field\": \"SECTION_ID\", \"xPathExpression\": \"/root/email[1]/hash/digest/@value\" }, { \"field\": \"PARENT_ID\", \"xPathExpression\": \"/root/email[2]/hash/digest/@value\" }, { \"field\": \"ROOT_ID\", \"xPathExpression\": \"/root/email[last()]/hash/digest/@value\" }, { \"field\": \"SECTION_SORT\", \"xPathExpression\": \"/root/email[1]/headers/Sent/text()\" }, { \"field\": \"MESSAGE_ID\", \"xPathExpression\": \"/root/CAF_MAIL_MESSAGE_ID/text()\" }, { \"field\": \"CONVERSATION_TOPIC\", \"xPathExpression\": \"/root/CAF_MAIL_CONVERSATION_TOPIC/text()\" }, { \"field\": \"CONVERSATION_INDEX_JSON\", \"xPathExpression\": \"/root/CAF_MAIL_CONVERSATION_INDEX_PARSED/text()\" }, { \"field\": \"IN_REPLY_TO\", \"xPathExpression\": \"/root/CAF_MAIL_IN_REPLY_TO/text()\" }], \"isEmail\": true }");

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
        Assert.assertEquals("Expecting message type to be MarkerWorker", MarkupWorkerConstants.WORKER_NAME, response.getMessageType());
        Assert.assertEquals("Expecting destination queue to be the same as that set in properties object", "MarkupInput", response.getQueueReference());
        Assert.assertEquals("Check Api version matches", MarkupWorkerConstants.WORKER_API_VER, response.getApiVersion());

        MarkupWorkerTask task = (MarkupWorkerTask) response.getData();
        Assert.assertNotNull(task);

        Multimap<String, ReferencedData> sourceData = task.sourceData;
        Assert.assertNotNull(sourceData);
        Assert.assertTrue(sourceData.containsKey("metadataReferences"));
        Assert.assertTrue(sourceData.containsKey("internetmessageid"));
        Assert.assertTrue(sourceData.containsKey("conversationtopic"));
        Assert.assertTrue(sourceData.containsKey("caf-mail-conversation-index"));
        Assert.assertTrue(sourceData.containsKey("caf-mail-in-reply-to"));

        List<HashConfiguration> hashConfiguration = task.hashConfiguration;
        Assert.assertNotNull(hashConfiguration);
        Assert.assertTrue(hashConfiguration.size() == 2);
        Assert.assertTrue(hashConfiguration.get(0).name.equals("NORMALIZED_HASH"));
        Assert.assertTrue(hashConfiguration.get(0).scope.equals(Scope.EMAIL_SPECIFIC));
        Assert.assertTrue(hashConfiguration.get(0).fields.size() == 4);
        Assert.assertTrue(hashConfiguration.get(0).hashFunctions.size() == 1);
        Assert.assertTrue(hashConfiguration.get(0).fields.get(0).name.equals("to"));
        Assert.assertTrue(hashConfiguration.get(0).fields.get(0).normalizationType.equals(NormalizationType.NAME_ONLY));
        Assert.assertTrue(hashConfiguration.get(0).fields.get(1).name.equals("from"));
        Assert.assertTrue(hashConfiguration.get(0).fields.get(1).normalizationType.equals(NormalizationType.NAME_ONLY));
        Assert.assertTrue(hashConfiguration.get(0).fields.get(2).name.equals("body"));
        Assert.assertTrue(hashConfiguration.get(0).fields.get(2).normalizationType.equals(NormalizationType.REMOVE_WHITESPACE_AND_LINKS));
        Assert.assertTrue(hashConfiguration.get(0).fields.get(3).name.equals("CHILD_INFO_*_HASH"));
        Assert.assertTrue(hashConfiguration.get(0).fields.get(3).normalizationType.equals(NormalizationType.NAME_ONLY));
        Assert.assertTrue(hashConfiguration.get(1).name.equals("VARIANT_HASH"));
        Assert.assertTrue(hashConfiguration.get(1).scope.equals(Scope.EMAIL_THREAD));
        Assert.assertTrue(hashConfiguration.get(1).fields.size() == 1);
        Assert.assertTrue(hashConfiguration.get(1).hashFunctions.size() == 1);
        Assert.assertTrue(hashConfiguration.get(1).fields.get(0).name.equals("body"));
        Assert.assertTrue(hashConfiguration.get(1).fields.get(0).normalizationType.equals(NormalizationType.REMOVE_WHITESPACE_AND_LINKS));

        List<OutputField> outputFields = task.outputFields;
        Assert.assertNotNull(outputFields);
        Assert.assertTrue(outputFields.size() == 8);
        Assert.assertTrue(outputFields.get(0).field.equals("SECTION_ID"));
        Assert.assertTrue(outputFields.get(0).xPathExpression.equals("/root/email[1]/hash/digest/@value"));
        Assert.assertTrue(outputFields.get(1).field.equals("PARENT_ID"));
        Assert.assertTrue(outputFields.get(1).xPathExpression.equals("/root/email[2]/hash/digest/@value"));
        Assert.assertTrue(outputFields.get(2).field.equals("ROOT_ID"));
        Assert.assertTrue(outputFields.get(2).xPathExpression.equals("/root/email[last()]/hash/digest/@value"));
        Assert.assertTrue(outputFields.get(3).field.equals("SECTION_SORT"));
        Assert.assertTrue(outputFields.get(3).xPathExpression.equals("/root/email[1]/headers/Sent/text()"));
        Assert.assertTrue(outputFields.get(4).field.equals("MESSAGE_ID"));
        Assert.assertTrue(outputFields.get(4).xPathExpression.equals("/root/CAF_MAIL_MESSAGE_ID/text()"));
        Assert.assertTrue(outputFields.get(5).field.equals("CONVERSATION_TOPIC"));
        Assert.assertTrue(outputFields.get(5).xPathExpression.equals("/root/CAF_MAIL_CONVERSATION_TOPIC/text()"));
        Assert.assertTrue(outputFields.get(6).field.equals("CONVERSATION_INDEX_JSON"));
        Assert.assertTrue(outputFields.get(6).xPathExpression.equals("/root/CAF_MAIL_CONVERSATION_INDEX_PARSED/text()"));
        Assert.assertTrue(outputFields.get(7).field.equals("IN_REPLY_TO"));
        Assert.assertTrue(outputFields.get(7).xPathExpression.equals("/root/CAF_MAIL_IN_REPLY_TO/text()"));

        boolean isEmail = task.isEmail;
        Assert.assertTrue(isEmail);
    }

    @Test
    public void testWorkerHandler_FieldFilter() throws IOException
    {
        Document document = new DocumentImpl();
        String testDocumentReference = "test";
        document.setReference(testDocumentReference);
        document.getMetadata().put("metadataReferences", "{CONTENT=[com.hpe.caf.util.ref.ReferencedData@4fae403]}");
        document.getMetadata().put("internetmessageid", "<F9118B65E25E54499F4688F179E8F5A620BB2F68@HPE.COM>");
        document.getMetadata().put("conversationtopic", "Hewlett Packard Enterprise reports Q1 FY16 results");
        document.getMetadata().put("caf-mail-conversation-index", "Ac3pCr/g148OQoCCQSCy8dDjwH7QBwAAzLowAAARRGA=");
        document.getMetadata().put("caf-mail-in-reply-to", "<TU4PR84MB0126244971A519B8281F5398EFE60@HPE.COM>");

        String childInfo1HashValue = UUID.randomUUID().toString();
        document.getMetadata().put("CHILD_.1_INFO_.1_HASH.1", childInfo1HashValue);
        String childInfo2HashValue = UUID.randomUUID().toString();
        document.getMetadata().put("ChIlD_.1_InFo_.2_HaSh.1", childInfo2HashValue);

        Policy testPolicy = new Policy();
        testPolicy.id = 1L;

        ObjectMapper mapper = new ObjectMapper();
        testPolicy.details = mapper.readTree("{\"queueName\": \"MarkupInput\",\"fields\": [\"internetmessageid\", \"CHILD_.*_INFO_.*_HASH.*\"], \"isEmail\": true }");

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
        Assert.assertEquals("Expecting message type to be MarkerWorker", MarkupWorkerConstants.WORKER_NAME, response.getMessageType());
        Assert.assertEquals("Expecting destination queue to be the same as that set in properties object", "MarkupInput", response.getQueueReference());
        Assert.assertEquals("Check Api version matches", MarkupWorkerConstants.WORKER_API_VER, response.getApiVersion());

        MarkupWorkerTask task = (MarkupWorkerTask) response.getData();
        Assert.assertNotNull(task);

        Multimap<String, ReferencedData> sourceData = task.sourceData;
        Assert.assertNotNull(sourceData);
        Assert.assertTrue(!sourceData.containsKey("metadataReferences"));
        Assert.assertTrue(sourceData.containsKey("internetmessageid"));
        Assert.assertTrue(!sourceData.containsKey("conversationtopic"));
        Assert.assertTrue(!sourceData.containsKey("caf-mail-conversation-index"));
        Assert.assertTrue(!sourceData.containsKey("caf-mail-in-reply-to"));

        Assert.assertTrue(sourceData.containsKey("CHILD_.1_INFO_.1_HASH.1"));
        Assert.assertTrue(sourceData.containsKey("ChIlD_.1_InFo_.2_HaSh.1"));

        boolean isEmail = task.isEmail;
        Assert.assertTrue(isEmail);
    }

    @Test
    public void testLoadPolicyDefFromFile() throws IOException
    {
        MarkupWorkerHandler localHandler = new MarkupWorkerHandler();
        PolicyType policyType = localHandler.getPolicyType();
        JsonNode policyDef = new ObjectMapper().readTree(this.getClass().getResource("/markup-policy-definition.json"));
        Assert.assertEquals("Should have loaded policy definition from file", policyDef, policyType.definition);
    }
}
