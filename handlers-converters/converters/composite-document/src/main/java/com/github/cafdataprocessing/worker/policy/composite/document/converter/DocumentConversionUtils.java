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
package com.github.cafdataprocessing.worker.policy.composite.document.converter;

import com.github.cafdataprocessing.worker.policy.converters.ConverterUtils;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import org.apache.commons.codec.binary.Base64;

import java.util.List;
import java.util.Map;

import static com.hpe.caf.worker.document.DocumentWorkerFieldEncoding.utf8;

/**
 * Utility methods for applying data from a DocumentWorkerDocument to a Policy Worker Document.
 */
class DocumentConversionUtils {
    /**
     * Adds the specified field values to the policy worker document metadata and metadata references, converting them
     * as required.
     * @param fields Fields to add. Field values with utf8 or null encoding will be added to the document metadata.
     *               Those with storage_ref or base64 encoding will be added to metadata references on the document.
     * @param document Policy Document to update.
     */
    static void addFields(final Map<String, List<DocumentWorkerFieldValue>> fields, final DocumentInterface document)
    {
        if(fields==null){
            return;
        }
        for (Map.Entry<String, List<DocumentWorkerFieldValue>> fieldEntry : fields.entrySet()) {
            for (DocumentWorkerFieldValue fieldValue : fieldEntry.getValue()) {
                if (fieldValue != null) {
                    final String data = fieldValue.data;
                    if (data != null) {
                        DocumentWorkerFieldEncoding encoding = fieldValue.encoding;

                        if (encoding == null) {
                            encoding = utf8;
                        }

                        switch (encoding) {
                            case utf8: {
                                ConverterUtils.addMetadataToDocument(fieldEntry.getKey(), data, document);
                            }
                            break;

                            case storage_ref: {
                                ConverterUtils.addMetadataReferenceToDocument(
                                        fieldEntry.getKey(), ReferencedData.getReferencedData(data), document);
                            }
                            break;

                            case base64: {
                                byte[] databyte = Base64.decodeBase64(data);
                                ConverterUtils.addMetadataReferenceToDocument(
                                        fieldEntry.getKey(), ReferencedData.getWrappedData(databyte), document);
                            }
                            break;

                            default: {
                                throw new RuntimeException("Encoding type not recognised, encoding should be of type base64, utf8 or storage_ref.");
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Converts and adds the specified DocumentWorkerDocument as a sub-document of the provided policy worker document.
     * @param documentWorkerDocument The DocumentWorkerDocument representation of the sub-document that should be added.
     * @param document The Policy Worker document to update with the sub-document..
     */
    static void addSubDocument(final DocumentWorkerDocument documentWorkerDocument, final DocumentInterface document)
    {
        final DocumentInterface subDoc = document.addSubDocument(documentWorkerDocument.reference);
        DocumentConversionUtils.addFields(documentWorkerDocument.fields, subDoc);
        if (documentWorkerDocument.subdocuments != null) {
            documentWorkerDocument.subdocuments.forEach((doc) -> {
                addSubDocument(doc, subDoc);
            });
        }
    }
}
