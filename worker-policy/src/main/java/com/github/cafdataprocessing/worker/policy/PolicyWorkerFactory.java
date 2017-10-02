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

import com.github.cafdataprocessing.corepolicy.api.ApiProvider;
import com.github.cafdataprocessing.corepolicy.common.ElasticsearchProperties;
import com.github.cafdataprocessing.corepolicy.common.EngineProperties;
import com.github.cafdataprocessing.corepolicy.common.WorkflowApi;
import com.github.cafdataprocessing.corepolicy.common.dto.SequenceWorkflow;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterInterface;
import com.github.cafdataprocessing.corepolicy.booleanagent.BooleanAgentServices;
import com.github.cafdataprocessing.corepolicy.common.CorePolicyApplicationContext;
import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.worker.policy.shared.PolicyWorkerConstants;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.worker.*;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.TrackedDocument;
import com.github.cafdataprocessing.worker.policy.version.tagging.PolicyReprocessingVersionTagging;
import com.github.cafdataprocessing.worker.policy.version.tagging.PolicyReprocessingVersionTaggingException;
import com.github.cafdataprocessing.worker.policy.version.tagging.WorkerProcessingInfo;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import java.util.concurrent.TimeUnit;

/**
 * Produces a PolicyWorker given some serialized task data which has originated from a WorkerQueue.
 */
public class PolicyWorkerFactory implements WorkerFactory {
    private final Codec codec;
    private DataStore dataStore;
    private final String resultQueue;
    private final TaskDataConverter taskDataConverter;
    private final CorePolicyApplicationContext applicationContext;
    private final String workerIdentifier;
    private final int workerThreads;
    private final PolicyWorkerConfiguration configuration;
    private static Logger logger = LoggerFactory.getLogger(PolicyWorkerFactory.class);
    private final ApiProvider apiProvider;
    private final LoadingCache<Long, SequenceWorkflow> workflowCache;

    public PolicyWorkerFactory(Codec codec, DataStore dataStore, PolicyWorkerConfiguration config) {
        this.codec = codec;
        this.dataStore = dataStore;
        this.resultQueue = config.getResultQueue();
        this.workerIdentifier = config.getWorkerIdentifier();
        this.workerThreads = config.getWorkerThreads();
        this.configuration = config;
        applicationContext = new CorePolicyApplicationContext();
        createBeanDefinition(WorkerResponseHolder.class, "WorkerResponseHolder", "thread");
        createBeanDefinition(WorkerRequestHolder.class, "WorkerRequestHolder", "thread");
        applicationContext.refresh();
        taskDataConverter = new TaskDataConverter();
        apiProvider = new ApiProvider(applicationContext);
        EngineProperties engineProperties = applicationContext.getBean(EngineProperties.class);
        Period period = new Period(engineProperties.getEnvironmentCacheVerifyPeriod());
        workflowCache = CacheBuilder.newBuilder().expireAfterWrite(period.toStandardDuration().getMillis(), TimeUnit.MILLISECONDS)
                .build(new CacheLoader<Long, SequenceWorkflow>()
                {
                    @Override
                    public SequenceWorkflow load(Long key) throws Exception
                    {
                        logger.warn("Loading workflow from database, id: {}", key);
                        return getWorkflow(key, apiProvider.getWorkflowApi());
                    }
                });
        if (config.getRegisterHandlers()) {
            new PolicyHandlerSetup(applicationContext).checkBasePolicies();
        }
    }

    private SequenceWorkflow getWorkflow(long id, WorkflowApi workflowApi) throws InvalidTaskException {
        try {
            logger.warn("Setting up cache: Retrieving SequenceWorkflow from Database.");
            return workflowApi.retrieveSequenceWorkflow(id);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Could not find a match for the SequenceWorkflow requested")) {
                throw new InvalidTaskException(e.getMessage(),e);
            } else {
                throw e;
            }
        }
    }

    private void createBeanDefinition(Class<?> beanClass, String beanClassName, String beanScope ) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setScope(beanScope);
        beanDefinition.setBeanClass(beanClass);

        applicationContext.registerBeanDefinition(beanClassName, beanDefinition);
    }

    @Override
    public Worker getWorker(WorkerTaskData workerTask) throws InvalidTaskException {
        TaskStatus taskStatus = workerTask.getStatus();
        byte[] data = workerTask.getData();
        byte[] context = workerTask.getContext();

        TaskData task;
        try {
            if (workerTask == null) {
                throw new InvalidTaskException("Cannot retrieve TaskData, the WorkerTaskData provided was null.");
            }

            final String classifier = workerTask.getClassifier();
            final int version = workerTask.getVersion();

            // Check if the message is destined for the Policy Worker itself or if it should be routed through a converter
            if (classifier.equalsIgnoreCase(PolicyWorkerConstants.WORKER_NAME)) {
                if (version == PolicyWorkerConstants.API_VERSION) {
                    task = codec.deserialise(workerTask.getData(), TaskData.class);
                } else {
                    throw new InvalidTaskException("Policy Worker Message Version not supported: " + version);
                }
            } else {
                // The Converter's conversion logic will only be called if the TaskStatus is NEW_TASK, RESULT_SUCCESS or RESULT_FAILURE.
                // Otherwise the task is still returned from the context, and the error information is added to it.
                final PolicyWorkerConverterInterface converter = taskDataConverter.getConverter(classifier, version);
                task = TaskDataConverter.convert(converter, codec, dataStore, workerTask);
                taskStatus = TaskStatus.RESULT_SUCCESS;
            }
        } catch (CodecException e) {
            logger.warn(TaskDataConverter.getCodecExceptionVariablesInfoMessage(workerTask.getClassifier(), workerTask.getData(), workerTask.getContext()));
            throw new InvalidTaskException("Message could not be deserialized", e);
        }
        catch (Error e) {
            // Ensure we don't throw errors, convert to a type the CAF framework expects.
            throw new InvalidTaskException("Error occurred when converting the data", e);
        }
        catch (InvalidTaskException e) {
            // don't wrap accepted exceptions.
            throw e;
        }
        catch(Exception e)
        {
            // otherwise, wrap and throw as accepted type by CAF.
            throw new InvalidTaskException( "Unexpected exception occurred", e);
        }

        //add Policy Worker Version information to current taskData
        try {
            if(task!=null) {
                Document workerDocument = task.getDocument();
                if (workerDocument == null) {
                    logger.warn("Document on task is null, Worker Version will not be recorded.");
                } else {
                    //wrap the Document from task data in TrackedDocument so this addition is recorded on processing record
                    TrackedDocument trackedDocument = new TrackedDocument(task.getDocument());
                    PolicyReprocessingVersionTagging.addProcessingWorkerVersion(trackedDocument, new WorkerProcessingInfo(configuration.getWorkerVersion(),
                            PolicyWorkerConstants.WORKER_NAME));
                }
            }
        } catch (PolicyReprocessingVersionTaggingException e) {
            logger.warn("Failed to add Processing Worker Version to document.", e);
        }

        return new PolicyWorker(workerIdentifier, applicationContext,
                workflowCache, apiProvider, taskDataConverter, taskStatus, task,
                codec, data, context, resultQueue, dataStore);
    }

    @Override
    public HealthResult healthCheck() {

        HealthStatus status;
        if (applicationContext.getBean(ElasticsearchProperties.class).isElasticsearchDisabled()) {
            status = HealthStatus.HEALTHY;
        }
        else {
            BooleanAgentServices booleanAgentServices = applicationContext.getBean(BooleanAgentServices.class);
            if (booleanAgentServices.canConnect()) {
                status = HealthStatus.HEALTHY;
            } else {
                status = HealthStatus.UNHEALTHY;
            }
        }
        return new HealthResult(status);
    }

    @Override
    public WorkerConfiguration getWorkerConfiguration() {
        return configuration;
    }

    @Override
    public String getInvalidTaskQueue()
    {
        return this.resultQueue;
    }

    @Override
    public int getWorkerThreads() {
        return this.workerThreads;
    }
}
