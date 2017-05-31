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
package com.github.cafdataprocessing.worker.policy;

import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterRuntimeBase;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.DecodeMethod;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.WorkerTaskData;

public class PolicyWorkerConverterRuntimeBaseImpl implements PolicyWorkerConverterRuntimeBase
{
    private final Codec codec;
    private final DataStore dataStore;
    private final WorkerTaskData workerTask;

    public PolicyWorkerConverterRuntimeBaseImpl(
        final Codec codec,
        final DataStore dataStore,
        final WorkerTaskData workerTask
    )
    {
        this.codec = codec;
        this.dataStore = dataStore;
        this.workerTask = workerTask;
    }

    @Override
    public TaskStatus getTaskStatus()
    {
        return workerTask.getStatus();
    }

    @Override
    public <T> T deserialiseData(Class<T> clazz) throws CodecException
    {
        final byte[] data = workerTask.getData();

        return codec.deserialise(data, clazz);
    }

    @Override
    public <T> T deserialiseData(Class<T> clazz, DecodeMethod decodeMethod) throws CodecException
    {
        final byte[] data = workerTask.getData();

        return codec.deserialise(data, clazz, decodeMethod);
    }

    @Override
    public DataStore getDataStore()
    {
        return dataStore;
    }

    @Override
    public byte[] getData()
    {
        return workerTask.getData();
    }
}
