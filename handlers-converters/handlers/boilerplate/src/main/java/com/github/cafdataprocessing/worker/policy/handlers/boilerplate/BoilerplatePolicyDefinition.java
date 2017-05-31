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
package com.github.cafdataprocessing.worker.policy.handlers.boilerplate;

import com.github.cafdataprocessing.worker.policy.handlers.shared.PolicyQueueDefinition;
import com.hpe.caf.worker.boilerplateshared.RedactionType;

import java.util.HashSet;
import java.util.Set;

/**
 * The properties that make up a Boilerplate Policy.
 */
public class BoilerplatePolicyDefinition extends PolicyQueueDefinition {
    /*
        ID of a Tag. The Policy Item will be evaluated against the Boilerplate Expressions with this Tag. Either this or 'expressionIds' must be set on policy definition.
     */
    public Long tagId;
    /*
        Boilerplate Expression IDs to evaluate against Policy Item. Either this or 'tagId' must be set on policy definition.
     */
    public Set<Long> expressionIds;
    /*
        Optional. Fields that will be evaluated for Boilerplate text. Defaults to CONTENT if none provided.
     */
    public Set<String> fields = new HashSet<String>();
    /*
        Optional. Indicates action to perform on values that match boilerplate expression. Defaults to "DoNothing".
     */
    public RedactionType redactionType = RedactionType.DO_NOTHING;
    /*
        Optional. Whether the detail of the boilerplate expression matches should be returned. Defaults to 'true'
     */
    public boolean returnMatches = true;

    /**
     * The field names and expressions to define email key content segregation.
     */
    public EmailSegregationRules emailSegregationRules;

    /**
     * Optional. Email Signature Detection mode with optional sender.
     */
    public EmailSignatureDetection emailSignatureDetection;
}
