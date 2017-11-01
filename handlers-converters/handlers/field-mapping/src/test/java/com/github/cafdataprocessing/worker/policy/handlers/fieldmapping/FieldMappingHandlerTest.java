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
package com.github.cafdataprocessing.worker.policy.handlers.fieldmapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cafdataprocessing.corepolicy.common.CaseInsensitiveKeyMultimap;
import com.github.cafdataprocessing.corepolicy.common.CorePolicyApplicationContext;
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.DocumentImpl;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.worker.policy.WorkerRequestHolder;
import com.github.cafdataprocessing.worker.policy.WorkerResponseHolder;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.google.common.collect.Multimap;
import com.hpe.caf.util.ref.ReferencedData;
import org.junit.Assert;
import org.junit.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.Before;
import org.springframework.beans.factory.support.RootBeanDefinition;

public class FieldMappingHandlerTest
{
    private CorePolicyApplicationContext applicationContext;
    private FieldMappingHandler handler;

    @Before
    public void setupApplicationContext()
    {
        this.applicationContext = new CorePolicyApplicationContext();

        createBeanDefinition(WorkerResponseHolder.class, "WorkerResponseHolder", "thread");
        createBeanDefinition(WorkerRequestHolder.class, "WorkerRequestHolder", "thread");

        applicationContext.refresh();
        applicationContext.getBean(WorkerRequestHolder.class).clear();
        applicationContext.getBean(WorkerResponseHolder.class).setTaskData(new TaskData());
        handler = new FieldMappingHandler();
        handler.setApplicationContext(applicationContext);
    }

    @Test
    public void testSimpleRenaming() throws IOException
    {
        //Arrange
        final Document document = setupDocument(null);
        final Policy testPolicy = new Policy();
        testPolicy.id = 1L;
        testPolicy.details = new ObjectMapper().readTree("{"
            + "  \"mappings\":"
            + "  {"
            + "    \"abc\": \"def\","
            + "    \"PQR\": \"xyz\""
            + "  }"
            + "}");
        final Long testColSeqId = 1L;

        //Act
        handler.handle(document, testPolicy, testColSeqId);

        //Assert
        Assert.assertTrue(document.getMetadata().get("def").contains("abc-value1"));
        Assert.assertTrue(document.getMetadata().get("def").contains("abc-value2"));

        Assert.assertTrue(document.getMetadata().get("xyz").contains("pqr-value1"));
        Assert.assertTrue(document.getMetadata().get("xyz").contains("pqr-value2"));
    }

    @Test
    public void testAbsentSourceFieldDoesntDeleteTargetField() throws IOException
    {
        //Arrange
        final Document document = setupDocument(null);
        final Policy testPolicy = new Policy();
        testPolicy.id = 1L;
        testPolicy.details = new ObjectMapper().readTree("{"
            + "  \"mappings\":"
            + "  {"
            + "    \"absentField\": \"def\""
            + "  }"
            + "}");
        final Long testColSeqId = 1L;

        //Act
        handler.handle(document, testPolicy, testColSeqId);

        //Assert
        Assert.assertTrue(document.getMetadata().get("def").contains("def-value1"));
        Assert.assertTrue(document.getMetadata().get("def").contains("def-value2"));

        Assert.assertTrue(document.getMetadata().get("absentField").isEmpty());
    }

    @Test
    public void testFieldNameSwap() throws IOException
    {
        //Arrange
        final Document document = setupDocument(null);
        final Policy testPolicy = new Policy();
        testPolicy.id = 1L;
        testPolicy.details = new ObjectMapper().readTree("{"
            + "  \"mappings\":"
            + "  {"
            + "    \"abc\": \"Def\","
            + "    \"DEF\": \"pqr\","
            + "    \"pqr\": \"ABC\""
            + "  }"
            + "}");
        final Long testColSeqId = 1L;

        //Act
        handler.handle(document, testPolicy, testColSeqId);

        //Assert
        Assert.assertTrue(document.getMetadata().get("def").contains("abc-value1"));
        Assert.assertTrue(document.getMetadata().get("def").contains("abc-value2"));

        Assert.assertTrue(document.getMetadata().get("pqr").contains("def-value1"));
        Assert.assertTrue(document.getMetadata().get("pqr").contains("def-value2"));

        Assert.assertTrue(document.getMetadata().get("abc").contains("pqr-value1"));
        Assert.assertTrue(document.getMetadata().get("abc").contains("pqr-value2"));

        Assert.assertTrue(document.getMetadata().get("xyz").contains("xyz-value1"));
        Assert.assertTrue(document.getMetadata().get("xyz").contains("xyz-value2"));
    }

    @Test
    public void testFieldNameSwapWithCommonTargetName() throws IOException
    {
        //Arrange
        final Document document = setupDocument(null);
        final Policy testPolicy = new Policy();
        testPolicy.id = 1L;
        testPolicy.details = new ObjectMapper().readTree("{"
            + "  \"mappings\":"
            + "  {"
            + "    \"abc\": \"xyz\","
            + "    \"def\": \"pqr\","
            + "    \"pqr\": \"xyz\""
            + "  }"
            + "}");
        final Long testColSeqId = 1L;

        //Act
        handler.handle(document, testPolicy, testColSeqId);

        //Assert
        Assert.assertTrue(document.getMetadata().get("xyz").contains("abc-value1"));
        Assert.assertTrue(document.getMetadata().get("xyz").contains("abc-value2"));
        Assert.assertTrue(document.getMetadata().get("xyz").contains("pqr-value1"));
        Assert.assertTrue(document.getMetadata().get("xyz").contains("pqr-value2"));
        Assert.assertTrue(document.getMetadata().get("xyz").contains("xyz-value1"));
        Assert.assertTrue(document.getMetadata().get("xyz").contains("xyz-value2"));

        Assert.assertTrue(document.getMetadata().get("pqr").contains("def-value1"));
        Assert.assertTrue(document.getMetadata().get("pqr").contains("def-value2"));

        Assert.assertTrue(document.getMetadata().get("abc").isEmpty());
    }

    @Test
    public void testFieldNameMappingOfEncodedField() throws IOException
    {
        final Multimap<String, ReferencedData> metadataReferences = new CaseInsensitiveKeyMultimap<>();
        final ReferencedData ref1 = ReferencedData.getWrappedData("ReferenceField1'sValue".getBytes(StandardCharsets.UTF_8));
        final ReferencedData ref2 = ReferencedData.getWrappedData("ReferenceField2'sValue".getBytes(StandardCharsets.UTF_8));
        final ReferencedData ref3 = ReferencedData.getWrappedData("ReferenceField3'sValue".getBytes(StandardCharsets.UTF_8));
        metadataReferences.put("abc", ref1);
        metadataReferences.put("def", ref2);
        metadataReferences.put("jkl", ref3);
        //Arrange
        final Document document = setupDocument(metadataReferences);
        final Policy testPolicy = new Policy();
        testPolicy.id = 1L;
        testPolicy.details = new ObjectMapper().readTree("{"
            + "  \"mappings\":"
            + "  {"
            + "    \"abc\": \"xyz\","
            + "    \"def\": \"pqr\","
            + "    \"pqr\": \"xyz\","
            + "    \"jkl\": \"wrx\""
            + "  }"
            + "}");
        final Long testColSeqId = 1L;

        //Act
        handler.handle(document, testPolicy, testColSeqId);

        //Assert
        Assert.assertTrue(document.getMetadata().get("xyz").contains("abc-value1"));
        Assert.assertTrue(document.getMetadata().get("xyz").contains("abc-value2"));
        Assert.assertTrue(document.getMetadata().get("xyz").contains("pqr-value1"));
        Assert.assertTrue(document.getMetadata().get("xyz").contains("pqr-value2"));
        Assert.assertTrue(document.getMetadata().get("xyz").contains("xyz-value1"));
        Assert.assertTrue(document.getMetadata().get("xyz").contains("xyz-value2"));

        Assert.assertTrue(document.getMetadata().get("pqr").contains("def-value1"));
        Assert.assertTrue(document.getMetadata().get("pqr").contains("def-value2"));

        Assert.assertTrue(document.getMetadata().get("abc").isEmpty());

        //Test to confirm field mappings to new field names were successfull
        final com.github.cafdataprocessing.worker.policy.shared.Document documentAfterTest = applicationContext
            .getBean(WorkerResponseHolder.class)
            .getTaskData()
            .getDocument();
        Assert.assertEquals(documentAfterTest.getMetadataReferences().get("xyz").stream().findFirst().get(), ref1);
        Assert.assertEquals(documentAfterTest.getMetadataReferences().get("pqr").stream().findFirst().get(), ref2);
        Assert.assertEquals(documentAfterTest.getMetadataReferences().get("wrx").stream().findFirst().get(), ref3);

        //Test to ensure that old fields were removed
        Assert.assertTrue(!documentAfterTest.getMetadataReferences().containsKey("abc"));
        Assert.assertTrue(!documentAfterTest.getMetadataReferences().containsKey("def"));
        Assert.assertTrue(!documentAfterTest.getMetadataReferences().containsKey("jkl"));
    }

    private Document setupDocument(final Multimap<String, ReferencedData> metadataReferences)
    {
        final Document document = new DocumentImpl();
        document.setReference("test");

        document.getMetadata().put("abc", "abc-value1");
        document.getMetadata().put("abc", "abc-value2");

        document.getMetadata().put("def", "def-value1");
        document.getMetadata().put("def", "def-value2");

        document.getMetadata().put("pqr", "pqr-value1");
        document.getMetadata().put("pqr", "pqr-value2");

        document.getMetadata().put("xyz", "xyz-value1");
        document.getMetadata().put("xyz", "xyz-value2");

        WorkerResponseHolder workerResponseHolder = applicationContext.getBean(WorkerResponseHolder.class);
        com.github.cafdataprocessing.worker.policy.shared.Document taskDataDocument
            = new com.github.cafdataprocessing.worker.policy.shared.Document();
        if (metadataReferences != null) {
            taskDataDocument.setMetadataReferences(metadataReferences);
        }
        TaskData testTaskData = new TaskData();
        testTaskData.setDocument(taskDataDocument);
        testTaskData.setOutputPartialReference(UUID.randomUUID().toString());
        workerResponseHolder.setTaskData(testTaskData);
        return document;
    }

    private void createBeanDefinition(Class<?> beanClass, String beanClassName, String beanScope)
    {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setScope(beanScope);
        beanDefinition.setBeanClass(beanClass);

        applicationContext.registerBeanDefinition(beanClassName, beanDefinition);
    }
}
