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
package com.github.cafdataprocessing.worker.policy.converters.boilerplate;

import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterException;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterInterface;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterRuntime;
import com.google.common.collect.Multimap;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.worker.boilerplateshared.BoilerplateWorkerConstants;
import com.hpe.caf.worker.boilerplateshared.response.BoilerplateWorkerResponse;

/**
 * Converts result from Boilerplate Worker to a Policy Worker TaskData with fields added to represent the
 * Boilerplate Worker result.
 */
public class BoilerplateWorkerConverter implements PolicyWorkerConverterInterface
{
    @Override
    public void updateSupportedClassifierVersions(Multimap<String, Integer> multimap) {
        multimap.put(BoilerplateWorkerConstants.WORKER_NAME, BoilerplateWorkerConstants.WORKER_API_VERSION);
    }

    @Override
    public void convert(PolicyWorkerConverterRuntime runtime)
            throws PolicyWorkerConverterException, CodecException, InvalidTaskException
    {
        BoilerplateWorkerResponse taskResponse = runtime.deserialiseData(BoilerplateWorkerResponse.class);

        if(taskResponse == null){
            runtime.recordError("NULL_PTR", "boilerplateWorkerResponse is null");
            return;
        }

        TaskDataUpdater.updateTaskData(runtime.getDocument(), taskResponse);
    }
}
