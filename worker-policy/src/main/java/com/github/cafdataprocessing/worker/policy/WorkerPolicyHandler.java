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
package com.github.cafdataprocessing.worker.policy;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.cafdataprocessing.worker.policy.common.DocumentFields;
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.corepolicy.PolicyHandler;
import com.github.cafdataprocessing.corepolicy.ProcessingAction;
import com.github.cafdataprocessing.corepolicy.api.ApiProvider;
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.dto.ConflictResolutionMode;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.TrackedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.*;

import static com.github.cafdataprocessing.worker.policy.common.DocumentFields.removeTemporaryWorkingData;

/**
 * Base class for Policy Handlers.
 */
public abstract class WorkerPolicyHandler implements PolicyHandler {

    private long policyTypeId = -1;
    protected ApplicationContext applicationContext;
    protected static Logger logger = LoggerFactory.getLogger(WorkerPolicyHandler.class);

    public WorkerPolicyHandler() {

    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public ProcessingAction handle(Document document, Policy policy, Long collectionSequenceID) {

        // Before we call any policy handlers, ensure all temporary fields we know about and use, are removed
        // from the document. This is so it doesn't end up being used for anything other than context.
        Multimap<String, String> temporaryData = removeTemporaryWorkingData(document);

        ProcessingAction processingAction = null;
        // has to be wrapped in a finally block, to ensure context we removed, is put back onto the doc.
        try {
            processingAction = handlePolicy(document, policy, collectionSequenceID);
        } catch (InvalidTaskException e) {
            throw new RuntimeException(e);
        } finally {
            DocumentFields.reapplyTemporaryWorkingData(document, temporaryData);
        }

        if (processingAction == ProcessingAction.STOP_PROCESSING) {
            WorkerResponseHolder workerResponseHolder = this.applicationContext.getBean(WorkerResponseHolder.class);
            workerResponseHolder.setShouldStop(true);
        }

        // add the policy we have just executed to the document as temporary context information.
        DocumentFields.addExecutedPolicyInfoToDocument(collectionSequenceID, Arrays.asList(policy.id), document);

        return processingAction;
    }

    /**
     * Gets the policy type required for this Handler
     * @return the policy type
     */
    public abstract PolicyType getPolicyType();

    public final long getPolicyTypeId() {
        if (policyTypeId == -1){
            throw new RuntimeException("Policy Type should not be -1 for a WorkerPolicyHandler");
        }
        return policyTypeId;
    }

    public final void setPolicyTypeId(long policyTypeId) {
        this.policyTypeId = policyTypeId;
    }

    /**
     * Constructs a new set representing the key names contained in the passed in metadata at the time the method was called.
     * @param metadata Metadata to pull keys from.
     * @return Set of key names present on the metadata
     */
    private static Set<String> getMetadataKeysAsSet(Multimap<String, String> metadata){
        final Set<String> metadataKeys = new HashSet<>();
        metadata.keySet().forEach(e -> metadataKeys.add(e));
        return metadataKeys;
    }

    /**
     * Method that merges the meta data from the CorePolicy document returned from classification onto the
     * Policy Worker's task data.
     *
     * @param taskData The worker's task data to merge onto.
     * @param corepolicyDocument The CorePolicy document containing the meta data to merge.
     * @return TaskData with updated meta data.
     */
    public static TaskData updateTaskDataDocument(TaskData taskData, Document corepolicyDocument) {
        com.github.cafdataprocessing.worker.policy.shared.Document workerDocument = taskData.getDocument();
        if (workerDocument != null) {
            //Remove temporary data to avoid recording those fields as added
            Multimap<String, String> corePolicyTempData = DocumentFields.removeTemporaryWorkingData(corepolicyDocument);
            DocumentFields.removeTemporaryWorkingData(workerDocument);

            //Create a TrackedDocument here to enable the tracking of any meta data changes instead of just
            //replacing with the meta data returned from CorePolicy.
            DocumentInterface trackedWorkerDocument = new TrackedDocument(taskData.getDocument());

            final Set<String> workerDocumentKeys = getMetadataKeysAsSet(trackedWorkerDocument.getMetadata());
            final Set<String> corePolicyDocumentKeys = getMetadataKeysAsSet(corepolicyDocument.getMetadata());

            for (String workerDocumentKey : workerDocumentKeys) {
                List<String> workerEntry = new ArrayList<>(trackedWorkerDocument.getMetadata().get(workerDocumentKey));
                List<String> corePolicyEntry = new ArrayList<>(corepolicyDocument.getMetadata().get(workerDocumentKey));

                //CorePolicy document does contain this entry, so add any values that are new and remove any removed.
                if (!corePolicyEntry.isEmpty()) {
                    //loop over the workflow document, removing any values the corepolicy document doesn't have.
                    int collectionSize = workerEntry.size();
                    for (int i = 0; i < collectionSize; i++) {
                        if(!corePolicyEntry.contains(workerEntry.get(i))){
                            trackedWorkerDocument.getMetadata().remove(workerDocumentKey,workerEntry.get(i));
                        }
                    }
                    //loop over the corepolicy document, adding any missing values to the workflow document.
                    collectionSize = corePolicyEntry.size();
                    for (int i = 0; i < collectionSize; i++) {
                        if (!workerEntry.contains(corePolicyEntry.get(i))) {
                            trackedWorkerDocument.getMetadata().put(workerDocumentKey,corePolicyEntry.get(i));
                        }
                    }
                    //remove this key from the corepolicy document to reduce the second loop
                    corePolicyDocumentKeys.remove(workerDocumentKey);
                } else { //Corepolicy has removed this field, so delete from worker document.
                    trackedWorkerDocument.getMetadata().removeAll(workerDocumentKey);
                }
            }

            //Corepolicy document now only has fields the worker document is missing so add all of them.
            for (String corePolicyKey : corePolicyDocumentKeys){
                if(!trackedWorkerDocument.getMetadata().containsKey(corePolicyKey)){
                    trackedWorkerDocument.getMetadata().putAll(corePolicyKey, corepolicyDocument.getMetadata().get(corePolicyKey));
                }
            }
            //put back on temp data without tracking it (including adding any new temp data)
            DocumentFields.reapplyTemporaryWorkingData(corepolicyDocument,corePolicyTempData);
            workerDocument.getMetadata().putAll(corePolicyTempData);
        }
        return taskData;
    }

    protected abstract ProcessingAction handlePolicy(Document document, Policy policy, Long collectionSequenceID) throws InvalidTaskException;

    protected ApiProvider getApiProvider()
    {
        return applicationContext.getBean(ApiProvider.class);
    }

    protected PolicyType createEmptyDefinitionPolicyType(String name, String shortName)
    {
        PolicyType policyType = new PolicyType();
        policyType.conflictResolutionMode = ConflictResolutionMode.PRIORITY;
        JsonNodeFactory nodeFactory = new JsonNodeFactory(false);
        policyType.definition = nodeFactory.objectNode();
        policyType.name = name;
        policyType.shortName = shortName;
        return policyType;
    }

    protected static <T> T loadWorkerHandlerProperties(Class<T> propertiesClass)
    {
        AnnotationConfigApplicationContext propertiesApplicationContext = new AnnotationConfigApplicationContext();
        propertiesApplicationContext.register(PropertySourcesPlaceholderConfigurer.class);
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(propertiesClass);
        propertiesApplicationContext.registerBeanDefinition(propertiesClass.getSimpleName(), beanDefinition);
        propertiesApplicationContext.refresh();
        return propertiesApplicationContext.getBean(propertiesClass);
    }

}
