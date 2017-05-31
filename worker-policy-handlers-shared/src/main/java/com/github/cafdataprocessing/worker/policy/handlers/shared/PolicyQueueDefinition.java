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
package com.github.cafdataprocessing.worker.policy.handlers.shared;

/**
 * Shared base class that policy handlers extend to grant specification of task and diagnostics queues
 *
 */
public class PolicyQueueDefinition {

    /*
        Optional. Name of the queue to send a policy items to.
     */
    public String queueName;

    /*
        Optional. Name of the diagnostics queue to send a failed policy items to.
     */
    public String diagnosticsQueueName;
}
