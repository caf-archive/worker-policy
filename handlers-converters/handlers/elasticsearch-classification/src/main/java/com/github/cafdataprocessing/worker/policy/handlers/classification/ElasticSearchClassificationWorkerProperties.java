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
package com.github.cafdataprocessing.worker.policy.handlers.classification;

import com.github.cafdataprocessing.worker.policy.handlers.shared.HandlerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;

/**
 * Reads in properties to control behaviour of the elastic search classification handler.
 */
@Configuration
@PropertySources({
        @PropertySource("classpath:elasticsearchclassificationworkerhandler.properties")
})
public class ElasticSearchClassificationWorkerProperties implements HandlerProperties {
    @Autowired
    private Environment environment;

    public String getTaskQueueName() {
        String queue = environment.getProperty("elasticsearchclassificationworkerhandler.taskqueue");
        return queue != null ? queue : "ElasticSearchClassificationWorkerInput";
    }

    public String getDiagnosticsQueueName() {
        String queue = environment.getProperty("elasticsearchclassificationworkerhandler.diagnosticstaskqueue");
        return queue;
    }
}
