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
package com.github.cafdataprocessing.worker.policy.converters.markup;

import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterException;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterInterface;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterRuntime;
import com.github.cafdataprocessing.worker.policy.converters.ConverterUtils;
import com.google.common.collect.Multimap;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.worker.markup.MarkupWorkerConstants;
import com.hpe.caf.worker.markup.MarkupWorkerResult;
import com.hpe.caf.worker.markup.NameValuePair;

/**
 * Converter for the Markup Worker
 */
public class MarkupWorkerConverter implements PolicyWorkerConverterInterface
{
    @Override
    public void updateSupportedClassifierVersions(Multimap<String, Integer> multimap)
    {
        multimap.put(MarkupWorkerConstants.WORKER_NAME, MarkupWorkerConstants.WORKER_API_VER);
    }

    @Override
    public void convert(PolicyWorkerConverterRuntime runtime) throws PolicyWorkerConverterException,
            CodecException, InvalidTaskException
    {
        MarkupWorkerResult taskResponse = runtime.deserialiseData(MarkupWorkerResult.class);

        if (taskResponse == null) {
            runtime.recordError("NULL_PTR", "MarkupWorkerResult is null");
            return;
        }

        if(taskResponse.workerStatus != null){
            // Add worker status returned for the Markup operation
            ConverterUtils.addMetadataToDocument(MarkupWorkerFields.MARKUPWORKER_STATUS, taskResponse.workerStatus.toString(), runtime.getDocument());
        }

        // Cycle through the list of key-value Pair objects in the task response and add elements to the metadata document.
        for (NameValuePair p : taskResponse.fieldList) {
            ConverterUtils.addMetadataToDocument(p.name, p.value, runtime.getDocument());
        }
    }
}
