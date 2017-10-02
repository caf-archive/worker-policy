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
package com.github.cafdataprocessing.worker.policy.composite.document.converter;

import com.github.cafdataprocessing.worker.policy.common.DocumentFields;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterException;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterInterface;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterRuntime;
import com.github.cafdataprocessing.worker.policy.converters.ConverterUtils;
import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.google.common.collect.Multimap;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.document.*;
import org.apache.commons.codec.binary.Base64;

import java.util.List;
import java.util.Map;

/**
 * Class to handle updating a document based Diffgram from document worker.
 */
public class CompositeDocumentWorkerConverter implements PolicyWorkerConverterInterface
{
    @Override
    public void updateSupportedClassifierVersions(Multimap<String, Integer> multimap)
    {
        multimap.put(DocumentWorkerConstants.DOCUMENT_TASK_NAME, DocumentWorkerConstants.DOCUMENT_TASK_API_VER);
    }

    @Override
    public void convert(PolicyWorkerConverterRuntime runtime) throws PolicyWorkerConverterException,
                                                                     CodecException, InvalidTaskException
    {
        DocumentWorkerDocumentTask taskResponse = runtime.deserialiseData(DocumentWorkerDocumentTask.class);

        if (taskResponse == null) {
            runtime.recordError("NULL_PTR", "DocumentWorkerDocumentTask is null");
            return;
        }

        if (taskResponse.changeLog != null) {
            CompositeDocumentWorkerTaskUpdater.updateDocument(runtime.getDocument(), taskResponse.changeLog);
        }
    }

    @Override
    public TaskData convertPolicyWorkerContext(final byte[] context, final byte[] taskData, final Codec codec) throws CodecException {
        DocumentWorkerDocumentTask workerResponse = codec.deserialise(taskData, DocumentWorkerDocumentTask.class);
        TaskData constructedPolicyWorkerTask = codec.deserialise(context, TaskData.class);
        DocumentWorkerDocument originalDocument = workerResponse.document;
        // add the metadata from the original document to the policy worker task data document
        Document policyWorkerDoc = constructedPolicyWorkerTask.getDocument();

        // add metadata and metadata references
        DocumentConversionUtils.addFields(originalDocument.fields, policyWorkerDoc);
        //add sub-documents
        for(DocumentWorkerDocument subDoc: originalDocument.subdocuments){
            DocumentConversionUtils.addSubDocument(subDoc, policyWorkerDoc);
        }
        // policy worker document on the constructed task data should now have the policy worker progress fields from the
        // context combined with the metadata, metadata references and sub-documents that were on the document before it
        // was sent to the composite document worker.
        return constructedPolicyWorkerTask;
    }

    /**
     * Specialized implementation of context generation method for composite document converter. This converter should have all document
     * (and sub-documents) fields available in the worker-document Document of the DocumentWorkerDocumentTask. Thus it is not necessary
     * to pass those fields in the context. Only pass those not included on the Document, such as Policy Worker temporary fields.
     * @param taskData Policy worker task data representing current processing progress.
     * @param codec Codec to use in serializing context.
     * @return Built policy worker context for use in reconstructing policy worker progress when result message is
     * passed to this converter.
     * @throws CodecException If an exception occurs serializing the constructed context.
     */
    @Override
    public byte[] generatePolicyWorkerContext(final TaskData taskData, final Codec codec) throws CodecException {
        TaskData newTaskData = new TaskData();
        newTaskData.setOutputPartialReference(taskData.getOutputPartialReference());
        newTaskData.setProjectId(taskData.getProjectId());
        newTaskData.setCollectionSequence(taskData.getCollectionSequences());
        newTaskData.setExecutePolicyOnClassifiedDocuments(taskData.isExecutePolicyOnClassifiedDocument());
        newTaskData.setPoliciesToExecute(taskData.getPoliciesToExecute());
        newTaskData.setWorkflowId(taskData.getWorkflowId());

        Document originalDocument = taskData.getDocument();
        Document reducedDocument = new Document();
        reducedDocument.setReference(originalDocument.getReference());
        Multimap<String, String> reducedDocumentMetadata = reducedDocument.getMetadata();
        //retain the temporary policy worker metadata fields only
        Multimap<String, String> temporaryWorkingDataFields = DocumentFields.getTemporaryWorkingData(originalDocument);
        for(Map.Entry<String, String> fieldEntry: temporaryWorkingDataFields.entries()){
            reducedDocumentMetadata.put(fieldEntry.getKey(), fieldEntry.getValue());
        }
        newTaskData.setDocument(reducedDocument);
        return codec.serialise(newTaskData);
    }
}
