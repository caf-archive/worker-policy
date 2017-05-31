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
package com.github.cafdataprocessing.worker.policy.converter.qa;

import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterException;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterInterface;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterRuntimeBase;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.TrackedDocument;
import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.google.common.collect.Multimap;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.util.ref.ReferencedData;

import java.util.ArrayList;

/**
 * Helper to create objects and run the convert method of the
 */
public class ConvertHelper
{

    public static DocumentInterface RunConvert(Object result, ReferencedData documentReference, DataStore dataStore,
                                               TaskStatus taskStatus, PolicyWorkerConverterInterface converter, String classifier)
            throws PolicyWorkerConverterException, CodecException, InvalidTaskException
    {
        return RunConvert(result, documentReference.getReference(), dataStore, taskStatus, converter, classifier);
    }


    public static DocumentInterface RunConvert(Object bpResponse, ReferencedData documentReference, DataStore dataStore,
                                               TaskStatus taskStatus, PolicyWorkerConverterInterface converter, String classifier,
                                               Multimap<String, String> metadataForDoc, Multimap<String, ReferencedData> metadataReferencesForDoc)
            throws CodecException, InvalidTaskException, PolicyWorkerConverterException
    {
        return RunConvert(bpResponse, documentReference.getReference(), dataStore, taskStatus, converter, classifier, metadataForDoc, metadataReferencesForDoc);
    }


    public static DocumentInterface RunConvert(Object result, String ref, DataStore dataStore, TaskStatus taskStatus,
                                               PolicyWorkerConverterInterface converter, String classifier)
            throws PolicyWorkerConverterException, CodecException, InvalidTaskException
    {
        Codec codec = new JsonCodec();
        byte[] data;
        try {
            data = codec.serialise(result);
        } catch (CodecException e) {
            throw new RuntimeException("JsonProcessingException thrown while converting the test BoilerpalteWorkerResponse into byte array");
        }
        //create context
        DocumentInterface documentToClassify = createDocument(ref);

        TaskData taskData = new TaskData();
        taskData.setDocument((Document) documentToClassify);
        byte[] context;
        try {
            context = codec.serialise(taskData);
        } catch (CodecException e) {
            throw new RuntimeException("CodecException thrown while converting taskData for textDataTest into byte array");
        }

        TaskSourceInfo sourceInfo = new TaskSourceInfo();
        sourceInfo.setName("TestName");
        sourceInfo.setVersion("1.0.0");
        WorkerTaskData workerTaskData = new TestWorkerTaskData(classifier, 1, taskStatus, data, context, null, sourceInfo);

        TrackedDocument trackedDocument = new TrackedDocument(documentToClassify);
        TestWorkerConverterRuntimeImpl runtime = new TestWorkerConverterRuntimeImpl(trackedDocument, codec, dataStore, workerTaskData);

        converter.convert(runtime);

        return runtime.getDocument();
    }

    // Sub document RunConvert returning task data
    public static TaskData RunConvertWithBase(Object result, String ref, DataStore dataStore, TaskStatus taskStatus,
                                               PolicyWorkerConverterInterface converter, String classifier)
            throws PolicyWorkerConverterException, CodecException, InvalidTaskException
    {
        Codec codec = new JsonCodec();
        byte[] data;
        try {
            data = codec.serialise(result);
        } catch (CodecException e) {
            throw new RuntimeException("JsonProcessingException thrown while converting the test BoilerpalteWorkerResponse into byte array");
        }
        //create context
        DocumentInterface documentToClassify = createDocument(ref);

        TaskData taskData = new TaskData();
        taskData.setDocument((Document) documentToClassify);
        byte[] context;
        try {
            context = codec.serialise(taskData);
        } catch (CodecException e) {
            throw new RuntimeException("CodecException thrown while converting taskData for textDataTest into byte array");
        }

        TaskSourceInfo sourceInfo = new TaskSourceInfo();
        sourceInfo.setName("TestName");
        sourceInfo.setVersion("1.0.0");
        WorkerTaskData workerTaskData = new TestWorkerTaskData(classifier, 1, taskStatus, data, context, null, sourceInfo);

        TrackedDocument trackedDocument = new TrackedDocument(documentToClassify);
        PolicyWorkerConverterRuntimeBase baseRuntime = new TestConverterRuntimeBaseImpl(codec, dataStore, workerTaskData);

        TaskData returnedTaskData = converter.convert(baseRuntime);

        return returnedTaskData;
    }


    public static DocumentInterface RunConvert(byte[] data, byte[] context, ReferencedData documentReference, DataStore dataStore,
                                                  TaskStatus taskStatus, PolicyWorkerConverterInterface converter, String classifier)
            throws PolicyWorkerConverterException, CodecException, InvalidTaskException
    {
        Codec codec = new JsonCodec();
        //create context
        DocumentInterface documentToClassify = createDocument(documentReference.getReference());

        TaskSourceInfo sourceInfo = new TaskSourceInfo();
        sourceInfo.setName("TestName");
        sourceInfo.setVersion("1.0.0");
        WorkerTaskData workerTaskData = new TestWorkerTaskData(classifier, 1, taskStatus, data, context, null, sourceInfo);

        TrackedDocument trackedDocument = new TrackedDocument(documentToClassify);
        TestWorkerConverterRuntimeImpl runtime = new TestWorkerConverterRuntimeImpl(trackedDocument, codec, dataStore, workerTaskData);

        converter.convert(runtime);

        return runtime.getDocument();
    }


    public static DocumentInterface RunConvert(byte[] data, String reference, DataStore dataStore, TaskStatus taskStatus,
                                               PolicyWorkerConverterInterface converter, String classifier)
            throws PolicyWorkerConverterException, CodecException, InvalidTaskException
    {
        Codec codec = new JsonCodec();

        //create context
        DocumentInterface documentToClassify = createDocument(reference);

        TaskData taskData = new TaskData();
        taskData.setDocument((Document) documentToClassify);
        byte[] context;
        try {
            context = codec.serialise(taskData);
        } catch (CodecException e) {
            throw new RuntimeException("CodecException thrown while converting taskData for textDataTest into byte array");
        }

        TaskSourceInfo sourceInfo = new TaskSourceInfo();
        sourceInfo.setName("TestName");
        sourceInfo.setVersion("1.0.0");
        WorkerTaskData workerTaskData = new TestWorkerTaskData(classifier, 1, taskStatus, data, context, null, sourceInfo);

        TrackedDocument trackedDocument = new TrackedDocument(documentToClassify);
        TestWorkerConverterRuntimeImpl runtime = new TestWorkerConverterRuntimeImpl(trackedDocument, codec, dataStore, workerTaskData);

        converter.convert(runtime);

        return runtime.getDocument();
    }


    public static DocumentInterface RunConvert(Object bpResponse, String ref, DataStore dataStore,
                                               TaskStatus taskStatus, PolicyWorkerConverterInterface converter, String classifier,
                                               Multimap<String, String> metadataForDoc, Multimap<String, ReferencedData> metadataReferencesForDoc)
            throws CodecException, InvalidTaskException, PolicyWorkerConverterException
    {
        Codec codec = new JsonCodec();
        byte[] data;
        try {
            data = codec.serialise(bpResponse);
        }
        catch (CodecException e){
            throw new RuntimeException("JsonProcessingException thrown while converting the test BoilerpalteWorkerResponse into byte array");
        }
        //create context
        DocumentInterface documentToClassify = createDocument(ref);
        if (metadataForDoc != null && metadataForDoc.size() > 0) {
            documentToClassify.getMetadata().putAll(metadataForDoc);
        }
        if (metadataReferencesForDoc != null && metadataReferencesForDoc.size() > 0) {
            documentToClassify.getMetadataReferences().putAll(metadataReferencesForDoc);
        }

        TaskData taskData = new TaskData();
        taskData.setDocument((Document) documentToClassify);
        byte[] context;
        try {
            context = codec.serialise(taskData);
        } catch (CodecException e) {
            throw new RuntimeException("CodecException thrown while converting taskData for textDataTest into byte array");
        }

        TaskSourceInfo sourceInfo = new TaskSourceInfo();
        sourceInfo.setName("TestName");
        sourceInfo.setVersion("1.0.0");
        WorkerTaskData workerTaskData = new TestWorkerTaskData(classifier, 1, taskStatus, data, context, null, sourceInfo);

        TrackedDocument trackedDocument = new TrackedDocument(documentToClassify);
        TestWorkerConverterRuntimeImpl runtime = new TestWorkerConverterRuntimeImpl(trackedDocument, codec, dataStore, workerTaskData);

        converter.convert(runtime);

        return runtime.getDocument();
    }

    public static DocumentInterface RunConvert(Object result, DocumentInterface doc, DataStore dataStore, TaskStatus taskStatus,
                                               PolicyWorkerConverterInterface converter, String classifier)
            throws PolicyWorkerConverterException, CodecException, InvalidTaskException
    {
        Codec codec = new JsonCodec();
        byte[] data;
        try {
            data = codec.serialise(result);
        } catch (CodecException e) {
            throw new RuntimeException("JsonProcessingException thrown while converting the test BoilerpalteWorkerResponse into byte array");
        }

        TaskData taskData = new TaskData();
        taskData.setDocument((Document) doc);
        byte[] context;
        try {
            context = codec.serialise(taskData);
        } catch (CodecException e) {
            throw new RuntimeException("CodecException thrown while converting taskData for textDataTest into byte array");
        }

        TaskSourceInfo sourceInfo = new TaskSourceInfo();
        sourceInfo.setName("TestName");
        sourceInfo.setVersion("1.0.0");
        WorkerTaskData workerTaskData = new TestWorkerTaskData(classifier, 1, taskStatus, data, context, null, sourceInfo);

        TrackedDocument trackedDocument = new TrackedDocument(doc);
        TestWorkerConverterRuntimeImpl runtime = new TestWorkerConverterRuntimeImpl(trackedDocument, codec, dataStore, workerTaskData);

        converter.convert(runtime);

        return runtime.getDocument();
    }

    private static Document createDocument(String reference) {
        Document document = new Document();
        document.setDocuments(new ArrayList<Document>());
        document.setReference(reference);
        return document;
    }
}
