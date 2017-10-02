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
package com.github.cafdataprocessing.worker.policy.converters;

import com.github.cafdataprocessing.worker.policy.shared.MatchedCollection;

import java.util.stream.Collectors;

/**
 * Logic for converting between Core Policy MatchedCollection and worker-policy-shared MatchedCollection types
 */
public class MatchedCollectionConverter {
    public MatchedCollection convert(com.github.cafdataprocessing.corepolicy.common.dto.MatchedCollection matchedCollection) {
        if(matchedCollection ==null){
            return null;
        }
        MatchedCollection newMatchedCollection = new MatchedCollection();

        newMatchedCollection.setId(matchedCollection.getId());
        newMatchedCollection.setName(matchedCollection.getName());
        if(matchedCollection.getPolicies() != null) {
            PolicyConverter policyConverter = new PolicyConverter();
            newMatchedCollection.setPolicies(matchedCollection.getPolicies().stream().map(policyConverter::convert).collect(Collectors.toList()));
        }
        if(matchedCollection.getMatchedConditions() != null) {
            MatchedConditionConverter matchedConditionConverter = new MatchedConditionConverter();
            newMatchedCollection.setMatchedConditions(matchedCollection.getMatchedConditions().stream().map(matchedConditionConverter::convert).collect(Collectors.toList()));
        }
        return newMatchedCollection;
    }
}
