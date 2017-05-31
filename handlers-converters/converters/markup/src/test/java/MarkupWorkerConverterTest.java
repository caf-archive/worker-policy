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
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.worker.policy.converters.markup.MarkupWorkerConverter;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.util.ref.DataSource;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.markup.MarkupWorkerResult;
import com.hpe.caf.worker.markup.MarkupWorkerStatus;
import com.hpe.caf.worker.markup.NameValuePair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Test for the MarkupWorkerConverter
 */
@RunWith(MockitoJUnitRunner.class)
public class MarkupWorkerConverterTest
{
    @Mock
    DataStore dataStore;

    @Mock
    DataSource dataSource;

    /**
     * Test the functionality of the MarkupWorkerConverter. The converter's main functionality is to add the results from the worker onto
     * the document metadata. Assertions ensure the fields were added correctly.
     *
     * @throws InvalidTaskException
     * @throws IOException
     */
    @Test
    public void updateDocumentTest() throws InvalidTaskException, IOException, CodecException, PolicyWorkerConverterException
    {
        ReferencedData referencedData = ReferencedData.getReferencedData(UUID.randomUUID().toString());
        Multimap<String, String> expectedMap = CaseInsensitiveMultimap.create();

        // Create a MarkupWorkerResult
        MarkupWorkerResult markupWorkerResult = new MarkupWorkerResult();
        markupWorkerResult.workerStatus = MarkupWorkerStatus.COMPLETED;
        markupWorkerResult.fieldList = createOutputFields(expectedMap);

        DocumentInterface doc = ConvertHelper.RunConvert(markupWorkerResult, referencedData, this.dataStore, TaskStatus.RESULT_SUCCESS,
                new MarkupWorkerConverter(), "MarkupWorker");

        Assert.assertNotNull("The converted response should not be null", doc);

        Multimap<String, String> metadata = doc.getMetadata();

        for (NameValuePair p : markupWorkerResult.fieldList) {
            final String name = p.name;
            Assert.assertTrue("The metadata document should contain the key: " + name, metadata.containsKey(name));

            Collection<String> resultValuesForCurrentKey = metadata.get(name);
            Collection<String> expectedValuesForCurrentKey = expectedMap.get(name);

            Assert.assertTrue("Result values should match expected values for key: " + name,
                    HashMultiset.create(resultValuesForCurrentKey).equals(HashMultiset.create(expectedValuesForCurrentKey)));
        }
    }

    /**
     * Test asserting the error message will be placed on the document with a null task response.
     *
     * @throws PolicyWorkerConverterException
     * @throws CodecException
     * @throws InvalidTaskException
     */
    @Test
    public void taskResponseNull_newImplementation() throws PolicyWorkerConverterException, CodecException, InvalidTaskException
    {
        ReferencedData referencedData = ReferencedData.getReferencedData(UUID.randomUUID().toString());
        MarkupWorkerResult result = null;

        DocumentInterface doc = ConvertHelper.RunConvert(result, referencedData, this.dataStore, TaskStatus.RESULT_FAILURE,
                new MarkupWorkerConverter(), "MarkupWorker");

        Assert.assertNotNull("The converted response should not be null", doc);

        Multimap<String, String> metadata = doc.getMetadata();

        Assert.assertTrue("The document metadata should contain the \"POILICYWORKER_ERROR_CODE\" field with code \"NULL_PTR\"",
                metadata.containsEntry("POLICYWORKER_ERROR_CODE", "NULL_PTR"));

        Assert.assertTrue("The document metadata should contain the \"POILICYWORKER_ERROR_MESSAGE\" field with message \"MarkupWorkerResult is null\"",
                metadata.containsEntry("POLICYWORKER_ERROR_MESSAGE", "MarkupWorkerResult is null"));
    }

    /**
     * Generate the fields to dummy the response from the worker.
     *
     * @return
     */
    private List<NameValuePair> createOutputFields(Multimap expectedMap)
    {
        final String SECTION_ID = "SECTION_ID";
        final String SECTION_SORT = "SECTION_SORT";
        final String PARENT_ID = "PARENT_ID";
        final String ROOT_ID = "ROOT_ID";
        final String MULTIPLE_TO_VALUES = "MULTIPLE_TO_VALUES";

        List<NameValuePair> pairs = new ArrayList<>();

        NameValuePair p1 = constructPair("SECTION_ID", "a4e7fa8100f5f6fb");
        NameValuePair p2 = constructPair("SECTION_SORT", "Sent: 20 July 2016 17:51");
        NameValuePair p3 = constructPair("PARENT_ID", "f5aaaeca8206aa23");
        NameValuePair p4 = constructPair("ROOT_ID", "6c86d7d70c3d14f8");
        NameValuePair p5 = constructPair("MULTIPLE_TO_VALUES", "To: Reid, Andy &lt;andrew.reid@hpe.com&gt;; Hardy, Dermot &lt;dermot.hardy@hpe.com&gt;");
        NameValuePair p6 = constructPair("MULTIPLE_TO_VALUES", "To: Reid, Andy &lt;andrew.reid@hpe.com&gt;; Hardy, Dermot &lt;dermot.hardy@hpe.com&gt;");
        NameValuePair p7 = constructPair("MULTIPLE_TO_VALUES", "To: Paul, Navamoni &lt;paul.navamoni@hpe.com&gt;; Hardy, Dermot &lt;dermot.hardy@hpe.com&gt;");

        expectedMap.put(SECTION_ID, "a4e7fa8100f5f6fb");
        expectedMap.put(SECTION_SORT, "Sent: 20 July 2016 17:51");
        expectedMap.put(PARENT_ID, "f5aaaeca8206aa23");
        expectedMap.put(ROOT_ID, "6c86d7d70c3d14f8");
        expectedMap.put(MULTIPLE_TO_VALUES, "To: Reid, Andy &lt;andrew.reid@hpe.com&gt;; Hardy, Dermot &lt;dermot.hardy@hpe.com&gt;");
        expectedMap.put(MULTIPLE_TO_VALUES, "To: Reid, Andy &lt;andrew.reid@hpe.com&gt;; Hardy, Dermot &lt;dermot.hardy@hpe.com&gt;");
        expectedMap.put(MULTIPLE_TO_VALUES, "To: Paul, Navamoni &lt;paul.navamoni@hpe.com&gt;; Hardy, Dermot &lt;dermot.hardy@hpe.com&gt;");

        pairs.add(p1);
        pairs.add(p2);
        pairs.add(p3);
        pairs.add(p4);
        pairs.add(p5);
        pairs.add(p6);
        pairs.add(p7);

        return pairs;
    }

    private static NameValuePair constructPair(String name, String value)
    {
        NameValuePair nameValuePair = new NameValuePair();
        nameValuePair.name = name;
        nameValuePair.value = value;
        return nameValuePair;
    }
}
