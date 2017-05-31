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
package com.github.cafdataprocessing.entity.fields;

import java.util.Map;

/**
 * Defines the information which has been changed on a document during DataProcessing.
 * Note this defines the contract and hence serialization of this class.
 * Remember it is possible to pass in information directly in json.
 * 
 * @author getty
 */
public class DocumentProcessingRecord {

    /**
     * Changes made on a document to either metadata or metadataReferences
     */
    public Map<String, FieldChangeRecord> metadataChanges;
    
    /**
     * Changes made to a document reference.
     */
    public FieldChangeRecord referenceChange;
}
