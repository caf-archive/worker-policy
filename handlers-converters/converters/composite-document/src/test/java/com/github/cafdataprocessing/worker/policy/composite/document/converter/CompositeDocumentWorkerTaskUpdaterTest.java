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
package com.github.cafdataprocessing.worker.policy.composite.document.converter;

import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterException;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.TrackedDocument;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.worker.document.DocumentWorkerChange;
import com.hpe.caf.worker.document.DocumentWorkerChangeLogEntry;
import com.hpe.caf.worker.document.DocumentWorkerDocumentTask;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;

import java.util.*;

/**
 * Unit tests for the functionality of the CompositeDocumentWorkerTaskUpdaterTest class
 */
public class CompositeDocumentWorkerTaskUpdaterTest {
    @Mock
    DataStore dataStore;

    /**
     * Test that a root document that has been updated and that has subdocuments which have also been updated, is output correctly.
     */
    @Test
    public void rootWithChildrenUpdated() throws WorkerException, CodecException, PolicyWorkerConverterException {
        String testFieldName = "TEST_UUID";
        String rootTestValue = UUID.randomUUID().toString();
        String rootDocRef = UUID.randomUUID().toString();
        TrackedDocument rootDocument = new TrackedDocument();
        rootDocument.getMetadata().put("REFERENCE", rootDocRef);
        rootDocument.setReference(rootDocRef);

        String firstLevelSubDocRef = UUID.randomUUID().toString();
        DocumentInterface firstLevelSubDocument = rootDocument.addSubDocument(firstLevelSubDocRef);
        String firstLevelTestValue = UUID.randomUUID().toString();

        String secondLevelSubDocRef = UUID.randomUUID().toString();
        DocumentInterface secondLevelSubDocument = firstLevelSubDocument.addSubDocument(secondLevelSubDocRef);
        String secondLevelTestValue = UUID.randomUUID().toString();

        String thirdLevelSubDocRef = UUID.randomUUID().toString();
        DocumentInterface thirdLevelSubDocument = secondLevelSubDocument.addSubDocument(thirdLevelSubDocRef);
        String thirdLevelTestValue = UUID.randomUUID().toString();

        //set up the document worker task to convert
        DocumentWorkerDocumentTask docWorkerTask = new DocumentWorkerDocumentTask();
        docWorkerTask.changeLog = new ArrayList<>();

        DocumentWorkerChange thirdLevelDocChange = addFieldChange(testFieldName, thirdLevelTestValue);

        DocumentWorkerChange secondLevelDocAddChange = addFieldChange(testFieldName, secondLevelTestValue);
        DocumentWorkerChange secondLevelDocUpdateSubChange = updateSubDocChange(thirdLevelSubDocRef, thirdLevelDocChange,
                null);

        DocumentWorkerChange firstLevelDocAddChange = addFieldChange(testFieldName, firstLevelTestValue);
        DocumentWorkerChange firstLevelDocUpdateSubChange = updateSubDocChange(secondLevelSubDocRef, secondLevelDocAddChange,
                secondLevelDocUpdateSubChange);

        DocumentWorkerChange rootDocAddChange = addFieldChange(testFieldName, rootTestValue);
        DocumentWorkerChange rootDocUpdateSubChange = updateSubDocChange(firstLevelSubDocRef, firstLevelDocAddChange,
                firstLevelDocUpdateSubChange);

        DocumentWorkerChangeLogEntry updateRootEntry = new DocumentWorkerChangeLogEntry();
        updateRootEntry.name = rootDocRef;
        updateRootEntry.changes = new ArrayList<>();
        updateRootEntry.changes.add(rootDocAddChange);
        updateRootEntry.changes.add(rootDocUpdateSubChange);

        docWorkerTask.changeLog.add(updateRootEntry);

        CompositeDocumentWorkerTaskUpdater.updateDocument(rootDocument, docWorkerTask.changeLog);

        Collection<String> returnedRootDocumentTestField = rootDocument.getMetadata().get(testFieldName);
        Assert.assertEquals("The single test field should have been added to the root document by the converter.",
                1, returnedRootDocumentTestField.size());
        Assert.assertEquals("The test field on root document should have expected value.",
                rootTestValue, returnedRootDocumentTestField.iterator().next());

        DocumentInterface returnedFirstLevelSubdoc = rootDocument.getSubDocuments().iterator().next();
        Collection<String> returnedFirstLevelDocumentTestField = returnedFirstLevelSubdoc.getMetadata()
                .get(testFieldName);
        Assert.assertEquals("The single test field should have been added to the first level subdocument by the converter.",
                1, returnedFirstLevelDocumentTestField.size());
        Assert.assertEquals("The test field on first level subdocument should have expected value.",
                firstLevelTestValue, returnedFirstLevelDocumentTestField.iterator().next());

        DocumentInterface returnedSecondLevelSubdoc = returnedFirstLevelSubdoc.getSubDocuments().iterator().next();
        Collection<String> returnedSecondLevelDocumentTestField = returnedSecondLevelSubdoc.getMetadata()
                .get(testFieldName);
        Assert.assertEquals("The single test field should have been added to the second level subdocument by the converter.",
                1, returnedSecondLevelDocumentTestField.size());
        Assert.assertEquals("The test field on second level subdocument should have expected value.",
                secondLevelTestValue, returnedSecondLevelDocumentTestField.iterator().next());

        DocumentInterface returnedThirdLevelSubdoc = returnedSecondLevelSubdoc.getSubDocuments().iterator().next();
        Collection<String> returnedThirdLevelDocumentTestField = returnedThirdLevelSubdoc.getMetadata()
                .get(testFieldName);
        Assert.assertEquals("The single test field should have been added to the third level subdocument by the converter.",
                1, returnedThirdLevelDocumentTestField.size());
        Assert.assertEquals("The test field on third level subdocument should have expected value.",
                thirdLevelTestValue, returnedThirdLevelDocumentTestField.iterator().next());
    }

    private DocumentWorkerChange addFieldChange(String fieldName, String fieldValue){
        DocumentWorkerChange docAddField = new DocumentWorkerChange();
        docAddField.addFields = new LinkedHashMap<>();
        DocumentWorkerFieldValue docAddFieldValue = new DocumentWorkerFieldValue();
        docAddFieldValue.data = fieldValue;
        docAddField.addFields.put(fieldName, Arrays.asList(docAddFieldValue));
        return docAddField;
    }

    private DocumentWorkerChange updateSubDocChange(String subdocRef, DocumentWorkerChange addFieldChange,
                                                    DocumentWorkerChange updateChildChange){
        DocumentWorkerChange updateSubDocChange = new DocumentWorkerChange();
        DocumentWorkerChange.UpdateSubdocumentParams updateSubdocParams = new DocumentWorkerChange.UpdateSubdocumentParams();
        updateSubdocParams.changes = new ArrayList<>();
        updateSubdocParams.changes.add(addFieldChange);
        if(updateChildChange!=null) {
            updateSubdocParams.changes.add(updateChildChange);
        }
        updateSubdocParams.index = 0;
        updateSubdocParams.reference = subdocRef;
        updateSubDocChange.updateSubdocument = updateSubdocParams;
        return updateSubDocChange;
    }
}
