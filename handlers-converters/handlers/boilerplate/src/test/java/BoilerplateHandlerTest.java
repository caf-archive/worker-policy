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
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.github.cafdataprocessing.corepolicy.common.CorePolicyApplicationContext;
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.DocumentImpl;
import com.github.cafdataprocessing.corepolicy.common.UserContext;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import com.github.cafdataprocessing.worker.policy.WorkerRequestHolder;
import com.github.cafdataprocessing.worker.policy.WorkerResponseHolder;
import com.github.cafdataprocessing.worker.policy.WorkerTaskResponsePolicyHandler;
import com.github.cafdataprocessing.worker.policy.handlers.boilerplate.*;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.util.ref.DataSourceException;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.boilerplateshared.*;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Unit tests for the Boilerplate Handler.
 */
public class BoilerplateHandlerTest {
    private CorePolicyApplicationContext applicationContext;
    private BoilerplateWorkerHandler handler;

    private static final String DOC_FIELD_NAME_1 = "key1";
    private static final String DOC_FIELD_NAME_2 = "key2";
    private static final String DOC_FIELD_NAME_3 = "key3";
    private static final String DOC_FIELD_NAME_CONTENT = "CONTENT";
    private static final String DOC_REF_FIELD_NAME_1 = "refKey1";
    private static final String DOC_REF_FIELD_NAME_2 = "refKey2";
    private static final String DOC_REF_FIELD_DREDBNAME = "DREDBNAME";

    @Before
    public void SetupApplicationContext(){
        this.applicationContext = new CorePolicyApplicationContext();

        createBeanDefinition(WorkerResponseHolder.class, "WorkerResponseHolder", "thread");
        createBeanDefinition(WorkerRequestHolder.class, "WorkerRequestHolder", "thread");

        applicationContext.refresh();
        applicationContext.getBean(WorkerRequestHolder.class).clear();
        applicationContext.getBean(WorkerResponseHolder.class).setTaskData(new TaskData());
        handler = new BoilerplateWorkerHandler();
        handler.setApplicationContext(applicationContext);
    }

    private void createBeanDefinition(Class<?> beanClass, String beanClassName, String beanScope ) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setScope(beanScope);
        beanDefinition.setBeanClass(beanClass);

        applicationContext.registerBeanDefinition(beanClassName, beanDefinition);
    }

    private static BoilerplateHandlerProperties getProperties()
    {
        AnnotationConfigApplicationContext propertiesApplicationContext = new AnnotationConfigApplicationContext();
        propertiesApplicationContext.register(PropertySourcesPlaceholderConfigurer.class);
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(BoilerplateHandlerProperties.class);
        propertiesApplicationContext.registerBeanDefinition("BoilerplateHandlerProperties", beanDefinition);
        propertiesApplicationContext.refresh();
        return propertiesApplicationContext.getBean(BoilerplateHandlerProperties.class);
    }

    private BoilerplatePolicyDefinition createPolicyDefinition(String queueName, Set<String> fields,
                                                               Set<Long> expressionIds,
                                                               EmailSegregationRules emailSegregationRules, Long tagId,
                                                               RedactionType redactionType, EmailSignatureDetection emailSignatureDetection) {
        BoilerplatePolicyDefinition polDefn = new BoilerplatePolicyDefinition();
        polDefn.queueName = queueName;
        polDefn.fields = fields;
        if(expressionIds!=null) {
            polDefn.expressionIds = expressionIds;
        }
        else if (tagId != null ) {
            polDefn.tagId = tagId;
        }
        else if (emailSegregationRules != null){
            polDefn.emailSegregationRules = emailSegregationRules;
        } else if (emailSignatureDetection != null){
            if (emailSignatureDetection.sender != null) {
                polDefn.emailSignatureDetection = emailSignatureDetection;
            }
        }
        polDefn.redactionType = redactionType;
        return polDefn;
    }

    private Document createDocument(Multimap<String, ReferencedData> metadataReferences){
        Document document = new DocumentImpl();
        String testDocumentReference = "test";
        document.setReference(testDocumentReference);

        //set up document with metadata
        Multimap<String,String> testFields = HashMultimap.create();
        testFields.put(DOC_FIELD_NAME_CONTENT, "value");
        testFields.put(DOC_FIELD_NAME_1, "value1");
        testFields.put(DOC_FIELD_NAME_2, "value2");
        testFields.put(DOC_FIELD_NAME_3, "value3");
        document.getMetadata().putAll(testFields);

        //set up document with metadata references
        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        com.github.cafdataprocessing.worker.policy.shared.Document taskDataDocument = new com.github.cafdataprocessing.worker.policy.shared.Document();

        taskDataDocument.setMetadataReferences(metadataReferences);
        TaskData testTaskData = new TaskData();
        testTaskData.setDocument(taskDataDocument);
        testTaskData.setOutputPartialReference(UUID.randomUUID().toString());
        workerResponseHolder.setTaskData(testTaskData);

        return document;
    }

    private void testBoilerplateHandler(String queueName, Set<String> fields, Set<Long> expressionIds, Long tagId,
                                        RedactionType redactionType) throws IOException, DataSourceException{
        testBoilerplateHandler(queueName, fields, expressionIds, tagId, redactionType, null);
    }

    private void testBoilerplateHandler(String queueName, Set<String> fields, Set<Long> expressionIds, Long tagId,
                                        RedactionType redactionType, String tenantId) throws IOException, DataSourceException {
        testBoilerplateHandler(queueName, fields, expressionIds, null, null,redactionType, null, tenantId);
    }

    private void testBoilerplateHandler(String queueName, Set<String> fields, Set<Long> expressionIds, Long tagId, EmailSegregationRules emailSegregationRules,
                                   RedactionType redactionType, EmailSignatureDetection emailSignatureDetection, String tenantId) throws IOException, DataSourceException {
        Policy testPolicy = new Policy();
        testPolicy.id = 1L;

        //create policy
        BoilerplatePolicyDefinition policyDefinition = createPolicyDefinition(queueName, fields, expressionIds, emailSegregationRules, tagId, redactionType, emailSignatureDetection);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new GuavaModule());
        testPolicy.details = mapper.valueToTree(policyDefinition);

        Multimap<String, ReferencedData> metadataReferences = ArrayListMultimap.create();
        metadataReferences.put(DOC_REF_FIELD_NAME_1, ReferencedData.getWrappedData("refValue1".getBytes()));
        metadataReferences.put(DOC_REF_FIELD_NAME_2, ReferencedData.getWrappedData("refValue2".getBytes()));
        metadataReferences.put(DOC_REF_FIELD_DREDBNAME, ReferencedData.getWrappedData("test".getBytes()));
        Document document = createDocument(metadataReferences);

        Long testColSeqId = 1L;
        handler.handle(document, testPolicy, testColSeqId);

        //combine metadata and metadata references for test verification phase
        for(Map.Entry<String, String> testField: document.getMetadata().entries()) {
            if(testField.getValue() != null) {
                metadataReferences.put(testField.getKey(), ReferencedData.getWrappedData(testField.getValue().getBytes()));
            } else {
                metadataReferences.put(testField.getKey(), ReferencedData.getWrappedData(null));
            }
        }

        checkHandleWorkerResponse(metadataReferences, policyDefinition, tenantId);
    }

    private void checkHandleWorkerResponse(Multimap<String,ReferencedData> testDocumentFields,
                                           BoilerplatePolicyDefinition policyDefinition, String tenantId) throws DataSourceException, IOException {
        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        WorkerTaskResponsePolicyHandler.WorkerHandlerResponse response = workerResponseHolder.getChainWorkerResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals("Check that worker response is NEW_TASK", TaskStatus.NEW_TASK, response.getTaskStatus());
        Assert.assertEquals("Expecting message type to be BoilerplateWorker", BoilerplateWorkerConstants.WORKER_NAME, response.getMessageType());
        Assert.assertEquals("Expecting API version to match", BoilerplateWorkerConstants.WORKER_API_VERSION, response.getApiVersion());

        String returnedQueue = response.getQueueReference();
        if(policyDefinition.queueName!=null){
            //if queueName was passed on policyDefinition check it was set in response
            Assert.assertEquals("Expecting queue name returned to be the same as that passed on policy definition", policyDefinition.queueName, returnedQueue);
        }
        else {
            BoilerplateHandlerProperties loadedProperties = getProperties();
            Assert.assertEquals("No queue name was set on Policy Definition, expecting queue name to be pulled from properties", loadedProperties.getTaskQueueName(), returnedQueue );
        }

        BoilerplateWorkerTask task = (BoilerplateWorkerTask) response.getData();
        checkBoilerplateWorkerTask(testDocumentFields, policyDefinition, task, tenantId);
    }

    private void checkBoilerplateWorkerTask(Multimap<String,ReferencedData> testDocumentFields, BoilerplatePolicyDefinition policyDefinition,
                                            BoilerplateWorkerTask task, String expectedTenantId) throws IOException, DataSourceException {
        Assert.assertNotNull("Expecting task for BoilerplateWorker to not be null", task);
        RedactionType expectedRedactionType = policyDefinition.redactionType;

        //check redaction type returned
        RedactionType returnedRedactionType = task.getRedactionType();
        Assert.assertEquals("Redaction type should be " + expectedRedactionType.toString(), expectedRedactionType, returnedRedactionType);

        //check tenant ID is as expected
        String returnedTenantId = task.getTenantId();
        Assert.assertEquals("Expecting tenantId on task returned to be the expected value.", expectedTenantId, returnedTenantId);

        //check expressions are those sent
        checkSelectedItems(policyDefinition, task);

        //check source data fields are as expected
        checkSourceData(testDocumentFields, policyDefinition, task);
    }

    private void checkSourceData(Multimap<String,ReferencedData> testDocumentFields, BoilerplatePolicyDefinition policyDefinition,
                                 BoilerplateWorkerTask task) throws DataSourceException, IOException {
        Multimap<String,ReferencedData> expectedFields = ArrayListMultimap.create();

        if(policyDefinition.fields == null || policyDefinition.fields.size() == 0){
            //if no fields were passed on policy definition then expect default fields from BoilerplateHandlerProperties to be returned
            BoilerplateHandlerProperties props = this.getProperties();
            for(String defaultField : props.getDefaultFields()) {
                expectedFields.putAll(defaultField, testDocumentFields.get(defaultField));
            }
        }
        else{
            for(String filterFieldName: policyDefinition.fields){
                expectedFields.putAll(filterFieldName, testDocumentFields.get(filterFieldName));
            }
        }
        Multimap<String, ReferencedData> returnedData = task.getSourceData();
        //check returned data is the expected length
        Assert.assertEquals("Expecting returned fields to be same size as expected number of fields.", expectedFields.size(), returnedData.size());
        for(Map.Entry<String, ReferencedData> returnedField: returnedData.entries()){
            //check returned field is in list of expected fields
            String returnedFieldName = returnedField.getKey();
            expectedFields.containsKey(returnedField);

            //check the value is the same on both field objects
            ReferencedData returnedValue = returnedField.getValue();
            ReferencedData expectedValue = expectedFields.get(returnedFieldName).iterator().next();
            Assert.assertEquals("Expected ReferencedData references to be the same on field "+returnedFieldName, expectedValue.getReference(), returnedValue.getReference());
            //get data (passing null datasource here as these should all be stored in memory)
            InputStream expectedValueStream = expectedValue.acquire(null);
            InputStream returnedValueStream = returnedValue.acquire(null);
            Assert.assertEquals("Value returned on field " + returnedFieldName + " should be same as expected value.", IOUtils.toString(expectedValueStream), IOUtils.toString(returnedValueStream));
        }
    }

    private void checkSelectedItems(BoilerplatePolicyDefinition policyDefinition, BoilerplateWorkerTask task){
        //if expressions were present on policy definition then check they are provided on the task
        Set<Long> expectedExpressionIds = policyDefinition.expressionIds;
        Long expectedTagId = policyDefinition.tagId;
        EmailSegregationRules emailSegregationRules = policyDefinition.emailSegregationRules;
        EmailSignatureDetection emailSignatureDetection = policyDefinition.emailSignatureDetection;
        SelectedItems returnedItems = task.getExpressions();
        if(expectedExpressionIds !=null && expectedExpressionIds.size() > 0) {
            //expecting returned items to be castable to SelectedExpressions
            SelectedExpressions returnedExpressions = null;
            try {
                returnedExpressions = (SelectedExpressions) returnedItems;
            }
            catch(ClassCastException e){
                Assert.fail("Unable to cast task expressions to expected type of SelectedExpressions.");
            }
            checkExpressions(expectedExpressionIds, returnedExpressions);
        }
        else if(expectedTagId != null && expectedTagId > 0){
            SelectedTag returnedTag = null;
            try{
                returnedTag = (SelectedTag) returnedItems;
            }
            catch(ClassCastException e){
                Assert.fail("Unable to cast task expressions to expected type of SelectedTag.");
            }
            checkTag(expectedTagId, returnedTag);
        }
        else if(emailSegregationRules != null){
            SelectedEmail returnedEmail = null;
            try {
                returnedEmail = (SelectedEmail) returnedItems;
            }
            catch(ClassCastException e){
                Assert.fail("Unable to cast task expressions to expected type of SelectedEmail.");
            }
            checkEmail(emailSegregationRules, returnedEmail, policyDefinition);
        }
        else if(emailSignatureDetection != null){
            SelectedEmailSignature returnedSender = null;
            try {
                returnedSender = (SelectedEmailSignature) returnedItems;
            } catch(ClassCastException e){
                Assert.fail("Unable to cast task expressions to expected type of SelectedEmailSignature.");
            }
            checkSender(emailSignatureDetection, returnedSender);
        }
        //currently accepting the state where neither tag id or expression ids are set on task.
    }

    private void checkSender(EmailSignatureDetection emailSignatureDetection, SelectedEmailSignature returnedSender) {
        Assert.assertEquals("Expecting email signature detection sender returned to be the same as that sent.", emailSignatureDetection.sender, returnedSender.sender);
    }

    private void checkExpressions(Set<Long> expectedExpressionIds, SelectedExpressions returnedExpressions){
        Collection<Long> returnedExpressionIds = returnedExpressions.getExpressionIds();
        Assert.assertEquals("Expecting same number of expression ids returned as were sent", expectedExpressionIds.size(), returnedExpressionIds.size());
        for(Long expectedExpressionId:expectedExpressionIds){
            Assert.assertTrue("Expecting expressionId to be in collection of returned expression ids.", returnedExpressionIds.contains(expectedExpressionId));
        }
    }

    private void checkTag(Long expectedTagId, SelectedTag returnedTag){
        Long returnedTagId = returnedTag.getTagId();
        Assert.assertEquals("Expecting tag id returned to be the same as that sent.", expectedTagId, returnedTagId);
    }

    private void checkEmail(EmailSegregationRules segregationRules, SelectedEmail returnedEmail, BoilerplatePolicyDefinition policyDefinition){
        Assert.assertEquals("Expecting primary content expression to be the same as that sent.", segregationRules.primaryExpression, returnedEmail.primaryContent);
        Assert.assertEquals("Expecting secondary content expression to be the same as that sent.", segregationRules.secondaryExpression, returnedEmail.secondaryContent);
        Assert.assertEquals("Expecting tertiary content expression to be the same as that sent.", segregationRules.tertiaryExpression, returnedEmail.tertiaryContent);
        Assert.assertEquals("Expected primary content field name to be the same as that sent.", policyDefinition.emailSegregationRules.primaryFieldName, segregationRules.primaryFieldName);
        Assert.assertEquals("Expected secondary content field name to be the same as that sent.", policyDefinition.emailSegregationRules.secondaryFieldName, segregationRules.secondaryFieldName);
        Assert.assertEquals("Expected tertiary content field name to be the same as that sent.", policyDefinition.emailSegregationRules.tertiaryFieldName, segregationRules.tertiaryFieldName);
    }

    @Test
    public void testReadDefinitionFromResourceFile() throws IOException {
        BoilerplateWorkerHandler handler = new BoilerplateWorkerHandler();
        PolicyType policyType = handler.getPolicyType();
        JsonNode policyDef = new ObjectMapper().readTree(this.getClass().getResource("/boilerplate-policy-definition.json"));
        Assert.assertEquals("Should have loaded policy definition from file", policyDef, policyType.definition);
    }

    @Test
    public void testQueueNamePassedBack() throws IOException, DataSourceException {
        RedactionType redactionType = RedactionType.DO_NOTHING;
        HashSet<String> fields = new HashSet<String>();
        fields.add(DOC_FIELD_NAME_3);
        testBoilerplateHandler("myQueue", fields, null, null, redactionType);
    }

    @Test
    public void testDefaultQueueNamePassedBack() throws IOException, DataSourceException {
        RedactionType redactionType = RedactionType.DO_NOTHING;
        HashSet<String> fields = new HashSet<String>();
        fields.add(DOC_FIELD_NAME_3);
        testBoilerplateHandler(null, fields, null, null, redactionType);
    }

    @Test
    public void testRedactionTypeReturnedCorrectly() throws IOException, DataSourceException {
        HashSet<String> fields = new HashSet<String>();
        fields.add(DOC_FIELD_NAME_3);

        RedactionType redactionType1 = RedactionType.DO_NOTHING;
        testBoilerplateHandler(null, fields, null, null, redactionType1);

        RedactionType redactionType2 = RedactionType.REMOVE;
        testBoilerplateHandler(null, fields, null, null, redactionType2);

        RedactionType redactionType3 = RedactionType.REPLACE;
        testBoilerplateHandler(null, fields, null, null, redactionType3);
    }

    @Test
    public void testSingleFieldOnExpressionId() throws IOException, DataSourceException {
        HashSet<String> fields = new HashSet<String>();
        fields.add(DOC_FIELD_NAME_3);

        Set<Long> expressionIds = new HashSet<Long>();
        expressionIds.add(1L);

        RedactionType redactionType = RedactionType.DO_NOTHING;
        testBoilerplateHandler(null, fields, expressionIds, null, redactionType);
    }

    @Test
    public void testMultipleFieldsAndMultipleExpressionIds() throws IOException, DataSourceException {
        HashSet<String> fields = new HashSet<String>();
        fields.add(DOC_FIELD_NAME_3);
        fields.add(DOC_REF_FIELD_DREDBNAME);

        Set<Long> expressionIds = new HashSet<Long>();
        expressionIds.add(1L);
        expressionIds.add(2L);
        expressionIds.add(3L);

        RedactionType redactionType = RedactionType.REMOVE;
        testBoilerplateHandler(null, fields, expressionIds, null, redactionType);
    }

    @Test
    public void testTenantIdPassed() throws IOException, DataSourceException {
        String testProjectId = UUID.randomUUID().toString();
        applicationContext.getBean(UserContext.class).setProjectId(testProjectId);

        Set<Long> expressionIds = new HashSet<Long>();
        expressionIds.add(1L);

        testBoilerplateHandler(null, null, expressionIds, null, RedactionType.DO_NOTHING, testProjectId);
    }

    @Test
    public void testNoExpressionIdsOrTagIdPassed() throws IOException, DataSourceException {
        testBoilerplateHandler(null, null, null, null, RedactionType.DO_NOTHING);
    }

    @Test
    public void testSingleFieldOnTagId() throws IOException, DataSourceException {
        HashSet<String> fields = new HashSet<String>();
        fields.add(DOC_FIELD_NAME_3);

        Long tagId = 1L;

        RedactionType redactionType = RedactionType.DO_NOTHING;
        testBoilerplateHandler(null, fields, null, tagId, redactionType);
    }

    @Test
    public void testSelectedEmailPassed() throws IOException, DataSourceException {
        HashSet<String> fields = new HashSet<String>();
        fields.add(DOC_FIELD_NAME_3);

        EmailSegregationRules emailSegregationRules = new EmailSegregationRules();
        emailSegregationRules.primaryExpression = "1";
        emailSegregationRules.secondaryExpression = "2..3";
        emailSegregationRules.tertiaryExpression = "LAST";
        emailSegregationRules.primaryFieldName = "MY_PRIMARY_CONTENT";
        emailSegregationRules.secondaryFieldName = "MY_SECONDARY_CONTENT";
        emailSegregationRules.tertiaryFieldName = "MY_THIRD_CONTENT";
        testBoilerplateHandler(null, fields, null, null, emailSegregationRules, RedactionType.DO_NOTHING, null, null);
    }

    @Test
    public void testSelectedEmailSenderSignaturePassed() throws IOException, DataSourceException {
        HashSet<String> fields = new HashSet<String>();
        fields.add(DOC_FIELD_NAME_3);

        EmailSignatureDetection emailSignatureDetection = new EmailSignatureDetection();
        emailSignatureDetection.sender = "an.email.address@hpe.com";

        testBoilerplateHandler(null, fields, null, null, null, RedactionType.DO_NOTHING, emailSignatureDetection, null);
    }
}