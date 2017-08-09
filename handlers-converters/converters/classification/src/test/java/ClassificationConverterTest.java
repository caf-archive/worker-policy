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
import com.github.cafdataprocessing.worker.policy.PolicyWorkerConverterRuntimeImpl;
import com.github.cafdataprocessing.worker.policy.converter.qa.ConvertHelper;
import com.github.cafdataprocessing.worker.policy.converter.qa.TestWorkerConverterRuntimeImpl;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterException;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterRuntime;
import com.github.cafdataprocessing.worker.policy.shared.*;
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.worker.policy.converters.classification.ClassificationWorkerConverter;
import com.github.cafdataprocessing.worker.policy.converters.classification.ClassificationWorkerConverterFields;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.DecodeMethod;
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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Unit Test for Classification Worker Converter
 */
@RunWith(MockitoJUnitRunner.class)
public class ClassificationConverterTest {

    @Mock
    DataStore dataStore;

    /**
     * Tests that expected classification fields are applied to the document by the coverter.
     * @throws CodecException
     * @throws InvalidTaskException
     * @throws PolicyWorkerConverterException
     */
    @Test
    public void testExternalClassificationResult() throws CodecException, InvalidTaskException,
            PolicyWorkerConverterException
    {
        String rootReference = UUID.randomUUID().toString();
        TaskResponse result = new TaskResponse();

        Collection<CollectionPolicy> policyCollection = new ArrayList<>();
        CollectionPolicy policy = createCollectionPolicy();
        policyCollection.add(policy);

        MatchedCollection matchedCollection = createMatchedCollection();
        matchedCollection.setPolicies(policyCollection);

        addMatchedCondition(matchedCollection, rootReference);

        Collection<ClassifyDocumentResult> classifyDocumentResults =
                createClassifyDocumentResultsFromCollection(matchedCollection);

        result.setClassifiedDocuments(classifyDocumentResults);

        ReferencedData referencedData = ReferencedData.getReferencedData(rootReference);

        DocumentInterface doc = ConvertHelper.RunConvert(result, referencedData, this.dataStore, TaskStatus.RESULT_SUCCESS,
                new ClassificationWorkerConverter(), "ClassificationWorker");

        verifyMatchedCollectionFieldsPresent(doc, matchedCollection, policy);
    }

    /**
     * Tests that expected classification fields are applied to root document and its children by the converter.
     * @throws PolicyWorkerConverterException
     * @throws CodecException
     * @throws InvalidTaskException
     */
    @Test
    public void testMatchedSubDocumentsGetFieldsApplied() throws PolicyWorkerConverterException, CodecException, InvalidTaskException {
        String rootReference = UUID.randomUUID().toString();
        TaskResponse result = new TaskResponse();
        DocumentInterface rootDocument = new Document();
        rootDocument.setReference(rootReference);
        String firstLevelSubDocRef = UUID.randomUUID().toString();
        DocumentInterface firstLevelSubDocument = rootDocument.addSubDocument(firstLevelSubDocRef);
        String secondLevelSubDocRef = UUID.randomUUID().toString();
        DocumentInterface secondLevelSubDocument = firstLevelSubDocument.addSubDocument(secondLevelSubDocRef);

        //set up a match against the root document
        Collection<CollectionPolicy> rootOnlyPolicyCollection = new ArrayList<>();
        CollectionPolicy rootOnlyPolicy = createCollectionPolicy("rootOnly_"+UUID.randomUUID().toString());
        rootOnlyPolicyCollection.add(rootOnlyPolicy);

        MatchedCollection rootMatchedCollection = createMatchedCollection();
        rootMatchedCollection.setPolicies(rootOnlyPolicyCollection);
        addMatchedCondition(rootMatchedCollection, rootReference);

        //set up a match against first level sub doc
        Collection<CollectionPolicy> firstLevelPolicyCollection = new ArrayList<>();
        CollectionPolicy firstLevelPolicy = createCollectionPolicy("firstLevel_"+UUID.randomUUID().toString());
        firstLevelPolicyCollection.add(firstLevelPolicy);

        MatchedCollection firstLevelMatchedCollection = createMatchedCollection();
        firstLevelMatchedCollection.setPolicies(firstLevelPolicyCollection);
        addMatchedCondition(firstLevelMatchedCollection, firstLevelSubDocRef);

        //set up a match against second level sub doc
        Collection<CollectionPolicy> secondLevelPolicyCollection = new ArrayList<>();
        CollectionPolicy secondLevelPolicy = createCollectionPolicy("secondLevel_"+UUID.randomUUID().toString());
        secondLevelPolicyCollection.add(secondLevelPolicy);

        MatchedCollection secondLevelMatchedCollection = createMatchedCollection();
        secondLevelMatchedCollection.setPolicies(secondLevelPolicyCollection);
        addMatchedCondition(secondLevelMatchedCollection, secondLevelSubDocRef);

        //set up a match against root, first level and second level
        Collection<CollectionPolicy> allLevelsPolicyCollection = new ArrayList<>();
        CollectionPolicy allLevelsPolicy = createCollectionPolicy("allLevels_"+UUID.randomUUID().toString());
        allLevelsPolicyCollection.add(allLevelsPolicy);

        MatchedCollection allLevelsMatchedCollection = createMatchedCollection();
        allLevelsMatchedCollection.setPolicies(allLevelsPolicyCollection);
        addMatchedCondition(allLevelsMatchedCollection, rootReference);
        addMatchedCondition(allLevelsMatchedCollection, firstLevelSubDocRef);
        addMatchedCondition(allLevelsMatchedCollection, secondLevelSubDocRef);

        Collection<ClassifyDocumentResult> classifyDocumentResults =
                createClassifyDocumentResultsFromCollections(
                        Arrays.asList(rootMatchedCollection, firstLevelMatchedCollection,
                                secondLevelMatchedCollection, allLevelsMatchedCollection)
                );
        result.setClassifiedDocuments(classifyDocumentResults);
        PolicyWorkerConverterRuntime runtime = new TestConverterRuntime(rootDocument, result);

        ClassificationWorkerConverter converter = new ClassificationWorkerConverter();
        converter.convert(runtime);

        verifyMatchedCollectionFieldsPresent(runtime.getDocument(), rootMatchedCollection, rootOnlyPolicy);
        verifyMatchedCollectionFieldsPresent(rootDocument, allLevelsMatchedCollection, allLevelsPolicy);
        verifyMatchedCollectionFieldsPresent(rootDocument, firstLevelMatchedCollection, firstLevelPolicy);
        verifyMatchedCollectionFieldsPresent(rootDocument, secondLevelMatchedCollection, secondLevelPolicy);

        verifyMatchedCollectionFieldsPresent(firstLevelSubDocument, firstLevelMatchedCollection, firstLevelPolicy);
        verifyMatchedCollectionFieldsPresent(firstLevelSubDocument, allLevelsMatchedCollection, allLevelsPolicy);
        verifyMatchedCollectionFieldsNotPresent(firstLevelSubDocument, secondLevelMatchedCollection, secondLevelPolicy);
        verifyMatchedCollectionFieldsNotPresent(firstLevelSubDocument, rootMatchedCollection, rootOnlyPolicy);

        verifyMatchedCollectionFieldsPresent(secondLevelSubDocument, secondLevelMatchedCollection, secondLevelPolicy);
        verifyMatchedCollectionFieldsPresent(secondLevelSubDocument, allLevelsMatchedCollection, allLevelsPolicy);
        verifyMatchedCollectionFieldsNotPresent(secondLevelSubDocument, firstLevelMatchedCollection, firstLevelPolicy);
        verifyMatchedCollectionFieldsNotPresent(secondLevelSubDocument, rootMatchedCollection, rootOnlyPolicy);
    }

    private void addMatchedCondition(MatchedCollection matchedCollection, String matchedDocReference){
        MatchedCondition matchedCondition = new MatchedCondition();
        matchedCondition.setName("Example Condition");
        matchedCondition.setId(1L);
        matchedCondition.setReference(matchedDocReference);
        if(matchedCollection.getMatchedConditions()==null){
            matchedCollection.setMatchedConditions(new ArrayList<>());
        }
        matchedCollection.getMatchedConditions().add(matchedCondition);
    }

    private Collection<ClassifyDocumentResult> createClassifyDocumentResultsFromCollection(
            MatchedCollection matchedCollection){
        return createClassifyDocumentResultsFromCollections(Arrays.asList(matchedCollection));
    }

    private Collection<ClassifyDocumentResult> createClassifyDocumentResultsFromCollections(
            Collection<MatchedCollection> matchedCollections){
        Collection<ClassifyDocumentResult> classifyDocumentResults = new ArrayList<>();
        ClassifyDocumentResult classifyDocumentResult = new ClassifyDocumentResult();
        classifyDocumentResult.setMatchedCollections(matchedCollections);
        classifyDocumentResults.add(classifyDocumentResult);
        return classifyDocumentResults;
    }

    private CollectionPolicy createCollectionPolicy(){
        return createCollectionPolicy(UUID.randomUUID().toString());
    }

    private CollectionPolicy createCollectionPolicy(String policyName){
        CollectionPolicy policy = new CollectionPolicy();
        policy.setName(policyName);
        policy.setId(ThreadLocalRandom.current().nextLong());
        return policy;
    }

    private MatchedCollection createMatchedCollection(){
        MatchedCollection matchedCollection = new MatchedCollection();
        matchedCollection.setId(ThreadLocalRandom.current().nextLong());
        matchedCollection.setName("Example Condition");
        return matchedCollection;
    }

    private void verifyMatchedCollectionFieldsPresent(DocumentInterface doc, MatchedCollection matchedCollection,
                                             CollectionPolicy policy){
        Multimap<String, String> metaData = doc.getMetadata();
        Optional<String> value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_MATCHED_COLLECTION)
                .stream().filter(md -> md.equals(matchedCollection.getId().toString())).findFirst();
        Assert.assertTrue("Document should have POLICY_MATCHED_COLLECTION in metadata with expected value.",
                value.isPresent());

        value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_POLICYID_FIELD).stream()
                .filter(md -> md.equals(policy.getId().toString())).findFirst();
        Assert.assertTrue("Document should have POLICY_MATCHED_POLICYID in metadata with expected value.",
                value.isPresent());

        value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_POLICYNAME_FIELD).stream()
                .filter(md -> md.equals(policy.getName())).findFirst();
        Assert.assertTrue("Document should have POLICY_MATCHED_POLICYNAME in metadata with expected value.",
                value.isPresent());

        value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_ID_FIELD).stream()
                .filter(md -> md.equals(policy.getId().toString())).findFirst();
        Assert.assertTrue("Document should have "+ClassificationWorkerConverterFields.CLASSIFICATION_ID_FIELD+
                " in metadata with expected value.", value.isPresent());

        value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_NAME_FIELD).stream()
                .filter(md -> md.equals(policy.getName())).findFirst();
        Assert.assertTrue("Document should have"+ClassificationWorkerConverterFields.CLASSIFICATION_NAME_FIELD+
                " in metadata with expected value.", value.isPresent());
    }

    private void verifyMatchedCollectionFieldsNotPresent(DocumentInterface doc, MatchedCollection matchedCollection,
                                                      CollectionPolicy policy){
        Multimap<String, String> metaData = doc.getMetadata();
        Optional<String> value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_MATCHED_COLLECTION)
                .stream().filter(md -> md.equals(matchedCollection.getId().toString())).findFirst();
        Assert.assertTrue("Document should not have POLICY_MATCHED_COLLECTION in metadata with expected value.",
                !value.isPresent());

        value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_POLICYID_FIELD).stream()
                .filter(md -> md.equals(policy.getId().toString())).findFirst();
        Assert.assertTrue("Document should not have POLICY_MATCHED_POLICYID in metadata with expected value.",
                !value.isPresent());

        value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_POLICYNAME_FIELD).stream()
                .filter(md -> md.equals(policy.getName())).findFirst();
        Assert.assertTrue("Document should not have POLICY_MATCHED_POLICYNAME in metadata with expected value.",
                !value.isPresent());

        value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_ID_FIELD).stream()
                .filter(md -> md.equals(policy.getId().toString())).findFirst();
        Assert.assertTrue("Document should not have "+ClassificationWorkerConverterFields.CLASSIFICATION_ID_FIELD+
                " in metadata with expected value.", !value.isPresent());

        value = metaData.get(ClassificationWorkerConverterFields.CLASSIFICATION_NAME_FIELD).stream()
                .filter(md -> md.equals(policy.getName())).findFirst();
        Assert.assertTrue("Document should not have"+ClassificationWorkerConverterFields.CLASSIFICATION_NAME_FIELD+
                " in metadata with expected value.", !value.isPresent());
    }
}
