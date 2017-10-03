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
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.worker.policy.common.DocumentFields;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;


/**
 * Tests for DocumentFields class
 */
@RunWith(MockitoJUnitRunner.class)
public class DocumentFieldsTest {

    @Test
    public void testRemovedTemporaryFields() {
        Document document = Mockito.mock(Document.class);

        // build up the document with expected temporary fields.
        Multimap<String, String> knownTempFields = createDummyMetadata();

        // We need to take a copy onto the document object as if we remove from the results map, we have nothing
        // to compare against!
        Multimap<String, String> docData = copyMap(knownTempFields);
        docData.put("MyFixedString", "AnyValue");
        docData.put("MyNullValue", null);

        Mockito.when(document.getMetadata()).thenReturn(docData);

        // each field now has unique information present.
        // check remove temporary working data, not only removes this from the the document, but that
        // it also returns it correctly in the map.
        Multimap<String, String> tempWorkingData = DocumentFields.removeTemporaryWorkingData(document);

        for (String tempField : tempWorkingData.keySet()) {
            Assert.assertEquals("Values should match for field: " + tempField, tempWorkingData.get(tempField), knownTempFields.get(tempField));

            // also make sure each temp field we hold is not on the main doc any longer.
            Assert.assertFalse("Document shouldn't contain this temp field: " + tempField, document.getMetadata().containsKey(tempField));
        }

        Assert.assertEquals("Should only have our fixed field left.", 2, document.getMetadata().keys().size());
    }

    private Multimap<String, String> createDummyMetadata() {
        Multimap<String, String> knownTempFields = ArrayListMultimap.create();

        // Ensure we put null, 1 and >1 values for our keys.
        for (
                String knownField
                : DocumentFields.getListOfKnownTemporaryData(null))

        {
            switch (knownTempFields.size()) {
                case 0:
                    // first item goes on with no value.
                    knownTempFields.put(knownField, null);
                    continue;
                case 1:
                    // second item gets 2 entries.
                    knownTempFields.put(knownField, getUniqueString("Second Value"));

                    // let it go into default limb to add 2 values for this item
                default:

                    // all others get a single value
                    knownTempFields.put(knownField, getUniqueString("UniqueTestFieldContent"));
            }
        }

        return knownTempFields;
    }


    @Test
    public void testReApplyTempData() {

        Document document = Mockito.mock(Document.class);

        // build up the document with expected temporary fields.
        Multimap<String, String> knownTempFields = createDummyMetadata();

        // Create a new map to house the new info...
        Multimap<String, String> docData = ArrayListMultimap.create();

        Mockito.when(document.getMetadata()).thenReturn(docData);

        // each field now has unique information present.
        // check remove temporary working data, not only removes this from the the document, but that
        // it also returns it correctly in the map.
        DocumentFields.reapplyTemporaryWorkingData(document, knownTempFields);


        // now check that the final document holds all the temp fields as well.
        Assert.assertEquals("Tempfields size must match reapplied metadata size", knownTempFields.size(), docData.size());

        for (String tempField
                : knownTempFields.keySet())

        {
            Assert.assertEquals("Values should match for field: " + tempField, knownTempFields.get(tempField), docData.get(tempField));
        }
    }

    private Multimap<String, String> copyMap(Multimap<String, String> knownTempFields) {
        Multimap<String, String> retDataMap = ArrayListMultimap.create();
        for( String knownField : knownTempFields.keySet()) {
            retDataMap.putAll ( knownField, knownTempFields.get(knownField));
        }
        return retDataMap;
    }

    private static String getUniqueString( String strPrefix ) {
        return  strPrefix + UUID.randomUUID().toString();
    }
}
