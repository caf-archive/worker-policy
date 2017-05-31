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
package com.github.cafdataprocessing.worker.policy.testing.shared;

import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.worker.policy.shared.Document;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Helper class for common Document operations in testing.
 */
public class TestDocumentHelper {
    public static Document createDocument(Multimap<String, String> metadata)
    {
        Document createdDocument = new Document();
        createdDocument.setReference(UUID.randomUUID().toString());
        createdDocument.setMetadata(metadata);

        createdDocument.setDocuments(new ArrayList<>());
        return createdDocument;
    }
}
