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
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.worker.policy.TaskDataConverter;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterException;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterInterface;
import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.worker.policy.shared.PolicyWorkerConstants;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.github.cafdataprocessing.worker.policy.testconverters.CodecExceptionConverter;
import com.github.cafdataprocessing.worker.policy.util.TestWorkerTaskData;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.*;
import com.github.cafdataprocessing.worker.policy.version.tagging.PolicyReprocessingVersionTagging;
import com.hpe.caf.codec.JsonLzfCodec;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * Unit testing for the TaskDataConverter class
 */
public class TaskDataConverterTest {

    static PolicyWorkerConverterInterface converter;

    @Mock
    DataStore dataStore;

    @BeforeClass
    public static void setupConverter() throws PolicyWorkerConverterException, CodecException, InvalidTaskException {
        //create an empty implementation of a converter for use with tests
        converter = new PolicyWorkerConverterInterface() {
            @Override
            public void updateSupportedClassifierVersions(Multimap<String, Integer> supportedMap) {
            }
        };
    }

    /**
     * Test to verify that calling the WorkerTask version of convert results in a field with the worker version information being added to the document.
     *
     * @throws CodecException
     * @throws InvalidTaskException
     * @throws PolicyWorkerConverterException
     */
    @Test
    public void testConvertWithWorkerTaskAddsVersion() throws CodecException, InvalidTaskException, PolicyWorkerConverterException
    {
        Codec codec = new JsonLzfCodec();
        byte[] data = new byte[0];
        String classifier = "TestClassifier";
        int version = 1;

        WorkerTaskData workerTaskData = createWorkerTaskData(codec, classifier, version, data, TaskStatus.RESULT_SUCCESS);

        TaskData taskData = TaskDataConverter.convert(converter, codec, dataStore, workerTaskData);

        Multimap<String, String> metadata = taskData.getDocument().getMetadata();

        Assert.assertNotNull("TaskData returned from convert should not be null.", taskData);

        String expectedFieldName = PolicyReprocessingVersionTagging.getProcessingFieldName(classifier);
        Collection<String> workerVersionTag = metadata.get(expectedFieldName);
        Assert.assertEquals("Expecting only a single worker version tag value.", 1, workerVersionTag.size());
        Assert.assertEquals("Expecting value of worker version tag to be that of passed in TaskSourceInfo version.",
                ""+version, workerVersionTag.iterator().next());
    }


    /**
     * Test to verify that calling convert with an unsuccessful task status results in a field with the worker failure
     * field being added to the document.
     */
    @Test
    public void testConvertWithFailureTaskStatus() throws InvalidTaskException, CodecException, PolicyWorkerConverterException
    {
        Codec codec = new JsonLzfCodec();
        byte[] data = new byte[0];
        String classifier = "TestClassifier";
        int version = 1;

        TestWorkerTaskData workerTaskData = (TestWorkerTaskData) createWorkerTaskData(codec, classifier, version, data, TaskStatus.RESULT_FAILURE);

        TaskData taskData = TaskDataConverter.convert(converter, codec, dataStore, workerTaskData);

        Multimap<String, String> metadata = taskData.getDocument().getMetadata();

        Assert.assertTrue("One field should have been added to the document POLICYWORKER_FAILURE field.", metadata.get(PolicyWorkerConstants.POLICYWORKER_ERROR).size()==1);
        Assert.assertTrue("One field should have been added to the document POLICYWORKER_FAILURE field.", metadata.get(PolicyWorkerConstants.POLICYWORKER_FAILURE).size()==1);

        Assert.assertEquals("The Error field should contain the error message.",
                buildNonExceptionMsg(classifier, TaskStatus.RESULT_FAILURE),
                metadata.get(PolicyWorkerConstants.POLICYWORKER_ERROR).iterator().next());

        Assert.assertEquals("The Failure field should contain the error message.",
                buildNonExceptionMsg(classifier, TaskStatus.RESULT_FAILURE),
                metadata.get(PolicyWorkerConstants.POLICYWORKER_FAILURE).iterator().next());

    }


    /**
     * This test verifies that when a task with a TaskStatus of RESULT_EXCEPTION is sent to TaskDataConverter that it will log the expected
     * error and failure fields on the policy document. It won't go into the converter implementations.
     *
     * @throws CodecException
     * @throws InvalidTaskException
     */
    @Test
    public void testResultException() throws CodecException, InvalidTaskException
    {
        Codec codec = new JsonLzfCodec();

        String stackTrace = "class org.hibernate.exception.JDBCConnectionException could not inspect JDBC autocommit mode org.hibernate.exception.internal.SQLStateConversionDelegate.convert(SQLStateConversionDelegate.java:132) org.hibernate.exception.internal.StandardSQLExceptionConverter.convert(StandardSQLExceptionConverter.java:49) org.hibernate.engine.jdbc.spi.SqlExceptionHelper.convert(SqlExceptionHelper.java:126) org.hibernate.engine.jdbc.spi.SqlExceptionHelper.convert(SqlExceptionHelper.java:112) org.hibernate.engine.jdbc.internal.LogicalConnectionImpl.isAutoCommit(LogicalConnectionImpl.java:325) org.hibernate.engine.transaction.internal.TransactionCoordinatorImpl.afterNonTransactionalQuery(TransactionCoordinatorImpl.java:199) org.hibernate.internal.SessionImpl.afterOperation(SessionImpl.java:503) org.hibernate.internal.SessionImpl.list(SessionImpl.java:1690) org.hibernate.internal.CriteriaImpl.list(CriteriaImpl.java:380) com.github.cafdataprocessing.corepolicy.hibernate.repositories.HibernateBaseRepositoryImpl.retrieve(HibernateBaseRepositoryImpl.java:197) com.github.cafdataprocessing.corepolicy.hibernate.repositories.HibernateBaseRepository.retrieve(HibernateBaseRepository.java:125) com.github.cafdataprocessing.corepolicy.repositories.WorkflowApiRepositoryImpl.lambda$retrieveSequenceWorkflows$16(WorkflowApiRepositoryImpl.java:210) com.github.cafdataprocessing.corepolicy.hibernate.HibernateExecutionContextImpl.retryNonTransactional(HibernateExecutionContextImpl.java:129) com.github.cafdataprocessing.corepolicy.repositories.WorkflowApiRepositoryImpl.retrieveSequenceWorkflows(WorkflowApiRepositoryImpl.java:209) com.github.cafdataprocessing.corepolicy.repositories.WorkflowApiRepositoryImpl.retrieveSequenceWorkflow(WorkflowApiRepositoryImpl.java:229) com.github.cafdataprocessing.policyworker.ExecuteTaskData.execute(ExecuteTaskData.java:83) com.github.cafdataprocessing.policyworker.PolicyWorker.Execute(PolicyWorker.java:53) com.github.cafdataprocessing.policyworker.PolicyWorker.doWork(PolicyWorker.java:68) com.hpe.caf.worker.core.StreamingWorkerWrapper.run(StreamingWorkerWrapper.java:41) java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511) java.util.concurrent.FutureTask.run(FutureTask.java:266) java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142) java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617) java.lang.Thread.run(Thread.java:745). Cause: class org.postgresql.util.PSQLException This connection has been closed. org.postgresql.jdbc2.AbstractJdbc2Connection.checkClosed(AbstractJdbc2Connection.java:820) org.postgresql.jdbc2.AbstractJdbc2Connection.getAutoCommit(AbstractJdbc2Connection.java:781) com.mchange.v2.c3p0.impl.NewProxyConnection.getAutoCommit(NewProxyConnection.java:938) org.hibernate.engine.jdbc.internal.LogicalConnectionImpl.isAutoCommit(LogicalConnectionImpl.java:322) org.hibernate.engine.transaction.internal.TransactionCoordinatorImpl.afterNonTransactionalQuery(TransactionCoordinatorImpl.java:199) org.hibernate.internal.SessionImpl.afterOperation(SessionImpl.java:503) org.hibernate.internal.SessionImpl.list(SessionImpl.java:1690) org.hibernate.internal.CriteriaImpl.list(CriteriaImpl.java:380) com.github.cafdataprocessing.corepolicy.hibernate.repositories.HibernateBaseRepositoryImpl.retrieve(HibernateBaseRepositoryImpl.java:197) com.github.cafdataprocessing.corepolicy.hibernate.repositories.HibernateBaseRepository.retrieve(HibernateBaseRepository.java:125) com.github.cafdataprocessing.corepolicy.repositories.WorkflowApiRepositoryImpl.lambda$retrieveSequenceWorkflows$16(WorkflowApiRepositoryImpl.java:210) com.github.cafdataprocessing.corepolicy.hibernate.HibernateExecutionContextImpl.retryNonTransactional(HibernateExecutionContextImpl.java:129) com.github.cafdataprocessing.corepolicy.repositories.WorkflowApiRepositoryImpl.retrieveSequenceWorkflows(WorkflowApiRepositoryImpl.java:209) com.github.cafdataprocessing.corepolicy.repositories.WorkflowApiRepositoryImpl.retrieveSequenceWorkflow(WorkflowApiRepositoryImpl.java:229) com.github.cafdataprocessing.policyworker.ExecuteTaskData.execute(ExecuteTaskData.java:83) com.github.cafdataprocessing.policyworker.PolicyWorker.Execute(PolicyWorker.java:53) com.github.cafdataprocessing.policyworker.PolicyWorker.doWork(PolicyWorker.java:68) com.hpe.caf.worker.core.StreamingWorkerWrapper.run(StreamingWorkerWrapper.java:41) java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511) java.util.concurrent.FutureTask.run(FutureTask.java:266) java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142) java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617) java.lang.Thread.run(Thread.java:745)";
        byte[] data = codec.serialise(stackTrace);

        WorkerTaskData workerTaskData = createWorkerTaskData(codec, CodecExceptionConverter.SUPPORTED_CLASSIFIER,
                CodecExceptionConverter.SUPPORTED_VERSION, data, TaskStatus.RESULT_EXCEPTION);

        TaskData taskData = TaskDataConverter.convert(converter, codec, dataStore, workerTaskData);

        Multimap<String, String> metadata = taskData.getDocument().getMetadata();

        Assert.assertTrue("Metadata should contain POLICYWORKER_ERROR field.", metadata.containsKey(PolicyWorkerConstants.POLICYWORKER_ERROR));
        Assert.assertTrue("Metadata should contain POLICYWORKER_FAILURE field.", metadata.containsKey(PolicyWorkerConstants.POLICYWORKER_FAILURE));

        Assert.assertTrue(metadata.get(PolicyWorkerConstants.POLICYWORKER_ERROR).size()==1);

        Assert.assertEquals("Error added to field should match the form \"Classifier: TaskStatus: \"stack trace\"\"",
                buildExceptionMsg(CodecExceptionConverter.SUPPORTED_CLASSIFIER, TaskStatus.RESULT_EXCEPTION, stackTrace),
                metadata.get(PolicyWorkerConstants.POLICYWORKER_ERROR).iterator().next());

        Assert.assertEquals("Failure added to field should match the form \"Classifier: TaskStatus: \"stack trace\"\"",
                buildExceptionMsg(CodecExceptionConverter.SUPPORTED_CLASSIFIER, TaskStatus.RESULT_EXCEPTION, stackTrace),
                metadata.get(PolicyWorkerConstants.POLICYWORKER_FAILURE).iterator().next());
    }


    /**
     * This test verifies that when an invalid task is sent to TaskDataConverter that it will log the expected error and failure fields on
     * the policy document. It won't go into the converter implementations.
     *
     * @throws CodecException
     * @throws InvalidTaskException
     */
    @Test
    public void testInvalidTask() throws CodecException, InvalidTaskException
    {
        Codec codec = new JsonLzfCodec();

        String stackTrace = "class org.hibernate.exception.JDBCConnectionException could not inspect JDBC autocommit mode org.hibernate.exception.internal.SQLStateConversionDelegate.convert(SQLStateConversionDelegate.java:132) org.hibernate.exception.internal.StandardSQLExceptionConverter.convert(StandardSQLExceptionConverter.java:49) org.hibernate.engine.jdbc.spi.SqlExceptionHelper.convert(SqlExceptionHelper.java:126) org.hibernate.engine.jdbc.spi.SqlExceptionHelper.convert(SqlExceptionHelper.java:112) org.hibernate.engine.jdbc.internal.LogicalConnectionImpl.isAutoCommit(LogicalConnectionImpl.java:325) org.hibernate.engine.transaction.internal.TransactionCoordinatorImpl.afterNonTransactionalQuery(TransactionCoordinatorImpl.java:199) org.hibernate.internal.SessionImpl.afterOperation(SessionImpl.java:503) org.hibernate.internal.SessionImpl.list(SessionImpl.java:1690) org.hibernate.internal.CriteriaImpl.list(CriteriaImpl.java:380) com.github.cafdataprocessing.corepolicy.hibernate.repositories.HibernateBaseRepositoryImpl.retrieve(HibernateBaseRepositoryImpl.java:197) com.github.cafdataprocessing.corepolicy.hibernate.repositories.HibernateBaseRepository.retrieve(HibernateBaseRepository.java:125) com.github.cafdataprocessing.corepolicy.repositories.WorkflowApiRepositoryImpl.lambda$retrieveSequenceWorkflows$16(WorkflowApiRepositoryImpl.java:210) com.github.cafdataprocessing.corepolicy.hibernate.HibernateExecutionContextImpl.retryNonTransactional(HibernateExecutionContextImpl.java:129) com.github.cafdataprocessing.corepolicy.repositories.WorkflowApiRepositoryImpl.retrieveSequenceWorkflows(WorkflowApiRepositoryImpl.java:209) com.github.cafdataprocessing.corepolicy.repositories.WorkflowApiRepositoryImpl.retrieveSequenceWorkflow(WorkflowApiRepositoryImpl.java:229) com.github.cafdataprocessing.policyworker.ExecuteTaskData.execute(ExecuteTaskData.java:83) com.github.cafdataprocessing.policyworker.PolicyWorker.Execute(PolicyWorker.java:53) com.github.cafdataprocessing.policyworker.PolicyWorker.doWork(PolicyWorker.java:68) com.hpe.caf.worker.core.StreamingWorkerWrapper.run(StreamingWorkerWrapper.java:41) java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511) java.util.concurrent.FutureTask.run(FutureTask.java:266) java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142) java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617) java.lang.Thread.run(Thread.java:745). Cause: class org.postgresql.util.PSQLException This connection has been closed. org.postgresql.jdbc2.AbstractJdbc2Connection.checkClosed(AbstractJdbc2Connection.java:820) org.postgresql.jdbc2.AbstractJdbc2Connection.getAutoCommit(AbstractJdbc2Connection.java:781) com.mchange.v2.c3p0.impl.NewProxyConnection.getAutoCommit(NewProxyConnection.java:938) org.hibernate.engine.jdbc.internal.LogicalConnectionImpl.isAutoCommit(LogicalConnectionImpl.java:322) org.hibernate.engine.transaction.internal.TransactionCoordinatorImpl.afterNonTransactionalQuery(TransactionCoordinatorImpl.java:199) org.hibernate.internal.SessionImpl.afterOperation(SessionImpl.java:503) org.hibernate.internal.SessionImpl.list(SessionImpl.java:1690) org.hibernate.internal.CriteriaImpl.list(CriteriaImpl.java:380) com.github.cafdataprocessing.corepolicy.hibernate.repositories.HibernateBaseRepositoryImpl.retrieve(HibernateBaseRepositoryImpl.java:197) com.github.cafdataprocessing.corepolicy.hibernate.repositories.HibernateBaseRepository.retrieve(HibernateBaseRepository.java:125) com.github.cafdataprocessing.corepolicy.repositories.WorkflowApiRepositoryImpl.lambda$retrieveSequenceWorkflows$16(WorkflowApiRepositoryImpl.java:210) com.github.cafdataprocessing.corepolicy.hibernate.HibernateExecutionContextImpl.retryNonTransactional(HibernateExecutionContextImpl.java:129) com.github.cafdataprocessing.corepolicy.repositories.WorkflowApiRepositoryImpl.retrieveSequenceWorkflows(WorkflowApiRepositoryImpl.java:209) com.github.cafdataprocessing.corepolicy.repositories.WorkflowApiRepositoryImpl.retrieveSequenceWorkflow(WorkflowApiRepositoryImpl.java:229) com.github.cafdataprocessing.policyworker.ExecuteTaskData.execute(ExecuteTaskData.java:83) com.github.cafdataprocessing.policyworker.PolicyWorker.Execute(PolicyWorker.java:53) com.github.cafdataprocessing.policyworker.PolicyWorker.doWork(PolicyWorker.java:68) com.hpe.caf.worker.core.StreamingWorkerWrapper.run(StreamingWorkerWrapper.java:41) java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511) java.util.concurrent.FutureTask.run(FutureTask.java:266) java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142) java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617) java.lang.Thread.run(Thread.java:745)";
        byte[] data = codec.serialise(stackTrace);

        WorkerTaskData workerTaskData = createWorkerTaskData(codec, CodecExceptionConverter.SUPPORTED_CLASSIFIER,
                CodecExceptionConverter.SUPPORTED_VERSION, data, TaskStatus.INVALID_TASK);

        TaskData taskData = TaskDataConverter.convert(converter, codec, dataStore, workerTaskData);

        Multimap<String, String> metadata = taskData.getDocument().getMetadata();

        Assert.assertTrue("Metadata should contain POLICYWORKER_ERROR field.", metadata.containsKey(PolicyWorkerConstants.POLICYWORKER_ERROR));
        Assert.assertTrue("Metadata should contain POLICYWORKER_FAILURE field.", metadata.containsKey(PolicyWorkerConstants.POLICYWORKER_FAILURE));

        Assert.assertTrue(metadata.get(PolicyWorkerConstants.POLICYWORKER_ERROR).size()==1);

        Assert.assertEquals("Error added to field should match the form \"Classifier: TaskStatus: \"stack trace\"\"",
                CodecExceptionConverter.SUPPORTED_CLASSIFIER +": " +TaskStatus.INVALID_TASK + ": \"" +stackTrace +"\"",
                metadata.get(PolicyWorkerConstants.POLICYWORKER_ERROR).iterator().next());

        Assert.assertEquals("Failure added to field should match the form \"Classifier: TaskStatus: \"stack trace\"\"",
                CodecExceptionConverter.SUPPORTED_CLASSIFIER +": " +TaskStatus.INVALID_TASK + ": \"" +stackTrace +"\"",
                metadata.get(PolicyWorkerConstants.POLICYWORKER_FAILURE).iterator().next());
    }

    private WorkerTaskData createWorkerTaskData(Codec codec, String classifier, int version, byte[] data, TaskStatus taskStatus) throws CodecException
    {
        TaskSourceInfo sourceInfo = new TaskSourceInfo();
        sourceInfo.setName(classifier);
        sourceInfo.setVersion(""+version);

        String reference = UUID.randomUUID().toString();
        Document doc = createDocument(reference);
        TaskData taskData = new TaskData();
        taskData.setDocument(doc);

        byte[] context = codec.serialise(taskData);

        TrackingInfo trackingInfo = null;

        WorkerTaskData workerTaskData = new TestWorkerTaskData(classifier, version, taskStatus, data, context, trackingInfo, sourceInfo);
        return workerTaskData;
    }

    private static Document createDocument(String reference) {
        Document document = new Document();
        document.setDocuments(new ArrayList<Document>());
        document.setReference(reference);
        return document;
    }

    private String buildExceptionMsg(String classifier, TaskStatus taskStatus, String exceptionMessage){
        return classifier +": " +taskStatus.toString() +": \"" +exceptionMessage +"\"";
    }

    private String buildNonExceptionMsg(String classifier, TaskStatus taskStatus){
        return classifier +": " +taskStatus.toString();
    }
}
