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
import com.github.cafdataprocessing.worker.policy.converter.qa.ConvertHelper;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterException;
import com.github.cafdataprocessing.worker.policy.shared.*;
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.worker.policy.converters.classification.ClassificationWorkerConverter;
import com.github.cafdataprocessing.worker.policy.converters.classification.ClassificationWorkerConverterFields;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.util.ref.ReferencedData;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

/**
 * Unit Test for Classification Worker Converter
 */
@RunWith(MockitoJUnitRunner.class)
public class ClassificationConverterTest {

    @Mock
    DataStore dataStore;

    @Test
    public void testExternalClassificationResult() throws CodecException, InvalidTaskException,
            PolicyWorkerConverterException
    {
        String reference = UUID.randomUUID().toString();
        TaskResponse result = new TaskResponse();

        Collection<CollectionPolicy> policyCollection = new ArrayList<>();
        CollectionPolicy policy = new CollectionPolicy();
        policy.setName("Example Policy");
        policy.setId(1L);
        policyCollection.add(policy);

        MatchedCollection matchedCollection = new MatchedCollection();
        matchedCollection.setId(2L);
        matchedCollection.setName("Example Condition");
        matchedCollection.setPolicies(policyCollection);


        MatchedCondition matchedCondition = new MatchedCondition();
        matchedCondition.setName("Example Condition");
        matchedCondition.setId(1L);
        Collection<MatchedCondition> matchedConditions = new ArrayList<>();
        matchedConditions.add(matchedCondition);
        matchedCollection.setMatchedConditions(matchedConditions);

        Collection<ClassifyDocumentResult> classifyDocumentResults = new ArrayList<>();
        ClassifyDocumentResult classifyDocumentResult = new ClassifyDocumentResult();
        classifyDocumentResult.setMatchedCollections(Arrays.asList(matchedCollection));
        classifyDocumentResults.add(classifyDocumentResult);

        result.setClassifiedDocuments(classifyDocumentResults);

        ReferencedData referencedData = ReferencedData.getReferencedData(reference);

        DocumentInterface doc = ConvertHelper.RunConvert(result, referencedData, this.dataStore, TaskStatus.RESULT_SUCCESS,
                new ClassificationWorkerConverter(), "ClassificationWorker");

        Multimap<String, String> metaData = doc.getMetadata();

        Optional<String> value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_MATCHED_COLLECTION).stream().findFirst();
        Assert.assertTrue("Document should have POLICY_MATCHED_COLLECTION in meta data", value.isPresent());
        Assert.assertEquals("Should have matched the classification collection", String.valueOf(matchedCollection.getId()), value.get());

        value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_POLICYID_FIELD).stream().findFirst();
        Assert.assertTrue("Document should have POLICY_MATCHED_POLICYID in meta data", value.isPresent());
        Assert.assertEquals("Should have executed Tagging Policy", String.valueOf(policy.getId()), value.get());

        value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_POLICYNAME_FIELD).stream().findFirst();
        Assert.assertTrue("Document should have POLICY_MATCHED_POLICYNAME in meta data", value.isPresent());
        Assert.assertEquals("Should have executed Tagging Policy", policy.getName(), value.get());

        value = metaData.get(ClassificationWorkerConverterFields.getMatchedConditionField(matchedCollection.getId())).stream().findFirst();
        Assert.assertTrue("Document should have " + ClassificationWorkerConverterFields.getMatchedConditionField(matchedCollection.getId()) + " in meta data", value.isPresent());
        Assert.assertEquals("Should have matched condition", String.valueOf(matchedCondition.getId()), value.get());
    }
}
