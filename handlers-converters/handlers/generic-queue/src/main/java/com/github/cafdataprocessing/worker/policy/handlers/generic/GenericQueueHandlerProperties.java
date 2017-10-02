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
package com.github.cafdataprocessing.worker.policy.handlers.generic;

import com.github.cafdataprocessing.worker.policy.handlers.shared.HandlerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;

/**
 * Provides access to environment properties that configure behaviour of the Generic Queue Handler.
 */
@Configuration
@PropertySources({
        @PropertySource("classpath:genericqueuehandler.properties")
})
public class GenericQueueHandlerProperties implements HandlerProperties {
    @Autowired
    private Environment environment;

    @Override
    public String getTaskQueueName() {
        String genericQueueTaskQueue = environment.getProperty("genericqueuehandler.taskqueue");
        return genericQueueTaskQueue != null ? genericQueueTaskQueue : "GenericQueue";
    }
    
    @Override
    public String getDiagnosticsQueueName() {
        // no diagnostics queue name on generic queue -> its not a worker
        return null;
    }
}
