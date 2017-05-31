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
package com.github.cafdataprocessing.worker.policy.converters.documentworker;

import com.github.cafdataprocessing.worker.policy.converter.qa.TestWorkerConverterRuntimeImpl;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.TrackedDocument;
import com.hpe.caf.worker.document.DocumentWorkerResult;
import org.junit.*;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.util.ref.DataSourceException;
import com.hpe.caf.worker.document.DocumentWorkerAction;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import org.mockito.Mock;

import static com.github.cafdataprocessing.worker.policy.converters.documentworker.DocumentWorkerTestUtil.*;
import java.io.UnsupportedEncodingException;
import static org.junit.Assert.*;

public class DocumentWorkerConverterTest
{
    private String testString;
    private TrackedDocument trackedDocument;

    @Before
    public void setUp()
    {
        trackedDocument = createTrackedDocument();
    }

    @Mock
    DataStore dataStore;

    @Test
    public void checkDocumentWorkerAddFields() throws UnsupportedEncodingException
    {
        DocumentWorkerResult testResponse = createDocumentWorkerTestResponse(DocumentWorkerAction.add, "ADD_CONTENT",
                                                                             DocumentWorkerFieldEncoding.utf8);

        TestWorkerConverterRuntimeImpl runtime = createDocumentWorkerConverterQARuntime(trackedDocument, dataStore);

        DocumentWorkerTaskDataUpdater.updateDocumentMetadata(runtime, testResponse);

        String testStringActual = getStringValue(trackedDocument, "ADD_CONTENT");

        assertTrue(trackedDocument.getMetadata().containsKey("ADD_CONTENT"));
        assertEquals("New Testing Value", testStringActual);
    }

    @Test
    public void checkDocumentWorkerAddRefFields() throws UnsupportedEncodingException
    {
        String testValue = getBase64EncodedString("Test Value");
        DocumentWorkerResult testResponse = createDocumentWorkerTestResponse(DocumentWorkerAction.add, "ADD_REF_CONTENT",
                                                                             testValue, DocumentWorkerFieldEncoding.base64);

        TestWorkerConverterRuntimeImpl runtime = createDocumentWorkerConverterQARuntime(trackedDocument, dataStore);

        DocumentWorkerTaskDataUpdater.updateDocumentMetadata(runtime, testResponse);

        testString = getStringValueForMetaRef(trackedDocument, "ADD_REF_CONTENT");

        assertTrue(trackedDocument.getMetadataReferences().containsKey("ADD_REF_CONTENT"));
        assertEquals("Test Value", testString);
    }

    @Test
    public void checkDocumentWorkerReplaceFields() throws UnsupportedEncodingException
    {
        DocumentWorkerResult testResponse = createDocumentWorkerTestResponse(DocumentWorkerAction.replace, "Content",
                                                                             "This is a replacement", DocumentWorkerFieldEncoding.utf8);

        TestWorkerConverterRuntimeImpl runtime = createDocumentWorkerConverterQARuntime(trackedDocument, dataStore);

        DocumentWorkerTaskDataUpdater.updateDocumentMetadata(runtime, testResponse);

        testString = getStringValueForMeta(trackedDocument, "Content");

        assertNotEquals("Test Value", testString);
        assertEquals("This is a replacement", testString);
    }

    @Test
    public void checkDocumentWorkerReplaceRefFields() throws DataSourceException, UnsupportedEncodingException
    {
        String testValue = getBase64EncodedString("This is a replacement Ref Value");
        DocumentWorkerResult testResponse = createDocumentWorkerTestResponse(DocumentWorkerAction.replace, "REF_CONTENT", testValue,
                                                                             DocumentWorkerFieldEncoding.base64);

        TestWorkerConverterRuntimeImpl runtime = createDocumentWorkerConverterQARuntime(trackedDocument, dataStore);

        String beforeTestValue = getStringValueForMetaRef(trackedDocument, "REF_CONTENT");

        DocumentWorkerTaskDataUpdater.updateDocumentMetadata(runtime, testResponse);

        testString = getStringValueForMetaRef(trackedDocument, "REF_CONTENT");

        assertNotEquals(beforeTestValue, testString);
        assertEquals("This is a replacement Ref Value", testString);
    }

    @Test
    public void checkDocumentWorkerDeleteFields() throws UnsupportedEncodingException
    {
        DocumentWorkerResult testResponse = createDocumentWorkerTestResponse(DocumentWorkerAction.replace, "DELETED_CONTENT", null,
                                                                             DocumentWorkerFieldEncoding.utf8);

        TestWorkerConverterRuntimeImpl runtime = createDocumentWorkerConverterQARuntime(trackedDocument, dataStore);

        DocumentWorkerTaskDataUpdater.updateDocumentMetadata(runtime, testResponse);

        assertFalse(trackedDocument.getMetadata().containsKey("DELETED_CONTENT"));
    }

    @Test
    public void checkDocumentWorkerDeleteRefFields() throws UnsupportedEncodingException
    {
        DocumentWorkerResult testResponse = createDocumentWorkerTestResponse(DocumentWorkerAction.replace, "DELETED_REF_CONTENT", null,
                                                                             DocumentWorkerFieldEncoding.utf8);

        TestWorkerConverterRuntimeImpl runtime = createDocumentWorkerConverterQARuntime(trackedDocument, dataStore);

        DocumentWorkerTaskDataUpdater.updateDocumentMetadata(runtime, testResponse);

        assertFalse(trackedDocument.getMetadata().containsKey("DELETED_REF_CONTENT"));
    }
}
