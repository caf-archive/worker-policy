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

import com.github.cafdataprocessing.worker.policy.handlers.shared.HandlerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Provides access to environment properties that configure behaviour of the Boilerplate Handler.
 */
@Configuration
@PropertySources({
        @PropertySource("classpath:boilerplateworkerhandler.properties")
})
public class BoilerplateHandlerProperties implements HandlerProperties {
    @Autowired
    private Environment environment;

    public String getTaskQueueName() {
        String queue = environment.getProperty("boilerplateworkerhandler.taskqueue");
        return queue != null ? queue:"BoilerplateInput";
    }

    public String getDiagnosticsQueueName() {
        String queue = environment.getProperty("boilerplateworkerhandler.diagnosticstaskqueue");
        return queue;
    }

    public Collection<String> getDefaultFields(){
        String defaultFieldsAsString = environment.getProperty("boilerplateworkerhandler.defaultfields");
        if(defaultFieldsAsString==null) {
            return getDefaultFieldsInternal();
        }
        defaultFieldsAsString = defaultFieldsAsString.trim();
        if(defaultFieldsAsString.length() == 0) {
            return getDefaultFieldsInternal();
        }
        String[] defaultFields = defaultFieldsAsString.split(",");
        return Arrays.asList(defaultFields);
    }

    private Collection<String> getDefaultFieldsInternal() {
        Collection<String> defaults = new ArrayList<String>(1);
        defaults.add("CONTENT");
        return defaults;
    }
}
