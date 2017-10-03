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
import com.github.cafdataprocessing.worker.policy.handlers.binaryhash.BinaryHashWorkerHandler;
import com.github.cafdataprocessing.worker.policy.WorkerRequestHolder;
import com.github.cafdataprocessing.worker.policy.WorkerResponseHolder;
import com.github.cafdataprocessing.worker.policy.WorkerTaskResponsePolicyHandler;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.worker.binaryhash.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;

import java.io.IOException;

public class BinaryHashWorkerHandlerTest
{
    private CorePolicyApplicationContext applicationContext;
    private BinaryHashWorkerHandler handler;

    @Before
    public void SetupApplicationContext()
    {
        this.applicationContext = new CorePolicyApplicationContext();

        createBeanDefinition(WorkerResponseHolder.class, "WorkerResponseHolder", "thread");
        createBeanDefinition(WorkerRequestHolder.class, "WorkerRequestHolder", "thread");

        applicationContext.refresh();
        applicationContext.getBean(WorkerRequestHolder.class).clear();
        applicationContext.getBean(WorkerResponseHolder.class).setTaskData(new TaskData());
        handler = new BinaryHashWorkerHandler();
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
        document.getMetadata().put("storageReference", "testReference");

        Policy testPolicy = new Policy();
        testPolicy.id = 1L;

        ObjectMapper mapper = new ObjectMapper();
        testPolicy.details = mapper.readTree("{ \"queueName\": \"BinaryHashInput\" }");

        Long testColSeqId = 1L;

        WorkerResponseHolder responseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        responseHolder.setTaskData(new TaskData());

        handler.handle(document, testPolicy, testColSeqId);

        checkHandleWorkerResponse("testReference", "BinaryHashInput");
    }

    @Test
    public void checkForExceptionWithNoRef() throws IOException
    {
        Document document = new DocumentImpl();
        String testDocumentReference = "test";
        document.setReference(testDocumentReference);
        Policy testPolicy = new Policy();
        testPolicy.id = 1L;
        ObjectMapper mapper = new ObjectMapper();
        testPolicy.details = mapper.readTree("{\"queueName\":\"BinaryHashInput\"}");
        Long testColSeqId = 1L;
        WorkerResponseHolder responseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        responseHolder.setTaskData(new TaskData());
        boolean exceptionThrown = false;
        try {
            handler.handle(document, testPolicy, testColSeqId);
        } catch (RuntimeException e) {
            exceptionThrown = true;
        }
        Assert.assertTrue("Runtime should have thrown", exceptionThrown);
    }

    @Test
    public void testLoadPolicyDefFromFile() throws IOException
    {
        BinaryHashWorkerHandler localHandler = new BinaryHashWorkerHandler();
        PolicyType policyType = localHandler.getPolicyType();
        JsonNode policyDef = new ObjectMapper().readTree(this.getClass().getResource("/binaryhash-policy-definition.json"));
        Assert.assertEquals("Should have loaded policy definition from file", policyDef, policyType.definition);
    }

    private void checkHandleWorkerResponse(String srcDataReference, String policyDefQueueName)
    {
        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        WorkerTaskResponsePolicyHandler.WorkerHandlerResponse response = workerResponseHolder.getChainWorkerResponse();
        Assert.assertNotNull(response);
        TaskStatus taskStatus = response.getTaskStatus();

        Assert.assertEquals("Check that worker response is NEW_TASK", TaskStatus.NEW_TASK, taskStatus);
        Assert.assertEquals("Expecting message type to be BinaryHashWorker", BinaryHashWorkerConstants.WORKER_NAME, response.getMessageType());
        Assert.assertEquals("Expecting destination queue to be the same as that set in properties object", policyDefQueueName, response.getQueueReference());
        Assert.assertEquals("Check Api version matches", BinaryHashWorkerConstants.WORKER_API_VER, response.getApiVersion());

        BinaryHashWorkerTask task = (BinaryHashWorkerTask) response.getData();
        Assert.assertNotNull(task);
        Assert.assertEquals("Expecting source data reference to match ", srcDataReference, task.sourceData.getReference());
    }
}
