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
package com.github.cafdataprocessing.worker.policy.testconverters;

import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterException;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterInterface;
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterRuntime;
import com.google.common.collect.Multimap;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.InvalidTaskException;

/**
 * A converter for testing purposes that
 */
public class CodecExceptionConverter implements PolicyWorkerConverterInterface
{
    public static String SUPPORTED_CLASSIFIER = "CodecExceptionHandler";
    public static int SUPPORTED_VERSION = 1;

    @Override
    public void updateSupportedClassifierVersions(Multimap<String, Integer> supportedMap)
    {
        supportedMap.put(SUPPORTED_CLASSIFIER, SUPPORTED_VERSION);
    }

    @Override
    public void convert(PolicyWorkerConverterRuntime runtime) throws PolicyWorkerConverterException, CodecException, InvalidTaskException
    {
        throw new CodecException("This handler should be throwing an exception.", new RuntimeException());
    }
}
