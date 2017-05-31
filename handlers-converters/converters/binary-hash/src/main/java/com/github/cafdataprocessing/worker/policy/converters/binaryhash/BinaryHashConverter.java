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
package com.github.cafdataprocessing.worker.policy.converters.binaryhash;

import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterInterface;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterRuntime;
import com.github.cafdataprocessing.worker.policy.converters.ConverterUtils;
import com.google.common.collect.Multimap;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.worker.binaryhash.BinaryHashWorkerConstants;
import com.hpe.caf.worker.binaryhash.BinaryHashWorkerResult;

/**
 * Converter for the binary hash worker
 */
public class BinaryHashConverter implements PolicyWorkerConverterInterface
{
    @Override
    public void updateSupportedClassifierVersions(Multimap<String, Integer> multimap)
    {
        multimap.put(BinaryHashWorkerConstants.WORKER_NAME, BinaryHashWorkerConstants.WORKER_API_VER);
    }

    /**
     * New convert method which implements PolicyWorkerConverterInterface and takes a runtime object.
     *
     * @param runtime - implementation of PolicyWorkerConverterRuntime.
     * @throws CodecException
     */
    public void convert(PolicyWorkerConverterRuntime runtime) throws CodecException
    {
        final BinaryHashWorkerResult taskResponse = runtime.deserialiseData(BinaryHashWorkerResult.class);

        if (taskResponse == null) {
            runtime.recordError("NULL_PTR", "BinaryHashWorkerResult is null");
            return;
        }

        String fieldName = BinaryHashFields.BINARY_HASH_FIELD_NAME;
        if (taskResponse.hashResult != null) {
            ConverterUtils.addMetadataToDocument(fieldName, taskResponse.hashResult, runtime.getDocument());
        }
    }
}
