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

import java.util.Set;

/**
 * Object definition for a DocumentCollection which describes a policy to execute on document if condition is matched.
 */
public class DocumentCollection {
    public Long id;

    private String projectId;

    public String name;

    public String description;

    public Condition condition;

    public Set<Long> policyIds;

    public String fingerprint;

}
