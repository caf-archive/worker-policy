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
import com.github.cafdataprocessing.worker.policy.shared.PolicyWorkerConstants;
import com.google.common.collect.Multimap;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.document.DocumentWorkerChange;
import com.hpe.caf.worker.document.DocumentWorkerChange.InsertSubdocumentParams;
import com.hpe.caf.worker.document.DocumentWorkerChange.RemoveSubdocumentParams;
import com.hpe.caf.worker.document.DocumentWorkerChange.SetReferenceParams;
import com.hpe.caf.worker.document.DocumentWorkerChange.UpdateSubdocumentParams;
import com.hpe.caf.worker.document.DocumentWorkerChangeLogEntry;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerFailure;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import static com.hpe.caf.worker.document.DocumentWorkerFieldEncoding.utf8;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.codec.binary.Base64;

public final class CompositeDocumentWorkerTaskUpdater
{

    private CompositeDocumentWorkerTaskUpdater()
    {
    }

    public static void updateDocument(final DocumentInterface document, final List<DocumentWorkerChangeLogEntry> changeLog)
    {
        changeLog.stream().filter((entry) -> (entry.changes != null)).forEachOrdered((entry) -> {
            entry.changes.forEach((change) -> {
                applyChange(change, document);
            });
        });
    }

    private static void applyChange(final DocumentWorkerChange change, final DocumentInterface document)
    {
        Objects.requireNonNull(change);
        Objects.requireNonNull(document);

        final SetReferenceParams setReferenceParams = change.setReference;
        if (setReferenceParams != null) {
            setReference(setReferenceParams.value, document);
        }

        final Map<String, List<DocumentWorkerFieldValue>> addFieldsParam = change.addFields;
        if (addFieldsParam != null) {
            addFields(addFieldsParam, document);
        }

        final Map<String, List<DocumentWorkerFieldValue>> setFieldsParam = change.setFields;
        if (setFieldsParam != null) {
            setFields(setFieldsParam, document);
        }

        final List<String> removeFieldsParam = change.removeFields;
        if (removeFieldsParam != null) {
            removeFields(removeFieldsParam, document);
        }

        final DocumentWorkerFailure addFailureParam = change.addFailure;
        if (addFailureParam != null) {
            addFailures(addFailureParam, document);
        }
        final List<DocumentWorkerFailure> setFailuresParam = change.setFailures;
        if (setFailuresParam != null) {
            setFailures(setFailuresParam, document);
        }

        final DocumentWorkerDocument addSubdocumentParam = change.addSubdocument;
        if (addSubdocumentParam != null) {
            addSubDocument(addSubdocumentParam, document);
        }

        final InsertSubdocumentParams insertSubdocumentParams = change.insertSubdocument;
        if (insertSubdocumentParams != null) {
            // The ability to insert at an index is not supported in a DocumentInterface object as subfiles are held
            // in an unordered Collection, the document is simply added to the Collection.
            addSubDocument(insertSubdocumentParams.subdocument, document);
        }

        final UpdateSubdocumentParams updateSubdocumentParams = change.updateSubdocument;
        if (updateSubdocumentParams != null) {
            updateSubDocument(document, updateSubdocumentParams.changes, updateSubdocumentParams.reference);
        }

        final RemoveSubdocumentParams removeSubdocumentParams = change.removeSubdocument;
        if (removeSubdocumentParams != null) {
            removeSubdocument(document, removeSubdocumentParams.reference);
        }
    }

    private static void setReference(final String reference, final DocumentInterface document)
    {
        document.setReference(reference);
    }

    private static void addFields(final Map<String, List<DocumentWorkerFieldValue>> fieldChanges, final DocumentInterface document)
    {
        for (Map.Entry<String, List<DocumentWorkerFieldValue>> fieldEntryChange : fieldChanges.entrySet()) {
            for (DocumentWorkerFieldValue fieldValue : fieldEntryChange.getValue()) {
                if (fieldValue != null) {
                    final String data = fieldValue.data;
                    if (data != null) {
                        DocumentWorkerFieldEncoding encoding = fieldValue.encoding;

                        if (encoding == null) {
                            encoding = utf8;
                        }

                        switch (encoding) {
                            case utf8: {
                                ConverterUtils.addMetadataToDocument(fieldEntryChange.getKey(), data, document);
                            }
                            break;

                            case storage_ref: {
                                ConverterUtils.addMetadataReferenceToDocument(
                                    fieldEntryChange.getKey(), ReferencedData.getReferencedData(data), document);
                            }
                            break;

                            case base64: {
                                byte[] databyte = Base64.decodeBase64(data);
                                ConverterUtils.addMetadataReferenceToDocument(
                                    fieldEntryChange.getKey(), ReferencedData.getWrappedData(databyte), document);
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

    private static void setFields(final Map<String, List<DocumentWorkerFieldValue>> fieldChanges, final DocumentInterface document)
    {
        removeFields(new ArrayList(fieldChanges.keySet()), document);

        addFields(fieldChanges, document);
    }

    private static void removeFields(final List<String> fieldnames, final DocumentInterface document)
    {
        for (final String fieldname : fieldnames) {
            ConverterUtils.removeMetadataFromDocument(fieldname, document);
            ConverterUtils.removeMetadataReferenceFromDocument(fieldname, document);
        }
    }

    private static void addSubDocument(final DocumentWorkerDocument documentWorkerDocument, final DocumentInterface document)
    {
        final DocumentInterface subDoc = document.addSubDocument(documentWorkerDocument.reference);
        addFields(documentWorkerDocument.fields, subDoc);
        if (documentWorkerDocument.subdocuments != null) {
            documentWorkerDocument.subdocuments.forEach((doc) -> {
                addSubDocument(doc, document);
            });
        }
    }

    private static void removeSubdocument(final DocumentInterface document, final String reference)
    {
        document.removeSubdocument(reference);
    }

    private static void updateSubDocument(final DocumentInterface document, final List<DocumentWorkerChange> changeLog,
                                          final String reference)
    {
        for (final DocumentInterface doc : document.getSubDocuments()) {
            if (doc.getReference().equals(reference)) {
                final List<DocumentWorkerChangeLogEntry> changeEntryList = createEntryList(changeLog);
                updateDocument(document, changeEntryList);
            }
        }
    }

    private static void addFailures(final DocumentWorkerFailure failure, final DocumentInterface document)
    {
        // Get the document metadata
        final Multimap<String, String> documentMetadata = document.getMetadata();
        final String errorCode = failure.failureId;
        final String errorMessage = failure.failureMessage;

        // If there are previous failure details, then remove them
        documentMetadata.removeAll(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_CODE);
        documentMetadata.removeAll(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_MESSAGE);

        // Add the new failure details to the document
        documentMetadata.put(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_CODE, errorCode);
        documentMetadata.put(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_MESSAGE, errorMessage);

        // Record the failure in a second set of (multi-value) fields which will not get removed by subsequent logic
        documentMetadata.put(PolicyWorkerConstants.POLICYWORKER_ERROR_CODE, errorCode);
        documentMetadata.put(PolicyWorkerConstants.POLICYWORKER_ERROR_MESSAGE, errorMessage);
    }

    private static void setFailures(final List<DocumentWorkerFailure> failures, final DocumentInterface document)
    {
        document.getMetadata();
        failures.forEach((failure) -> {
            document.getMetadata().keySet().stream().forEach(key -> {
                if (key.equals(PolicyWorkerConstants.POLICYWORKER_FAILURE_ERROR_CODE)) {
                    document.getMetadata().get(key).clear();
                }
            });
            addFailures(failure, document);
        });
    }

    private static List<DocumentWorkerChangeLogEntry> createEntryList(final List<DocumentWorkerChange> changeLog)
    {
        final List<DocumentWorkerChangeLogEntry> entryList = new ArrayList<>();
        final DocumentWorkerChangeLogEntry change = new DocumentWorkerChangeLogEntry();
        change.changes = changeLog;
        entryList.add(change);
        return entryList;
    }
}
