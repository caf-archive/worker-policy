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
package com.github.cafdataprocessing.worker.policy.converters.classification;

import com.github.cafdataprocessing.corepolicy.common.DocumentFields;

/**
 * Field values for use with this converter.
 */
public class ClassificationWorkerConverterFields {
    public static final String CLASSIFICATION_ID_FIELD = "CAF_CLASSIFICATION_ID";
    public static final String CLASSIFICATION_NAME_FIELD = "CAF_CLASSIFICATION_NAME";
    @Deprecated
    public static final String CLASSIFICATION_POLICYNAME_FIELD = "POLICY_MATCHED_POLICYNAME";
    @Deprecated
    public static final String CLASSIFICATION_POLICYID_FIELD = "POLICY_MATCHED_POLICYID";
    public static final String CLASSIFICATION_MATCHED_COLLECTION = DocumentFields.SearchField_MatchedCollection;
    public static final String WORKER_NAME = "ClassificationWorker";

    @Deprecated
    public static String getMatchedConditionField(long collectionId) {
        return DocumentFields.getMatchedConditionField(collectionId);
    }
}
