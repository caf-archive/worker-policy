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
package com.github.cafdataprocessing.worker.policy;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.api.worker.WorkerFactoryProvider;

import java.util.Objects;

/**
 * Provides PolicyWorkerFactory.
 */
public class PolicyWorkerFactoryProvider implements WorkerFactoryProvider {

    public PolicyWorkerFactoryProvider() {

    }

    public WorkerFactory getWorkerFactory(ConfigurationSource config, DataStore dataStore, Codec codec) throws WorkerException
    {
        {
            try {
                Objects.requireNonNull(config);
                PolicyWorkerConfiguration testConfig = config.getConfiguration(PolicyWorkerConfiguration.class);
                return new PolicyWorkerFactory(codec, dataStore, testConfig);
            } catch ( ConfigurationException e ) {
                throw new WorkerException("Failed to create factory", e);
            }
        }
    }

    public int getWorkerThreads() {
        return 1;
    }
}
