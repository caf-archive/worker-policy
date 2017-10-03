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
package com.github.cafdataprocessing.worker.policy.shared;

/**
 * The category that a field on a document falls into. Different DocumentProcessingFieldTypes
 * may fall under a single Category
 */
public enum DocumentProcessingFieldCategory {
    /**
     * Represents the Document Reference
     */
    REFERENCE,
    /**
     * Represents Metadata and Metadata References on a Document
     */
    ALLMETADATA;
}
