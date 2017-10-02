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
import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterRuntime;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.github.cafdataprocessing.worker.policy.shared.TaskResponse;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.DecodeMethod;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.codec.JsonCodec;

/**
 * Implementation of PolicyWorkerConverterRuntime for use with classification converter unit testing
 */
public class TestConverterRuntime implements PolicyWorkerConverterRuntime {
    private final DocumentInterface document;
    private final TaskResponse result;

    public TestConverterRuntime(DocumentInterface document, TaskResponse result){
        this.document = document;
        this.result = result;
    }

    @Override
    public DocumentInterface getDocument() {
        return document;
    }

    @Override
    public void recordError(String errorCodeMessage) {

    }

    @Override
    public void recordError(String errorCode, String errorMessage) {

    }

    @Override
    public TaskStatus getTaskStatus() {
        return TaskStatus.RESULT_SUCCESS;
    }

    @Override
    public <T> T deserialiseData(Class<T> clazz) throws CodecException {
        return (T) result;
    }

    @Override
    public <T> T deserialiseData(Class<T> clazz, DecodeMethod decodeMethod) throws CodecException {
        return null;
    }

    @Override
    public DataStore getDataStore() {
        return null;
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }
}
