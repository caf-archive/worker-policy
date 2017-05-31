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
package com.github.cafdataprocessing.worker.policy.converterinterface;

import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import com.google.common.collect.Multimap;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.InvalidTaskException;

public interface PolicyWorkerConverterInterface
{
    /**
     * Updates the specified map with details on which workers the converter supports
     */
    void updateSupportedClassifierVersions(Multimap<String, Integer> supportedMap);

    /**
     * Called to provide the TaskData if it is not provided by the context
     */
    default TaskData convert(PolicyWorkerConverterRuntimeBase runtime)
        throws PolicyWorkerConverterException, CodecException, InvalidTaskException
    {
        return null;
    }

    /**
     * Updates the document based on the data returned by the worker
     */
    default void convert(PolicyWorkerConverterRuntime runtime)
        throws PolicyWorkerConverterException, CodecException, InvalidTaskException
    {
    }
}
