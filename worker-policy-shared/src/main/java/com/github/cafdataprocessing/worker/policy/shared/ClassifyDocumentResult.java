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
package com.github.cafdataprocessing.worker.policy.shared;

import java.util.Collection;

/**
 *
 */
public class ClassifyDocumentResult {
    private String reference;

    private Collection<MatchedCollection> matchedCollections;

    private Long collectionIdAssignedByDefault;

    private Collection<Long> incompleteCollections;

    private Collection<Long> resolvedPolicies;

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Collection<MatchedCollection> getMatchedCollections() {
        return matchedCollections;
    }

    public void setMatchedCollections(Collection<MatchedCollection> matchedCollections) {
        this.matchedCollections = matchedCollections;
    }

    public Long getCollectionIdAssignedByDefault() {
        return collectionIdAssignedByDefault;
    }

    public void setCollectionIdAssignedByDefault(Long collectionIdAssignedByDefault) {
        this.collectionIdAssignedByDefault = collectionIdAssignedByDefault;
    }

    public Collection<Long> getIncompleteCollections() {
        return incompleteCollections;
    }

    public void setIncompleteCollections(Collection<Long> incompleteCollections) {
        this.incompleteCollections = incompleteCollections;
    }

    public Collection<Long> getResolvedPolicies() {
        return resolvedPolicies;
    }

    public void setResolvedPolicies(Collection<Long> resolvedPolicies) {
        this.resolvedPolicies = resolvedPolicies;
    }
}