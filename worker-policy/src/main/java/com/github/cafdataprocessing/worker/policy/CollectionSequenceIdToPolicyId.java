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
package com.github.cafdataprocessing.worker.policy;

/**
 * Holds a collection sequence id to a policy id
 */
public class CollectionSequenceIdToPolicyId {

    private Long collectionSequenceId;

    private Long policyId;

    public CollectionSequenceIdToPolicyId(Long collectionSequenceId, Long policyId) {
        this.collectionSequenceId = collectionSequenceId;
        this.policyId = policyId;
    }

    public Long getCollectionSequenceId() {
        return collectionSequenceId;
    }

    public void setCollectionSequenceId(Long collectionSequenceId) {
        this.collectionSequenceId = collectionSequenceId;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }
}
