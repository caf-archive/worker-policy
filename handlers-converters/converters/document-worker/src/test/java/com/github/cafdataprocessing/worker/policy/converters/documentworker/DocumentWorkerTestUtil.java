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
package com.github.cafdataprocessing.worker.policy.converters.documentworker;

import com.github.cafdataprocessing.worker.policy.converter.qa.TestWorkerConverterRuntimeImpl;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.TrackedDocument;
import com.github.cafdataprocessing.worker.policy.shared.DocumentProcessingFieldType;
import com.google.common.collect.Multimap;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.document.DocumentWorkerAction;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.document.DocumentWorkerFieldChanges;
import com.hpe.caf.worker.document.DocumentWorkerResult;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import static javax.xml.bind.DatatypeConverter.parseBase64Binary;
import org.apache.commons.codec.binary.Base64;

public class DocumentWorkerTestUtil
{
    static TrackedDocument createTrackedDocument()
    {
        TrackedDocument trackedDocument = new TrackedDocument();
        trackedDocument.addFieldValue("CONTENT", "Test Value", DocumentProcessingFieldType.METADATA);
        trackedDocument.addFieldValue("CONTENT1", "Test Value3", DocumentProcessingFieldType.METADATA);
        trackedDocument.addFieldValue("CONTENT2", "Test Value4", DocumentProcessingFieldType.METADATA);
        trackedDocument.addFieldValue("CONTENT3", "Test Value5", DocumentProcessingFieldType.METADATA);
        trackedDocument.addFieldValue("DELETED_CONTENT", "New Testing Value", DocumentProcessingFieldType.METADATA);
        trackedDocument.addFieldValue("DELETED_REF_CONTENT", ReferencedData.getReferencedData(UUID.randomUUID().toString()),
                                      DocumentProcessingFieldType.METADATA_REFERENCE);
        trackedDocument.addFieldValue("REF_CONTENT_OTHER", ReferencedData.getReferencedData(UUID.randomUUID().toString()),
                                      DocumentProcessingFieldType.METADATA_REFERENCE);
        trackedDocument.addFieldValue("REF_CONTENT", ReferencedData.getWrappedData(parseBase64Binary("cmFuZG9tU3RyaW5nW10=")),
                                      DocumentProcessingFieldType.METADATA_REFERENCE);

        return trackedDocument;
    }

    static List<DocumentWorkerFieldValue> createTestDataList(DocumentWorkerFieldEncoding encoding)
    {
        List<DocumentWorkerFieldValue> testDataList = new ArrayList<>();
        DocumentWorkerFieldValue testData = new DocumentWorkerFieldValue();
        testData.data = "New Testing Value";
        testData.encoding = encoding;
        testDataList.add(testData);

        return testDataList;
    }

    static List<DocumentWorkerFieldValue> createTestDataList(String data, DocumentWorkerFieldEncoding encoding)
    {
        List<DocumentWorkerFieldValue> testDataList = new ArrayList<>();
        DocumentWorkerFieldValue testData = new DocumentWorkerFieldValue();
        testData.data = data;
        testData.encoding = encoding;
        testDataList.add(testData);

        return testDataList;
    }

    static DocumentWorkerFieldChanges createDocumentWorkerFieldChanges(DocumentWorkerAction action, DocumentWorkerFieldEncoding encoding)
    {
        DocumentWorkerFieldChanges testFieldChanges = new DocumentWorkerFieldChanges();
        testFieldChanges.action = action;
        testFieldChanges.values = createTestDataList(encoding);

        return testFieldChanges;
    }

    static DocumentWorkerFieldChanges createDocumentWorkerFieldChanges(DocumentWorkerAction action, String data,
                                                                       DocumentWorkerFieldEncoding encoding)
    {
        DocumentWorkerFieldChanges testFieldChanges = new DocumentWorkerFieldChanges();
        testFieldChanges.action = action;
        testFieldChanges.values = createTestDataList(data, encoding);

        return testFieldChanges;
    }

    static DocumentWorkerResult createDocumentWorkerTestResponse(DocumentWorkerAction action, String FieldName,
                                                                 DocumentWorkerFieldEncoding encoding)
    {
        DocumentWorkerResult testResponse = new DocumentWorkerResult();
        testResponse.fieldChanges = new HashMap<>();
        testResponse.fieldChanges.put(FieldName, createDocumentWorkerFieldChanges(action, encoding));

        return testResponse;
    }

    static DocumentWorkerResult createDocumentWorkerTestResponse(DocumentWorkerAction action, String FieldName, String data,
                                                                 DocumentWorkerFieldEncoding encoding)
    {
        DocumentWorkerResult testResponse = new DocumentWorkerResult();
        testResponse.fieldChanges = new HashMap<>();
        testResponse.fieldChanges.put(FieldName, createDocumentWorkerFieldChanges(action, data, encoding));

        return testResponse;
    }

    static TestWorkerConverterRuntimeImpl createDocumentWorkerConverterQARuntime(TrackedDocument trackedDocument, DataStore dataStore)
    {
        JsonCodec jsonCodec = new JsonCodec();
        TestWorkerConverterRuntimeImpl runtime = new TestWorkerConverterRuntimeImpl(trackedDocument, jsonCodec, dataStore, null);
        return runtime;
    }

    static byte[] createByte(TrackedDocument trackedDocument, String key)
    {
        byte[] testByte = null;
        Collection<ReferencedData> RefData = trackedDocument.getMetadataReferences().get(key);
        for (ReferencedData referenced : RefData) {
            testByte = referenced.getData();
        }
        return testByte;
    }

    static String getStringValue(TrackedDocument trackedDocument, String key)
    {
        String value = "";
        Collection<String> a = trackedDocument.getMetadata().get(key);
        for (String b : a) {
            value = b;
        }
        return value;
    }

    static String getStringValueForMetaRef(TrackedDocument trackedDocument, String key) throws UnsupportedEncodingException
    {
        byte[] testByte = createByte(trackedDocument, key);
        String value = new String(testByte, StandardCharsets.UTF_8);
        return value;
    }

    static String getStringValueForMeta(TrackedDocument trackedDocument, String key)
    {
        String value = "";
        Multimap<String, String> testMap = trackedDocument.getMetadata();
        for (String testS : testMap.get(key)) {
            value = testS;
        }
        return value;
    }

    static String getBase64EncodedString(String testString)
    {
        String value = Base64.encodeBase64String(testString.getBytes(StandardCharsets.UTF_8));
        return value;
    }
}
