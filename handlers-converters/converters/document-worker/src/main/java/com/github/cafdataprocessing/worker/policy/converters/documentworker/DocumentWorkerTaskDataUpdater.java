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
package com.github.cafdataprocessing.worker.policy.converters.documentworker;

import com.github.cafdataprocessing.worker.policy.converterinterface.PolicyWorkerConverterRuntime;
import com.github.cafdataprocessing.worker.policy.converters.ConverterUtils;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.document.DocumentWorkerAction;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.document.DocumentWorkerFieldChanges;
import com.hpe.caf.worker.document.DocumentWorkerResult;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import static com.hpe.caf.worker.document.DocumentWorkerFieldEncoding.*;

public final class DocumentWorkerTaskDataUpdater
{
    private DocumentWorkerTaskDataUpdater()
    {
    }

    static void updateDocumentMetadata(PolicyWorkerConverterRuntime runtime, DocumentWorkerResult taskResponse)
    {
        Map<String, DocumentWorkerFieldChanges> taskResponseMap = taskResponse.fieldChanges;
        DocumentInterface document = runtime.getDocument();

        for (Map.Entry<String, DocumentWorkerFieldChanges> fieldEntryChange : taskResponseMap.entrySet()) {

            String fieldName = fieldEntryChange.getKey();
            DocumentWorkerFieldChanges fieldChanges = fieldEntryChange.getValue();

            if (fieldChanges.action == DocumentWorkerAction.replace) {
                ConverterUtils.removeMetadataFromDocument(fieldName, document);
                ConverterUtils.removeMetadataReferenceFromDocument(fieldName, document);
            }

            List<DocumentWorkerFieldValue> fieldValues = fieldChanges.values;

            if (fieldValues != null) {
                for (DocumentWorkerFieldValue fieldValue : fieldValues) {
                    String data = fieldValue.data;
                    if (data != null) {
                        DocumentWorkerFieldEncoding encoding = fieldValue.encoding;

                        if (encoding == null) {
                            encoding = utf8;
                        }

                        switch (encoding) {
                            case utf8: {
                                ConverterUtils.addMetadataToDocument(fieldName, data, document);
                            }
                            break;

                            case storage_ref: {
                                ConverterUtils.addMetadataReferenceToDocument(
                                    fieldName, ReferencedData.getReferencedData(data), document);
                            }
                            break;

                            case base64: {
                                byte[] databyte = Base64.decodeBase64(data);
                                ConverterUtils.addMetadataReferenceToDocument(
                                    fieldName, ReferencedData.getWrappedData(databyte), document);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
}
