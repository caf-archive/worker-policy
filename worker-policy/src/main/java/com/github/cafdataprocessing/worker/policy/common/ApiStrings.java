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
package com.github.cafdataprocessing.worker.policy.common;

/**
 * Contains a list of public strings, which define constants used in our policyworkers.
 */
public class ApiStrings {
    //field of all collection sequences which have had any policies applied
    public final static String POLICYWORKER_COLLECTION_SEQUENCE = "POLICYWORKER_COLLECTIONSEQUENCE";
    //field of policies applied for this collection sequence id
    public final static String POLICYWORKER_COLLECTION_SEQUENCE_POLICY = "POLICYWORKER_COLLECTIONSEQUENCE_POLICIES_";
    //field of all collection sequences which had ALL policies applied.
    public final static String POLICYWORKER_COLLECTION_SEQUENCES_COMPLETED = "POLICYWORKER_COLLECTIONSEQUENCE_COMPLETED";
}
