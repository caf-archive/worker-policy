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
import com.github.cafdataprocessing.corepolicy.multimap.utils.CaseInsensitiveMultimap;
import com.github.cafdataprocessing.worker.policy.converter.qa.ConvertHelper;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterException;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterInterface;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.worker.policy.converters.binaryhash.BinaryHashConverter;
import com.github.cafdataprocessing.worker.policy.converters.binaryhash.BinaryHashFields;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.util.ref.DataSource;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.binaryhash.BinaryHashWorkerResult;
import com.hpe.caf.worker.binaryhash.BinaryHashWorkerStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

/**
 * Tests for the Binary Hash Converter
 */
@RunWith(MockitoJUnitRunner.class)
public class BinaryHashConverterTest
{
    @Mock
    DataStore dataStore;
    @Mock
    DataSource dataSource;

    /**
        Test for the new BinaryHashConverter implementation.
     */
    @Test
    public void binaryHashTest() throws CodecException, InvalidTaskException, PolicyWorkerConverterException
    {
        ReferencedData referencedData = ReferencedData.getReferencedData(UUID.randomUUID().toString());
        Multimap<String, String> expectedMap = CaseInsensitiveMultimap.create();
        String fieldName = BinaryHashFields.BINARY_HASH_FIELD_NAME;

        BinaryHashWorkerResult binaryHashWorkerResult = new BinaryHashWorkerResult();
        binaryHashWorkerResult.workerStatus = BinaryHashWorkerStatus.COMPLETED;
        binaryHashWorkerResult.hashResult = createDummyBinaryHash(expectedMap, fieldName);

        PolicyWorkerConverterInterface converter = new BinaryHashConverter();
        String classifier = "BinaryHashConverter";

        DocumentInterface doc = ConvertHelper.RunConvert(binaryHashWorkerResult, referencedData, this.dataStore,
                TaskStatus.RESULT_SUCCESS, converter, classifier);

        Assert.assertNotNull("The converted response should not be null", doc);

        Assert.assertTrue(
                "The document should contain the fieldName",
                doc.getMetadata().containsKey(fieldName));

        Assert.assertEquals(
                "The field added to the document should match the expected one",
                expectedMap.get(fieldName), doc.getMetadata().get(fieldName));
    }


    /**
     * Test asserting the error message will be placed on the document with a null task response.
     *
     * @throws PolicyWorkerConverterException
     * @throws CodecException
     * @throws InvalidTaskException
     */
    @Test
    public void taskResponseNull() throws PolicyWorkerConverterException, CodecException, InvalidTaskException
    {
        ReferencedData referencedData = ReferencedData.getReferencedData(UUID.randomUUID().toString());
        BinaryHashWorkerResult result = null;

        DocumentInterface doc = ConvertHelper.RunConvert(result, referencedData, this.dataStore, TaskStatus.RESULT_FAILURE,
                new BinaryHashConverter(), "MarkupWorker");

        Assert.assertNotNull("The converted response should not be null", doc);

        Multimap<String, String> metadata = doc.getMetadata();

        Assert.assertTrue("The document metadata should contain the \"POILICYWORKER_ERROR_CODE\" field with code \"NULL_PTR\"",
                metadata.containsEntry("POLICYWORKER_ERROR_CODE", "NULL_PTR"));

        Assert.assertTrue("The document metadata should contain an error field reporting \"markupWorkerResult is null\"",
                metadata.containsEntry("POLICYWORKER_ERROR_MESSAGE", "BinaryHashWorkerResult is null"));
    }

    private String createDummyBinaryHash(Multimap<String, String> expectedMap, String fieldName)
    {
        String s = "cf23df2207d99a74fbe169e3eba035e633b65d94";
        expectedMap.put(fieldName, s);
        return s;
    }
}