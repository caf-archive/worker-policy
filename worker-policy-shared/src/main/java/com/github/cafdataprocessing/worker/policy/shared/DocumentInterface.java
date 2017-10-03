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

import com.google.common.collect.Multimap;
import com.github.cafdataprocessing.entity.fields.DocumentProcessingRecord;
import com.hpe.caf.util.ref.ReferencedData;
import java.util.Collection;

/**
 * Interface to be used when accessing the policy worker shared document information.
 * 
 * @author getty
 */
public interface DocumentInterface {

    public Multimap<String, String> getMetadata();
 
    public Multimap<String, ReferencedData> getMetadataReferences();
    public Collection<DocumentInterface> getSubDocuments();
    public void removeSubdocument(String reference);
    public DocumentInterface addSubDocument(String reference);
    public String getReference();
    public void setReference(String reference);
    
    public DocumentProcessingRecord createPolicyDataProcessingRecord();
    public DocumentProcessingRecord getPolicyDataProcessingRecord();
    public void deletePolicyDataProcessingRecord();
     
}
