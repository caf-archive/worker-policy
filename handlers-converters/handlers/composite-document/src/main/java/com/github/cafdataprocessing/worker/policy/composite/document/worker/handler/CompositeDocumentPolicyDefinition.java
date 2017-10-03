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
package com.github.cafdataprocessing.worker.policy.composite.document.worker.handler;

import com.github.cafdataprocessing.worker.policy.handlers.shared.PolicyQueueDefinition;
import com.hpe.caf.worker.document.DocumentWorkerChangeLogEntry;

import java.util.*;

public class CompositeDocumentPolicyDefinition extends PolicyQueueDefinition
{

    public String workerName;

    /*  Optional. List of change log entries that represent the changes a worker has requested be performed to the document */
    public List<DocumentWorkerChangeLogEntry> changeLog;

    /*  Optional. For supplying additional information */
    public Map<String, Object> customData = new HashMap<>();
}
