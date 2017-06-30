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
package com.github.cafdataprocessing.worker.policy;

import com.github.cafdataprocessing.worker.policy.common.DocumentFields;
import com.github.cafdataprocessing.worker.policy.converters.ClassifyDocumentResultConverter;
import com.github.cafdataprocessing.worker.policy.converters.DocumentConverter;
import com.google.common.base.Strings;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.corepolicy.api.ApiProvider;
import com.github.cafdataprocessing.corepolicy.api.ClassifyDocumentApiDirectImpl;
import com.github.cafdataprocessing.corepolicy.api.CollectionSequenceIdExtractor;
import com.github.cafdataprocessing.corepolicy.api.CollectionSequenceIdExtractorImpl;
import com.github.cafdataprocessing.corepolicy.common.*;
import com.github.cafdataprocessing.corepolicy.common.dto.*;
import com.github.cafdataprocessing.corepolicy.common.exceptions.BackEndRequestFailedCpeException;
import com.github.cafdataprocessing.corepolicy.common.exceptions.InvalidFieldValueCpeException;
import com.github.cafdataprocessing.worker.policy.shared.ClassifyDocumentResult;
import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.worker.policy.shared.PolicyWorkerConstants;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.TrackedDocument;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.codec.JsonLzfCodec;
import com.hpe.caf.util.ref.DataSourceException;
import com.hpe.caf.util.ref.SourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Main logic class for handling executing against a task data object.
 */
public class ExecuteTaskData {
    private final CorePolicyApplicationContext corePolicyApplicationContext;
    private final DataStoreSource dataStoreSource;
    private final LoadingCache<Long, SequenceWorkflow> workflowCache;
    private static Logger logger = LoggerFactory.getLogger(ExecuteTaskData.class);
    private final ClassifyDocumentApi classifyDocumentApi;
    private final PolicyApi policyApi;
    private final PolicyWorker policyWorker;
    private final DocumentConverter documentConverter;
    private static List<String> registeredHandlers = new ArrayList<>();
    private static List<String> registeredUsers = new ArrayList<>();
    private ClassifyDocumentResultConverter classifyDocumentResultConverter;

    public ExecuteTaskData(CorePolicyApplicationContext corePolicyApplicationContext, PolicyWorker worker,
                           DataStoreSource dataStoreSource,
                           LoadingCache<Long, SequenceWorkflow> workflowCache, ApiProvider apiProvider) {
        this.corePolicyApplicationContext = corePolicyApplicationContext;
        this.dataStoreSource = dataStoreSource;
        this.workflowCache = workflowCache;
        this.classifyDocumentApi = apiProvider.getClassifyDocumentApi();
        this.policyApi = apiProvider.getPolicyApi();
        this.policyWorker = Objects.requireNonNull(worker);
        this.documentConverter = new DocumentConverter();
        this.classifyDocumentResultConverter = new ClassifyDocumentResultConverter();
    }

    public WorkerResponse execute(TaskData taskData) throws InvalidTaskException, TaskRejectedException {
        registerHandlers(taskData.getProjectId());

        logger.info("Executing for taskdata");
        WorkerResponseHolder workerResponseHolder = corePolicyApplicationContext.getBean(WorkerResponseHolder.class);
        //clear out any previous worker response that may have been set on this thread
        workerResponseHolder.clear();
        workerResponseHolder.setTaskData(taskData);

        UserContext userContext = corePolicyApplicationContext.getBean(UserContext.class);
        userContext.setProjectId(taskData.getProjectId());

        //if both workflow id and collection ids passed then workflow id supersedes and collection ids are ignored
        List<Long> collectionSequenceIds;
        String workflowId = taskData.getWorkflowId();
        if (workflowId != null) {
            Long id = Long.parseLong(workflowId);
            SequenceWorkflow workflow = null;
            try {
                workflow = workflowCache.get(id);
            }
            catch (ExecutionException e) {
                logger.error("Could not retrieve workflow from cache. Workflow id: " + id, e);
                throw new TaskRejectedException("Could not retrieve workflow from cache. Workflow id: " + id, e);
            }
            collectionSequenceIds = workflow.sequenceWorkflowEntries.stream()
                    .sorted((wEnt1, wEnt2) -> Short.compare(wEnt1.order, wEnt2.order))
                    .map(w -> w.collectionSequenceId)
                    .collect(Collectors.toList());
        } else {
            collectionSequenceIds = getCollectionSequenceIds(taskData.getCollectionSequences());
        }

        if (collectionSequenceIds.isEmpty()) {
            return getOrCreateResponse(new ArrayList<>());
        }

        Collection<ClassifyDocumentResult> classifyDocumentResults = new ArrayList<>();

        if (taskData.getPoliciesToExecute() != null) {
            executeDocument(collectionSequenceIds, taskData.getDocument(), taskData.getPoliciesToExecute());
        }
        if (taskData.getPoliciesToExecute() == null) {
            logger.trace("Classifying documents");

            classifyDocumentResults.addAll(classifyAndPossiblyExecuteDocument(collectionSequenceIds, taskData.getDocument(), taskData.isExecutePolicyOnClassifiedDocument()));
            logger.trace(classifyDocumentResults.toString());
        }

        return getOrCreateResponse(classifyDocumentResults);
    }

    /***
     * Create our policy handler response, to a normal classify or execution.
     * For more advanced executions which use chaining, we will have already created a WorkerResponse earlier which
     * consisted of a newtask to be sent on to another queue.
     *
     * @param classifyDocumentResults
     * @return
     * @throws WorkerException
     */
    private WorkerResponse getOrCreateResponse(Collection<ClassifyDocumentResult> classifyDocumentResults) {

        WorkerResponseHolder workerResponseHolder;
        workerResponseHolder = corePolicyApplicationContext.getBean(WorkerResponseHolder.class);
        if (workerResponseHolder.getWorkerResponse() != null) {

            return workerResponseHolder.getWorkerResponse();
        }
        return policyWorker.createSuccessResultCallback(PolicyWorker.createResultObject(classifyDocumentResults));
    }

    private List<Long> getCollectionSequenceIds(List<String> collectionSequence) {
        if (collectionSequence == null) {
            return null;
        }
        CollectionSequenceIdExtractor collectionSequenceIdExtractor = new CollectionSequenceIdExtractorImpl(corePolicyApplicationContext);
        return collectionSequence.stream().map(collectionSequenceIdExtractor::getCollectionSequenceId).collect(Collectors.toList());
    }

    private void executeDocument(List<Long> collectionSequenceIds, Document documentToExecutePolicyOn, Collection<Long> policiesToExecute) {

        try (com.github.cafdataprocessing.corepolicy.common.Document corePolicyDoc = new DocumentConverter().convert(documentToExecutePolicyOn, dataStoreSource)) {

            if (checkRetryPreviousFailureWithDiagnostics(collectionSequenceIds, corePolicyDoc, documentToExecutePolicyOn, policiesToExecute)) {
                return;
            }


            for (Long collectionSequenceId : collectionSequenceIds) {
                //clear out any previous worker response
                corePolicyApplicationContext.getBean(WorkerResponseHolder.class).clear();
                corePolicyApplicationContext.getBean(WorkerRequestHolder.class).clear();

                Collection<Long> policiesYetToBeExecuted = getPoliciesYetToBeExecuted(corePolicyDoc, collectionSequenceId, policiesToExecute);
                if (policiesYetToBeExecuted.size() == 0) {
                    logger.debug("All policies have already been executed for sequence" + collectionSequenceId);
                } else {

                    com.github.cafdataprocessing.corepolicy.common.dto.DocumentToExecutePolicyOn completeDocToExecute =
                            new com.github.cafdataprocessing.corepolicy.common.dto.DocumentToExecutePolicyOn();
                    completeDocToExecute.document = corePolicyDoc;
                    completeDocToExecute.policyIds = policiesYetToBeExecuted;

                    com.github.cafdataprocessing.corepolicy.common.Document executedDocument = classifyDocumentApi.execute(collectionSequenceId, completeDocToExecute);

                    // Before we exit from here, make a final decision about whether we have finished executing all policies
                    // for a given collection sequence - this prevents it classifying this sequence again in the future.
                    if (getPoliciesYetToBeExecuted(executedDocument, collectionSequenceId, policiesToExecute).size() == 0) {
                        // we have executed all policies add on that this sequence is complete.
                        DocumentFields.addCollectionSequenceCompletedInfo(executedDocument, collectionSequenceId);
                    }

                    if(updateTaskDataAndCheckIfShouldCreateChainedWorkerResponse(executedDocument)){
                        return;
                    }

                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * @param executedDocument A Corepolicy document that has been executed
     * @return True if should stop processing
     */
    private boolean updateTaskDataAndCheckIfShouldCreateChainedWorkerResponse(com.github.cafdataprocessing.corepolicy.common.Document executedDocument){

        WorkerResponseHolder workerResponseHolder = corePolicyApplicationContext.getBean(WorkerResponseHolder.class);
        Objects.requireNonNull(workerResponseHolder);

        TaskData taskData = workerResponseHolder.getTaskData();
        Objects.requireNonNull(taskData);

        //update taskData with current document
        taskData = WorkerPolicyHandler.updateTaskDataDocument(taskData, executedDocument);

        if (!shouldContinueProcessing()) {

            createChainedWorkerResponse(taskData, workerResponseHolder);
            return true;
        }

        return false;
    }

    private void createChainedWorkerResponse(TaskData taskData, WorkerResponseHolder workerResponseHolder) {

        // get the actual WorkerHandlerResponse for a complex chained policy reponse, and use it to create the
        // full WorkerResponse object.
        WorkerTaskResponsePolicyHandler.WorkerHandlerResponse workerHandlerResponse = workerResponseHolder.getChainWorkerResponse();
        if (workerHandlerResponse == null) {
            return;
        }

        this.policyWorker.getTaskDataConverter().updateObjectWithTaskData(workerHandlerResponse.getMessageType(),
                workerHandlerResponse.getApiVersion(), workerHandlerResponse.getData(), taskData);

        JsonLzfCodec policyCodec = new JsonLzfCodec();

        WorkerResponse workerResponse;
        try {
            workerResponse = new WorkerResponse(workerHandlerResponse.getQueueReference(),
                    workerHandlerResponse.getTaskStatus(),
                    policyCodec.serialise(workerHandlerResponse.getData()),
                    workerHandlerResponse.getMessageType(),
                    workerHandlerResponse.getApiVersion(),
                    policyCodec.serialise(taskData)
            );
        } catch (CodecException e) {
            throw new BackEndRequestFailedCpeException(e);
        }
        workerResponseHolder.setWorkerResponse(workerResponse);
    }

    private boolean shouldContinueProcessing() {
        WorkerResponseHolder bean = corePolicyApplicationContext.getBean(WorkerResponseHolder.class);
        return !bean.isShouldStop();
    }

    private Collection<ClassifyDocumentResult> classifyAndPossiblyExecuteDocument(List<Long> collectionSequenceIds, Document document, boolean execute) throws InvalidTaskException, TaskRejectedException {
        DocumentConverter documentConverter = new DocumentConverter();

        List<ClassifyDocumentResult> classifyDocumentResults = new ArrayList<>();

        // creating a single corepolicy document, to use across both methods, to avoid creation of it multiple times
        try (com.github.cafdataprocessing.corepolicy.common.Document doc = documentConverter.convert(document, dataStoreSource)) {

            // check if the previous policy failed, if it did, try to handle the previous policy with diagnostics
            // handling enabled.
            if (checkRetryPreviousFailureWithDiagnostics(collectionSequenceIds, doc, document, null)) return classifyDocumentResults;

            // Go through each of the collection sequences that we have been asked to evaluate,
            // skip any that we have already fully completed, or
            // work out which policies are yet to be executed, and pick up the next one in the list.
            if (evaluateAndExecuteNextPolicy(collectionSequenceIds, execute, classifyDocumentResults, doc))
                return classifyDocumentResults;

        } catch (RuntimeException e) {
            if (e.getCause() instanceof SourceNotFoundException) {
                throw new InvalidTaskException(e.getMessage(), e.getCause());
            } else if (e.getCause() instanceof DataSourceException) {
                //  Cannot determine source of DataSourceException.
                throw new TaskRejectedException(e.getMessage(), e.getCause());
            } else {
                throw e;
            }
        } catch (InvalidTaskException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return classifyDocumentResults;
    }


    private boolean checkRetryPreviousFailureWithDiagnostics(List<Long> collectionSequenceIds,
                                                             com.github.cafdataprocessing.corepolicy.common.Document doc,
                                                             Document policyWorkerDocument, Collection<Long> policiesToExecute) {
        // if we have failure flag work out the last collection sequence and policy being executed
        Multimap<String, String> documentMetadata = doc.getMetadata();
        if (!documentMetadata.containsKey(PolicyWorkerConstants.POLICYWORKER_FAILURE)) {
            // doc metadata contains no previous failure status, as such just exit and continue on as normal.
            return false;
        }

        corePolicyApplicationContext.getBean(WorkerResponseHolder.class).clear();
        corePolicyApplicationContext.getBean(WorkerRequestHolder.class).clear();

		// As the corepolicy document object has already been created, any changes we need to make to the main trackedDocument, we also need to make
		// to the corepolicy document we hold here, to ensure that it doesn't re-appear after we call the worker!
        TrackedDocument trackedDocument = new TrackedDocument(policyWorkerDocument);

        // Always remove the global failure field first, just incase something throws we do not want a previous failure reason being
        // left behind on the document this would result in it being applied to another policy.
        String failureReason = documentMetadata.get(PolicyWorkerConstants.POLICYWORKER_FAILURE).stream().findFirst().get();
        doc.getMetadata().removeAll(PolicyWorkerConstants.POLICYWORKER_FAILURE);

        //remove the failure from the policyWorkerDocument metadata, but we have to make sure any changes to this field are tracked, so that they
        // do not persist on the document during reprocessing.
        trackedDocument.getMetadata().removeAll(PolicyWorkerConstants.POLICYWORKER_FAILURE);

        // Always remove the global failure error code field first, just incase something throws we do not want a previous failure error reason being
        // left behind on the document this would result in it being applied to another policy.
        Optional<String> failureErrorCodeReasonOptional = documentMetadata.get(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_CODE).stream().findFirst();
        String failureErrorCodeReason = null;
        if (failureErrorCodeReasonOptional.isPresent()) {
            failureErrorCodeReason = failureErrorCodeReasonOptional.get();

            //remove the failure error code from the policyWorkerDocument metadata, but we have to make sure any changes to this field are tracked, so that they
            // do not persist on the document during reprocessing.
            doc.getMetadata().removeAll(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_CODE);
            trackedDocument.getMetadata().removeAll(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_CODE);
        }

        //Similarly remove the global failure error message if present.
        Optional<String> failureErrorMessageOptional = documentMetadata.get(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_MESSAGE).stream().findFirst();
        String failureErrorMessage = null;
        if (failureErrorMessageOptional.isPresent()) {
            failureErrorMessage = failureErrorMessageOptional.get();

            //remove the failure error messsage from the policyWorkerDocument metadata, but make sure any changes to this field are tracked, so that they
            // do not persist on the document during reprocessing.
            doc.getMetadata().removeAll(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_MESSAGE);
            trackedDocument.getMetadata().removeAll(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_MESSAGE);
        }

        // If the task returned had a status of INVALID_TASK then we do not want to retry with the diagnostics workers
        if (isInvalidTask(failureReason)) {
            return false;
        }

        CollectionSequenceIdToPolicyId collectionSequenceAndPolicyToRerun = previousCollectionSequenceAndPolicyRan(collectionSequenceIds, doc, policiesToExecute);

        if ( collectionSequenceAndPolicyToRerun == null ) {
            // let it continue to execute as normal as we found no previously executed policy or diagnostics has already been ran on the message if ( policyWorkerDocument.getMetadata().keySet().stream().filter(s -> s.startsWith(PolicyWorkerConstants.POLICYWORKER_FAILURE_POLICY)).findFirst().isPresent() )
            return false;
        }

        // Record the global field's failure to policyworker_failure_<id>=<failure>  ( move ensures we dont have the global failure left around ).
        String policyFailureIdName = PolicyWorkerConstants.POLICYWORKER_FAILURE_POLICY + collectionSequenceAndPolicyToRerun.getPolicyId();
		documentMetadata.removeAll(policyFailureIdName);
        documentMetadata.put(policyFailureIdName, failureReason);
		trackedDocument.getMetadata().removeAll(policyFailureIdName);
        trackedDocument.getMetadata().put(policyFailureIdName, failureReason);

        // only if we have a POLICYWORKER_FAILURE_ERROR_CODE global field, should we add it on.
        if (!Strings.isNullOrEmpty(failureErrorCodeReason ) ) {
            // Record the global failure error code field to policyworker_failure_error_code_policy_<id>=<failureErrorCode>  ( move ensures we dont have the global failure error code left around ).
            String failureErrorCodePolicyIdFieldName = PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_CODE_POLICY + collectionSequenceAndPolicyToRerun.getPolicyId();
            documentMetadata.removeAll(failureErrorCodePolicyIdFieldName);
            documentMetadata.put(failureErrorCodePolicyIdFieldName, failureErrorCodeReason);
            trackedDocument.getMetadata().removeAll(failureErrorCodePolicyIdFieldName);
            trackedDocument.getMetadata().put(failureErrorCodePolicyIdFieldName, failureErrorCodeReason);
        }

        // and only if we have a POLICYWORKER_FAILURE_ERROR_MESSAGE global field, should we add a Policy specific field for it
        if(!Strings.isNullOrEmpty(failureErrorMessage)){
            //Record the global failure error message field to policy worker failure error message field with value set to policyworker_failure_error_message_policy_<id>=<failureErrorMessage>
            String failureErrorMessagePolicyIdFieldName = PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_MESSAGE_POLICY + collectionSequenceAndPolicyToRerun.getPolicyId();
            documentMetadata.removeAll(failureErrorMessagePolicyIdFieldName);
            documentMetadata.put(failureErrorMessagePolicyIdFieldName, failureErrorMessage);
            trackedDocument.getMetadata().removeAll(failureErrorMessagePolicyIdFieldName);
            trackedDocument.getMetadata().put(failureErrorMessagePolicyIdFieldName, failureErrorMessage);
        }

        WorkerRequestHolder workerRequestHolder = this.corePolicyApplicationContext.getBean(WorkerRequestHolder.class);
        WorkerRequestInfo workerRequestInfo = new WorkerRequestInfo();
        workerRequestInfo.useDiagnostics = true;
        workerRequestHolder.setWorkerRequestInfo( workerRequestInfo );

        com.github.cafdataprocessing.corepolicy.common.dto.DocumentToExecutePolicyOn documentToExecutePolicyOn =
                new com.github.cafdataprocessing.corepolicy.common.dto.DocumentToExecutePolicyOn();
        documentToExecutePolicyOn.document = doc;
        documentToExecutePolicyOn.policyIds = Arrays.asList(collectionSequenceAndPolicyToRerun.getPolicyId());

        // Call handler of the policy
        com.github.cafdataprocessing.corepolicy.common.Document executedDocument =
                classifyDocumentApi.execute(collectionSequenceAndPolicyToRerun.getCollectionSequenceId(), documentToExecutePolicyOn);

        return updateTaskDataAndCheckIfShouldCreateChainedWorkerResponse(executedDocument);
    }

    /**
     * Checks whether the specified failure message indicates that the worker failed with an INVALID_TASK status.
     * @param failureMessage The message to be tested
     * @return Whether the message indicates that the task status was INVALID_TASK
     */
    private static boolean isInvalidTask(final String failureMessage)
    {
        return (failureMessage != null)
            && (failureMessage.contains(TaskStatus.INVALID_TASK.name()));
    }

    private CollectionSequenceIdToPolicyId previousCollectionSequenceAndPolicyRan(List<Long> collectionSequenceIds,
                                                                                  com.github.cafdataprocessing.corepolicy.common.Document doc,
                                                                                  Collection<Long> policiesToExecute) {

        List<Long> sequencesAlreadyCompleted = DocumentFields.getCollectionSequencesAlreadyCompleted(doc);
        List<Long> sequencesAlreadyStarted = DocumentFields.getCollectionSequencesAlreadyStarted(doc);

        for( int index=0; index < collectionSequenceIds.size(); index++){

            long collectionSequenceId = collectionSequenceIds.get(index);

            // check to see if this collection sequence has already been started, if it hasn't
            // we can't make any further assumptions about it, go onto next item. CAF-1415
            if ( !sequencesAlreadyStarted.contains(collectionSequenceId))
            {
                continue;
            }
            
            // if the cs hasnt been completed it is in this collection sequence
            if (!sequencesAlreadyCompleted.contains(collectionSequenceId)) {
                // get the last evaluated policy within the collection sequence
                Long lastEvaluatedPolicy = getLastEvaluatedPolicyForCollectionSequence(doc, collectionSequenceId, policiesToExecute);
                // If the policy was already previously ran against diagnostics return null
                if(shouldSkipPolicyBecauseAlreadyRunWithDiagnostics(doc, lastEvaluatedPolicy)) {
                    // return null if the policy was already ran with diagnostics
                    return null;
                }
                return new CollectionSequenceIdToPolicyId(collectionSequenceId, lastEvaluatedPolicy);
            }

            // has it started any other collection sequence, if so dont use this Id any longer.
            if ( checkHasStartedAnotherCollectionSequence( index, collectionSequenceIds, sequencesAlreadyStarted ) )
            {
                continue;
            }
            
            // ok we haven't started any other collection sequences yet, so work out the last one on this sequence.
            Long lastEvaluatedPolicy = getLastEvaluatedPolicyForCollectionSequence(doc, collectionSequenceId, policiesToExecute);

            // If the policy was already previously ran against diagnostics return null
            if(shouldSkipPolicyBecauseAlreadyRunWithDiagnostics(doc, lastEvaluatedPolicy)) {
                // return null if the policy was already ran with diagnostics
                return null;
            }

            return new CollectionSequenceIdToPolicyId(collectionSequenceId, lastEvaluatedPolicy);

        }
        // return null if we cannot find a previous policy that was executed
        return null;
    }

    private boolean checkHasStartedAnotherCollectionSequence( int index,
                                                              List<Long> collectionSequenceIds,
                                                              List<Long> sequencesAlreadyStarted ) {
        // otherwise, the tags suggest this collection sequence has completed, we need to check has it
        // started to execute the next collection sequence in the chain, or has it just executed the last policy
        // in this sequence during the last evaluation run.
        try {
            // go through each collection sequence after our current location, and check if any have been started.
            for( int subIndex=index+1; subIndex < collectionSequenceIds.size(); subIndex++){
                long nextCollectionSequenceId = collectionSequenceIds.get(subIndex);
                
                if (!sequencesAlreadyStarted.contains(nextCollectionSequenceId)) {
                    continue;
                }
                
                return true;
            }
        } catch (IndexOutOfBoundsException ex) {
            // no more elements in the area, just carry on and try for this sequence.
        }
        
        return false;
    }

    private boolean shouldSkipPolicyBecauseAlreadyRunWithDiagnostics(com.github.cafdataprocessing.corepolicy.common.Document doc,
                                                                     Long policyId) {

        if (doc.getMetadata().containsKey(PolicyWorkerConstants.POLICYWORKER_FAILURE_POLICY + policyId)) {
            return true;
        }
        return false;
    }

    /**
     * Returns the last evaluated Policy ID for the given collection sequence
     *
     * @param doc
     * @param collectionSequenceId
     * @param policiesToExecute
     * @return
     */
    public Long getLastEvaluatedPolicyForCollectionSequence(com.github.cafdataprocessing.corepolicy.common.Document doc,
                                                            Long collectionSequenceId, Collection<Long> policiesToExecute) {
        // get the evaluated policies from POLICYWORKER_COLLECTIONSEQUENCE_POLICIES_<csid> getPoliciesAlreadyExecutedForStringMap( csid )
        // get the list of ordered ( resolved policies from a classify result ).
        // Go through each resolved policy to find the last one in the already executed list ( just because the executed list may be randomly ordered ).

        // Evaluated Policies is the list of policies within the given collection sequence id that have been executed
        Collection<Long> evaluatedPoliciesExecuted = DocumentFields.getPoliciesAlreadyExecutedForStringMap(collectionSequenceId, doc.getMetadata());

        // If there is only one policy then this is the previous policy executed
        if (evaluatedPoliciesExecuted.size() == 1) {
            return evaluatedPoliciesExecuted.stream().findFirst().get();
        }

        if (policiesToExecute == null) {
            com.github.cafdataprocessing.corepolicy.common.dto.ClassifyDocumentResult classifyDocumentResult =
                    classifyDocumentApi.classify(collectionSequenceId, doc);
            policiesToExecute = classifyDocumentResult.resolvedPolicies;
        }

        Long previousPolicy = null;

        for (Long policyIdToExecute : policiesToExecute) {
            if (!evaluatedPoliciesExecuted.contains(policyIdToExecute)) {
                continue;
            }
            previousPolicy = policyIdToExecute;
        }

        return previousPolicy;
    }

    private boolean evaluateAndExecuteNextPolicy(List<Long> collectionSequenceIds, boolean execute,
                                                 List<ClassifyDocumentResult> classifyDocumentResults,
                                                 com.github.cafdataprocessing.corepolicy.common.Document doc) throws InvalidTaskException{
        for (Long collectionSequenceId : collectionSequenceIds) {
            //clear out any previous worker response
            corePolicyApplicationContext.getBean(WorkerResponseHolder.class).clear();
            corePolicyApplicationContext.getBean(WorkerRequestHolder.class).clear();

            if (canSkipAlreadyCompletedSequence(doc, collectionSequenceId)) continue;

            com.github.cafdataprocessing.corepolicy.common.dto.ClassifyDocumentResult classifyDocumentResult;
            try {
                classifyDocumentResult =
                        classifyDocumentApi.classify(collectionSequenceId, doc);
            } catch (InvalidFieldValueCpeException e) {
                //  No match found on collection sequence.
                throw new InvalidTaskException(e.getMessage(),e);
            }

            classifyDocumentResults.add(classifyDocumentResultConverter.convert(classifyDocumentResult));

            if (execute) {
                if (classifyDocumentResult.resolvedPolicies != null && classifyDocumentResult.resolvedPolicies.size() > 0) {
                    //get list of policies in the same order as the collections. Classify can return policies
                    //not in the order that they appear on collections if policies have the same type
                    List<Long> matchedPolicyIds = classifyDocumentResult.matchedCollections.stream()
                            .map(c -> c.getPolicies())
                            .flatMap(Collection::stream)
                            .map(pol -> pol.getId()).collect(Collectors.toList());

                    // Have we executed any of these policies for this collection sequence before now?
                    // If so drop them from the list.
                    Collection<Long> policiesYetToBeExecuted =
                            getPoliciesYetToBeExecuted(doc, collectionSequenceId, matchedPolicyIds);

                    if (policiesYetToBeExecuted.size() == 0) {
                        logger.debug("All policies have already been executed for sequence" + collectionSequenceId);
                    } else {
                        com.github.cafdataprocessing.corepolicy.common.dto.DocumentToExecutePolicyOn documentToExecutePolicyOn =
                                new com.github.cafdataprocessing.corepolicy.common.dto.DocumentToExecutePolicyOn();
                        documentToExecutePolicyOn.document = doc;
                        //loop here over individual policies due to ordering issues when calling execute
                        com.github.cafdataprocessing.corepolicy.common.Document executedDocument = documentToExecutePolicyOn.document;
                        for(Long policyIdToExecute: policiesYetToBeExecuted){
                            documentToExecutePolicyOn.policyIds = Arrays.asList(policyIdToExecute);
                            executedDocument = classifyDocumentApi.execute(collectionSequenceId, documentToExecutePolicyOn);
                            WorkerResponseHolder workerResponseHolder = corePolicyApplicationContext.getBean(WorkerResponseHolder.class);
                            if(workerResponseHolder != null && workerResponseHolder.isShouldStop()){
                                break;
                            }
                        }

                        // Before we exit from here, make a final decision about whether we have finished executing all policies
                        // for a given collection sequence - this prevents it classifying this sequence again in the future.
                        if (getPoliciesYetToBeExecuted(executedDocument, collectionSequenceId, matchedPolicyIds).size() == 0) {
                            // we have executed all policies add on that this sequence is complete.
                            DocumentFields.addCollectionSequenceCompletedInfo(executedDocument, collectionSequenceId);
                        }

                        //if the response was set then stop executing
                        if (updateTaskDataAndCheckIfShouldCreateChainedWorkerResponse(executedDocument)) {

                            return true;
                        }
                    }
                }
            } else {
                // there is no policies at all on this sequence, but we still need to mark context
                // of this sequence as completed, to ensure it chains to the next item if it ever leaves
                // here.
                DocumentFields.addCollectionSequenceCompletedInfo(doc, collectionSequenceId);
            }
        }
        return false;
    }

    /**
     * Try to work out if we have already fully evaluated a collection sequence ( and its policies ) by checking if
     * we have moved onto a subsequent sequenceid.
     *
     * @param document
     * @param collectionSequenceId
     * @return
     */
    private boolean canSkipAlreadyCompletedSequence(com.github.cafdataprocessing.corepolicy.common.Document document,
                                                    Long collectionSequenceId) {

        // check to see if we can skip a sequence, only sequences which are fully executed appear on this list
        List<Long> sequencesAlreadyClassified = DocumentFields.getCollectionSequencesAlreadyCompleted(document);

        // if no sequences already classified, return false.
        if (sequencesAlreadyClassified.size() == 0) {
            return false;
        }

        // does the sequence list contain the id of the item we are being asked about
        return sequencesAlreadyClassified.contains(collectionSequenceId);
    }

    private static Collection<Long> getPoliciesYetToBeExecuted(com.github.cafdataprocessing.corepolicy.common.Document document,
                                                               Long collectionSequenceId, Collection<Long> resolvedPolicies) {

        Collection<Long> policiesExecutedAlready =
                DocumentFields.getPoliciesAlreadyExecutedForStringMap(collectionSequenceId, document.getMetadata());

        Collection<Long> policiesYetToBeExecuted = new ArrayList<>();
        policiesYetToBeExecuted.addAll(resolvedPolicies);
        policiesYetToBeExecuted.removeAll(policiesExecutedAlready);
        return policiesYetToBeExecuted;
    }

    /**
     * Get the handlers for a given user context.  This will internally register either
     * a user specific type or a base data type implicitally.
     *
     * @param projectId
     */
    private void registerHandlers(String projectId) {
        if (classifyDocumentApi instanceof ClassifyDocumentApiDirectImpl) {
            UserContext userContext = corePolicyApplicationContext.getBean(UserContext.class);
            userContext.setProjectId(projectId);
            if (!registeredUsers.contains(projectId)) {
                ServiceLoader<WorkerPolicyHandler> loader = ServiceLoader.load(WorkerPolicyHandler.class);

                for (WorkerPolicyHandler handler : loader) {
                    setUpWorkerHandler(handler, projectId);
                }
                registeredUsers.add(projectId);
            }
        }
    }


    private void setUpWorkerHandler(WorkerPolicyHandler workerPolicyHandler, String projectId) {
        String uniqueName = workerPolicyHandler.getPolicyType().shortName + projectId;

        if (registeredHandlers.contains(uniqueName)) {
            return;
        }

        workerPolicyHandler.setApplicationContext(corePolicyApplicationContext);
        ((ClassifyDocumentApiDirectImpl) classifyDocumentApi).registerHandler(workerPolicyHandler);

        //Register policy
        PolicyType policyTypeToRegister = workerPolicyHandler.getPolicyType();
        PolicyType registeredPolicyType;
        try {
            registeredPolicyType = policyApi.retrievePolicyTypeByName(policyTypeToRegister.shortName);
        } catch (Exception e) {
            logger.trace("PolicyType for name " + policyTypeToRegister.shortName + " not found.", e);
            // workers for this type aren't found in the system this is fatal, and shouldn't happen.
            throw new RuntimeException("PolicyType for name " + policyTypeToRegister.shortName + " not found.", e);
        }

        workerPolicyHandler.setPolicyTypeId(registeredPolicyType.id);

        logger.info("Registered WorkerPolicyHandler - " + uniqueName);

        registeredHandlers.add(uniqueName);
    }
}