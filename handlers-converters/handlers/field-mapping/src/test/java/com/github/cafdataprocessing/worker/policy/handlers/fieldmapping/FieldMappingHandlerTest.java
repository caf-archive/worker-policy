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
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.DocumentImpl;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class FieldMappingHandlerTest
{
    @Test
    public void testSimpleRenaming() throws IOException
    {
        //Arrange
        final Document document = setupDocument();
        final Policy testPolicy = new Policy();
        testPolicy.id = 1L;
        testPolicy.details = new ObjectMapper().readTree("{" +
                "  \"mappings\":" +
                "  {" +
                "    \"abc\": \"def\"," +
                "    \"PQR\": \"xyz\"" +
                "  }" +
                "}");
        final Long testColSeqId = 1L;

        //Act
        final FieldMappingHandler handler = new FieldMappingHandler();
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
        final Document document = setupDocument();
        final Policy testPolicy = new Policy();
        testPolicy.id = 1L;
        testPolicy.details = new ObjectMapper().readTree("{" +
                "  \"mappings\":" +
                "  {" +
                "    \"absentField\": \"def\"" +
                "  }" +
                "}");
        final Long testColSeqId = 1L;

        //Act
        final FieldMappingHandler handler = new FieldMappingHandler();
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
        final Document document = setupDocument();
        final Policy testPolicy = new Policy();
        testPolicy.id = 1L;
        testPolicy.details = new ObjectMapper().readTree("{" +
                "  \"mappings\":" +
                "  {" +
                "    \"abc\": \"Def\"," +
                "    \"DEF\": \"pqr\"," +
                "    \"pqr\": \"ABC\"" +
                "  }" +
                "}");
        final Long testColSeqId = 1L;

        //Act
        final FieldMappingHandler handler = new FieldMappingHandler();
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
        final Document document = setupDocument();
        final Policy testPolicy = new Policy();
        testPolicy.id = 1L;
        testPolicy.details = new ObjectMapper().readTree("{" +
                "  \"mappings\":" +
                "  {" +
                "    \"abc\": \"xyz\"," +
                "    \"def\": \"pqr\"," +
                "    \"pqr\": \"xyz\"" +
                "  }" +
                "}");
        final Long testColSeqId = 1L;

        //Act
        final FieldMappingHandler handler = new FieldMappingHandler();
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
        //Arrange
        final Document document = setupDocument();
        final Policy testPolicy = new Policy();
        testPolicy.id = 1L;
        testPolicy.details = new ObjectMapper().readTree("{" +
                "  \"mappings\":" +
                "  {" +
                "    \"abc\": \"xyz\"," +
                "    \"def\": \"pqr\"," +
                "    \"pqr\": \"xyz\"," +
                "    \"jkl\": \"wrx\"" +
                "  }" +
                "}");
        final Long testColSeqId = 1L;

        //Act
        final FieldMappingHandler handler = new FieldMappingHandler();
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
        
        Assert.assertTrue(document.getStreams().containsKey("xyz"));
        Assert.assertTrue(document.getStreams().containsKey("pqr"));
        Assert.assertTrue(document.getStreams().containsKey("wrx"));
    }


    private Document setupDocument(){
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
        
        document.getStreams().put("abc", null);
        document.getStreams().put("def", null);
        document.getStreams().put("jkl", null);
        return document;
    }
}
