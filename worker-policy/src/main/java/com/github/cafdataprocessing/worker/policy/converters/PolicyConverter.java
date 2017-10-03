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

import com.github.cafdataprocessing.worker.policy.shared.CollectionPolicy;

/**
 * Logic for converting between Core Policy 'Policy' and worker-policy-shared 'Policy' types.
 */
public class PolicyConverter {

    public CollectionPolicy convert(com.github.cafdataprocessing.corepolicy.common.dto.CollectionPolicy collectionPolicy) {
        if(collectionPolicy == null) {
            return null;
        }

        CollectionPolicy newPolicy = new CollectionPolicy();
        newPolicy.setId(collectionPolicy.getId());
        newPolicy.setName(collectionPolicy.getName());

        return newPolicy;
    }
}
