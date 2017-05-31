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
import com.github.cafdataprocessing.corepolicy.common.DocumentImpl;
import com.github.cafdataprocessing.worker.policy.WorkerPolicyHandler;
import com.github.cafdataprocessing.worker.policy.common.ApiStrings;
import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.github.cafdataprocessing.entity.fields.FieldChangeRecord;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

/**
 * Tests to cover the WorkerPolicyHandler.updateTaskData method.
 */
public class UpdateTaskDataTest {


    @Test
    public void testUpdateTaskData() {
        final String fieldName = "myField";
        final String value1 = "aValue";
        final String value2 = "bValue";

        Document workerDocument = new Document();
        com.github.cafdataprocessing.corepolicy.common.Document corepolicyDocument = new DocumentImpl();

        //Add the same field but with different values to the corepolicy and worker documetns.
        corepolicyDocument.getMetadata().putAll(fieldName, Arrays.asList(value1, value2));
        workerDocument.getMetadata().put(fieldName, value1);

        TaskData taskData = new TaskData();
        taskData.setDocument(workerDocument);

        WorkerPolicyHandler.updateTaskDataDocument(taskData, corepolicyDocument);
        workerDocument = taskData.getDocument();
        Assert.assertNotNull("TaskData should contain a document", workerDocument);

        Assert.assertEquals("Worker's document should contain 1 field", 1, workerDocument.getMetadata().keySet().size());
        Collection<String> metadataValues = workerDocument.getMetadata().get(fieldName);
        Assert.assertEquals("Worker's document field should have 2 values", 2, metadataValues.size());
        Assert.assertEquals("Worker's document should have the correct value", value1, metadataValues.stream().filter(e -> e.equals(value1)).findFirst().orElse(""));
        Assert.assertEquals("Worker's document should have the correct value", value2, metadataValues.stream().filter(e -> e.equals(value2)).findFirst().orElse(""));

    }

    @Test
    public void testUpdateWithNoChanges() {
        final String fieldName = "myField";
        final String value1 = "aValue";
        final String value2 = "bValue";

        Document workerDocument = new Document();
        com.github.cafdataprocessing.corepolicy.common.Document corepolicyDocument = new DocumentImpl();

        //Add the same field but with different values to the corepolicy and worker documetns.
        corepolicyDocument.getMetadata().putAll(fieldName, Arrays.asList(value1, value2));
        workerDocument.getMetadata().putAll(fieldName, Arrays.asList(value1, value2));

        TaskData taskData = new TaskData();
        taskData.setDocument(workerDocument);

        WorkerPolicyHandler.updateTaskDataDocument(taskData, corepolicyDocument);
        workerDocument = taskData.getDocument();
        Assert.assertNotNull("TaskData should contain a document", workerDocument);

        Assert.assertEquals("Worker's document should contain 1 field", 1, workerDocument.getMetadata().keySet().size());
        Collection<String> metadataValues = workerDocument.getMetadata().get(fieldName);
        Assert.assertEquals("Worker's document field should have 2 values", 2, metadataValues.size());
        Assert.assertEquals("Worker's document should have the correct value", value1, metadataValues.stream().filter(e -> e.equals(value1)).findFirst().orElse(""));
        Assert.assertEquals("Worker's document should have the correct value", value2, metadataValues.stream().filter(e -> e.equals(value2)).findFirst().orElse(""));
    }

    @Test
    public void testUpdateRemoveValues() {
        final String fieldName = "myField";
        final String value1 = "aValue";
        final String value2 = "bValue";

        Document workerDocument = new Document();
        com.github.cafdataprocessing.corepolicy.common.Document corepolicyDocument = new DocumentImpl();

        //Add the same field but with different values to the corepolicy and worker documetns.
        corepolicyDocument.getMetadata().putAll(fieldName, Arrays.asList(value1));
        workerDocument.getMetadata().putAll(fieldName, Arrays.asList(value1, value2));

        TaskData taskData = new TaskData();
        taskData.setDocument(workerDocument);

        WorkerPolicyHandler.updateTaskDataDocument(taskData, corepolicyDocument);
        workerDocument = taskData.getDocument();
        Assert.assertNotNull("TaskData should contain a document", workerDocument);

        Assert.assertEquals("Worker's document should contain 1 field", 1, workerDocument.getMetadata().keySet().size());
        Collection<String> metadataValues = workerDocument.getMetadata().get(fieldName);
        Assert.assertEquals("Worker's document field should have 1 value", 1, metadataValues.size());
        Assert.assertEquals("Worker's document should have the correct value", value1, metadataValues.stream().filter(e -> e.equals(value1)).findFirst().orElse(""));
    }

    @Test
    public void testUpdateTaskData_MultipleFields() {
        final String fieldName = "myField";
        final String fieldName2 = "mySecondField";
        final String value1 = "aValue";
        final String value2 = "bValue";
        final String value3 = "cValue";

        Document workerDocument = new Document();
        com.github.cafdataprocessing.corepolicy.common.Document corepolicyDocument = new DocumentImpl();

        //Add the same field but with different values to the corepolicy and worker documetns.
        corepolicyDocument.getMetadata().putAll(fieldName, Arrays.asList(value1, value2));
        workerDocument.getMetadata().put(fieldName, value1);
        //Now add a second field to the corepolicy document that should be added to the workers
        corepolicyDocument.getMetadata().put(fieldName2, value3);

        TaskData taskData = new TaskData();
        taskData.setDocument(workerDocument);

        WorkerPolicyHandler.updateTaskDataDocument(taskData, corepolicyDocument);
        workerDocument = taskData.getDocument();
        Assert.assertNotNull("TaskData should contain a document", workerDocument);

        Assert.assertEquals("Worker's document should contain 2 fields", 2, workerDocument.getMetadata().keySet().size());

        Collection<String> metadataValues = workerDocument.getMetadata().get(fieldName);
        Assert.assertEquals("Worker's document field should have 2 values", 2, metadataValues.size());
        Assert.assertEquals("Worker's document should have the correct value", value1, metadataValues.stream().filter(e -> e.equals(value1)).findFirst().orElse(""));
        Assert.assertEquals("Worker's document should have the correct value", value2, metadataValues.stream().filter(e -> e.equals(value2)).findFirst().orElse(""));

        metadataValues = workerDocument.getMetadata().get(fieldName2);
        Assert.assertEquals("Worker's document field should have 1 values", 1, metadataValues.size());
        Assert.assertEquals("Value should match", value3, metadataValues.stream().findFirst().orElse(""));
    }

    @Test
    public void testUpdateRemoveField(){
        final String fieldName = "myField";
        final String fieldName2 = "mySecondField";
        final String value1 = "aValue";
        final String value2 = "bValue";
        final String value3 = "cValue";

        Document workerDocument = new Document();
        com.github.cafdataprocessing.corepolicy.common.Document corepolicyDocument = new DocumentImpl();

        //Add the same field but with different values to the corepolicy and worker documetns.
        corepolicyDocument.getMetadata().putAll(fieldName, Arrays.asList(value1, value2));
        workerDocument.getMetadata().put(fieldName, value1);
        //Now add a field to the worker's document that should be removed
        workerDocument.getMetadata().put(fieldName2, value3);

        TaskData taskData = new TaskData();
        taskData.setDocument(workerDocument);

        WorkerPolicyHandler.updateTaskDataDocument(taskData, corepolicyDocument);
        workerDocument = taskData.getDocument();

        Assert.assertEquals("Worker's document should contain 1 field", 1, workerDocument.getMetadata().keySet().size());
        Collection<String> metadataValues = workerDocument.getMetadata().get(fieldName);
        Assert.assertEquals("Worker's document field should have 2 values", 2, metadataValues.size());
        Assert.assertEquals("Worker's document should have the correct value", value1, metadataValues.stream().filter(e -> e.equals(value1)).findFirst().orElse(""));
        Assert.assertEquals("Worker's document should have the correct value", value2, metadataValues.stream().filter(e -> e.equals(value2)).findFirst().orElse(""));
    }

    @Test
    public void testDontTrackTempCorePolicyFields(){
        final String fieldName_1 = "test";
        final String value_1 = "test value";
        final String fieldName_2 = ApiStrings.POLICYWORKER_COLLECTION_SEQUENCE;
        final String value_2 = "1";
        final String fieldName_3 = ApiStrings.POLICYWORKER_COLLECTION_SEQUENCE_POLICY+"1";
        final String value_3 = "1";

        Document workerDocument = new Document();
        com.github.cafdataprocessing.corepolicy.common.Document corepolicyDocument = new DocumentImpl();
        //add metadata to both documents.
        addMetadataToBothDocs(workerDocument, corepolicyDocument, fieldName_1, value_1);
        addMetadataToBothDocs(workerDocument, corepolicyDocument, fieldName_2, value_2);
        addMetadataToBothDocs(workerDocument, corepolicyDocument, fieldName_3, value_3);

        //add a field value representing a sequence that just completed that will only be on the core policy document
        final String value_4 = "2";
        corepolicyDocument.getMetadata().put(fieldName_3, value_4);

        TaskData taskData = new TaskData();
        taskData.setDocument(workerDocument);

        WorkerPolicyHandler.updateTaskDataDocument(taskData, corepolicyDocument);

        Assert.assertNull("Should have tracked no changes", taskData.getDocument().getPolicyDataProcessingRecord());
    }

    private static void addMetadataToBothDocs(Document workerDocument,
                                              com.github.cafdataprocessing.corepolicy.common.Document corepolicyDocument,
                                              String fieldName, String fieldValue){
        corepolicyDocument.getMetadata().put(fieldName, fieldValue);
        workerDocument.getMetadata().put(fieldName, fieldValue);
    }

}
