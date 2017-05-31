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
package com.github.cafdataprocessing.worker.policy.handlers.markup;

import com.github.cafdataprocessing.worker.policy.handlers.shared.PolicyQueueDefinition;
import com.hpe.caf.worker.markup.HashConfiguration;
import com.hpe.caf.worker.markup.OutputField;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MarkupPolicyDefinition extends PolicyQueueDefinition
{
    /*  Optional. Fields that will be evaluated during markup. */
    public Set<String> fields = new HashSet<>();

    /*  Optional. The hash configuration specifies the configuration for which fields to be included in the hash, the type of
     *  normalization to be performed per field and the hash function to be carried out on the list of fields. Multiple hash
     *  configurations can be specified. */
    public List<HashConfiguration> hashConfiguration;

    /*  Optional. The list of the output fields to be returned.  These fields will be extracted from the XML document as specified. */
    public List<OutputField> outputFields;

    /* Optional. Indicates the message being passed is an email. */
    public boolean isEmail;
}
