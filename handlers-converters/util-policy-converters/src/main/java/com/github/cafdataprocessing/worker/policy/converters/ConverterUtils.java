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
package com.github.cafdataprocessing.worker.policy.converters;

import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.TrackedDocument;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.google.common.collect.Multimap;
import com.hpe.caf.util.ref.ReferencedData;

/**
 * Utility methods for the Policy Worker Converters.
 */
public class ConverterUtils {


    public static void addMetadataToDocument(String name, String value, TaskData taskData) {
        DocumentInterface document = getTrackedDocument(taskData);
        addMetadataToDocument(name, value, document);
    }

    public static void addMetadataToDocument(String name, String value, DocumentInterface document) {
        if (document != null && !document.getMetadata().containsEntry(name, value)) {
            document.getMetadata().put(name, value);
        }
    }

    public static void addMetadataReferenceToDocument(String name, ReferencedData value, TaskData taskData) {
        DocumentInterface document = getTrackedDocument(taskData);
        addMetadataReferenceToDocument(name, value, document);

    }

    public static void addMetadataReferenceToDocument(String name, ReferencedData value, DocumentInterface document) {
        if (document != null) {
            document.getMetadataReferences().put(name, value);
        }
    }

    public static void removeMetadataFromDocument(String name, TaskData taskData) {
        DocumentInterface document = getTrackedDocument(taskData);
        removeMetadataFromDocument(name, document);
    }

    public static void removeMetadataFromDocument(String name, DocumentInterface document) {
        if (document != null) {
            document.getMetadata().removeAll(name);
        }
    }

    public static void removeMetadataReferenceFromDocument(String name, TaskData taskData) {
        DocumentInterface document = getTrackedDocument(taskData);
        removeMetadataReferenceFromDocument(name, document);
    }

    public static void removeMetadataReferenceFromDocument(String name, DocumentInterface document) {
        if (document != null) {
            document.getMetadataReferences().removeAll(name);
        }
    }

    public static void removeMetadataFromDocument(String name, String value, TaskData taskData) {
        DocumentInterface document = getTrackedDocument(taskData);
        removeMetadataFromDocument(name, value, document);
    }

    public static void removeMetadataFromDocument(String name, String value, DocumentInterface document) {
        if (document != null) {
            document.getMetadata().remove(name, value);
        }
    }

    /**
     * Check if the metadata on the passed document contains a field with the passed name on its metadata
     * @param fieldName The name of the field to check for.
     * @param document The document whose metadata will be inspected.
     * @return Whether the document contains a field with the name in.
     */
    public static boolean checkMetadataOnDocumentContains(String fieldName, DocumentInterface document){
        if(document==null){
            return false;
        }
        Multimap<String, String> metadata = document.getMetadata();
        if(metadata==null || metadata.isEmpty()){
            return false;
        }
        return metadata.containsKey(fieldName);
    }

    /**
     * Check if the metadata on the passed document contains a field with the passed name and the passed value on its
     * metadata.
     * @param fieldName The name of the field to check.
     * @param fieldValue The value that is expected to be in the field.
     * @param document   The document whose metadata will be inspected.
     * @return Whether the document contains a field with the name and value passed in.
     */
    public static boolean checkMetadataOnDocumentContains(String fieldName, String fieldValue, DocumentInterface document){
        if(document==null){
            return false;
        }
        Multimap<String, String> metadata = document.getMetadata();
        if(metadata==null || metadata.isEmpty() || !metadata.containsKey(fieldName)){
            return false;
        }
        return metadata.containsEntry(fieldName, fieldValue);
    }


    public static DocumentInterface getTrackedDocument(TaskData taskData) {
        return new TrackedDocument(taskData.getDocument());
    }
}
