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
package com.github.cafdataprocessing.worker.policy.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class TaskData {
    /**
     * The project id for the user
     */
    private String projectId;

    /**
     * The collection sequence id or collection sequence name to classify documents against or executePolicyOnClassifiedDocument against
     */
    private List<String> collectionSequences = new ArrayList<>();

    /**
     * The document to classify or execute on
     */
    private Document document;

    /**
     * Execute the resolved policies on the document classified
     */
    private boolean executePolicyOnClassifiedDocument;

    /**
     * The document to executePolicyOnClassifiedDocuments policy on
     */
    private Collection<Long> policiesToExecute;

    /**
     * The partial reference for the data store
     */
    private String outputPartialReference;

    /**
     * The workflow id to classify document against or execute policy on classified document against.
     */
    private String workflowId;

    public List<String> getCollectionSequences() {
        return collectionSequences;
    }

    public void setCollectionSequence(List<String> collectionSequences) {
        this.collectionSequences = collectionSequences;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public boolean isExecutePolicyOnClassifiedDocument() {
        return executePolicyOnClassifiedDocument;
    }

    public void setExecutePolicyOnClassifiedDocuments(boolean executePolicyOnClassifiedDocuments) {
        this.executePolicyOnClassifiedDocument = executePolicyOnClassifiedDocuments;
    }

    public Collection<Long> getPoliciesToExecute() {
        return policiesToExecute;
    }

    public void setPoliciesToExecute(Collection<Long> policiesToExecute) {
        this.policiesToExecute = policiesToExecute;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getOutputPartialReference() {
        return outputPartialReference;
    }

    public void setOutputPartialReference(String outputPartialReference) {
        this.outputPartialReference = outputPartialReference;
    }
}