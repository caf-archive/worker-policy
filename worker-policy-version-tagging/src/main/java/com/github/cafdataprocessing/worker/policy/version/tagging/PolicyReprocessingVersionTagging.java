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
package com.github.cafdataprocessing.worker.policy.version.tagging;

import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;

import java.util.Collection;

/**
 * Ensures Document object has Worker version information added to it.
 */
public final class PolicyReprocessingVersionTagging {
    private static final String PROCESSING_FIELD_FORMAT_PREFIX = "PROCESSING_";
    private static final String PROCESSING_FIELD_FORMAT_SUFFIX = "_VERSION";

    private PolicyReprocessingVersionTagging(){}

    /**
     * Returns the field name of the worker version information based on the 'workerClassifier' passed in.
     * @param workerClassifier The classifier for a worker.
     * @return The name of the field that the 'workerClassifier' would map to.
     */
    public static String getProcessingFieldName(String workerClassifier){
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(PROCESSING_FIELD_FORMAT_PREFIX);
        strBuilder.append(workerClassifier);
        strBuilder.append(PROCESSING_FIELD_FORMAT_SUFFIX);
        return strBuilder.toString();
    }

    /**
     * Add worker information onto Document object as a field on the Document metadata.
     * @param document The Document to update with version information.
     * @param workerProcessingInfo WorkerProcessingInfo that will be used to construct new field on document.
     * @throws PolicyReprocessingVersionTaggingException
     */
    public static void addProcessingWorkerVersion(DocumentInterface document, WorkerProcessingInfo workerProcessingInfo) throws PolicyReprocessingVersionTaggingException {
        if(document==null){
            throw new PolicyReprocessingVersionTaggingException("'document' parameter cannot be null", new NullPointerException());
        }
        if(workerProcessingInfo==null){
            throw new PolicyReprocessingVersionTaggingException("'workerProcessingInfo' parameter cannot be null", new NullPointerException());
        }
        if(workerProcessingInfo.getWorkerVersion()==null){
            throw new PolicyReprocessingVersionTaggingException("workerVersion on 'workerProcessingInfo' parameter cannot be null", new NullPointerException());
        }
        if(workerProcessingInfo.getWorkerClassifier()==null){
            throw new PolicyReprocessingVersionTaggingException("workerClassifier on 'workerProcessingInfo' parameter cannot be null", new NullPointerException());
        }
        Multimap<String, String> metadata = document.getMetadata();
        if(metadata==null){
            throw new PolicyReprocessingVersionTaggingException("'getMetadata' on 'document' parameter returned null value", new NullPointerException());
        }
        String processingVersionFieldName = getProcessingFieldName(workerProcessingInfo.getWorkerClassifier());

        //check if field exists with this version number for this worker classifier
        Collection<String> workerClassifierField = metadata.get(processingVersionFieldName);
        if(workerClassifierField.contains(workerProcessingInfo.getWorkerVersion())){
            return;
        }
        metadata.put(processingVersionFieldName, workerProcessingInfo.getWorkerVersion());
    }
}
