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
package com.github.cafdataprocessing.worker.policy.converters.boilerplate;

import com.github.cafdataprocessing.worker.policy.converters.ConverterUtils;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.google.common.collect.Multimap;
import com.hpe.caf.policyworker.policyboilerplatefields.PolicyBoilerplateFields;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.boilerplateshared.BoilerplateWorkerConstants;
import com.hpe.caf.worker.boilerplateshared.response.BoilerplateMatch;
import com.hpe.caf.worker.boilerplateshared.response.BoilerplateResult;
import com.hpe.caf.worker.boilerplateshared.response.BoilerplateWorkerResponse;
import com.hpe.caf.worker.boilerplateshared.response.SignatureExtractStatus;

import java.util.Collection;
import java.util.Map;

/**
 * Updates data with the result returned from boilerplate worker.
 */
public class TaskDataUpdater {
    public static void updateTaskData(DocumentInterface document, BoilerplateWorkerResponse boilerplateWorkerResult){
        Map<String, BoilerplateResult> boilerplateResults = boilerplateWorkerResult.getTaskResults();
        for (String fieldName : boilerplateResults.keySet()) {
            BoilerplateResult boilerplateResult = boilerplateResults.get(fieldName);

            //if there were no boilerplate matches for the field then no match fields
            Collection<BoilerplateMatch> boilerplateMatches = boilerplateResult.getMatches();
            //add match fields onto document
            updateWithMatchFields(document, boilerplateMatches);
            //replace old fields with modified field content
            updateWithModifiedFields(document, fieldName, boilerplateResult.getData());
            //Add any groupedMatches data, such as email key content or signatures.
            updateWithEmailKeyContent(document, boilerplateResult.getGroupedMatches());

            updateWithEmailSignatureExtractionStatus(document, boilerplateResult);

            updateWithEmailSignatures(document, boilerplateResult);
        }
    }

    /*
        Add the boilerplate matches onto the document as fields
     */
    private static void updateWithMatchFields(DocumentInterface document,
                                              Collection<BoilerplateMatch> boilerplateMatches) {
        if (boilerplateMatches == null || boilerplateMatches.isEmpty()) {
            return;
        }
        for (BoilerplateMatch boilerplateMatch : boilerplateMatches) {
            String boilerplateId = boilerplateMatch.getBoilerplateId().toString();
            ConverterUtils.addMetadataToDocument(BoilerplateFields.BOILERPLATE_MATCH_ID, boilerplateId, document);
            ConverterUtils.addMetadataToDocument(BoilerplateFields.getMatchValueFieldName(boilerplateId),
                    boilerplateMatch.getValue(), document);
        }
    }

    private static void updateWithModifiedFields(DocumentInterface document, String fieldName,
                                                 Collection<ReferencedData> modifiedDataCollection) {
        if (modifiedDataCollection == null || modifiedDataCollection.isEmpty()) {
            return;
        }
        // remove old field(s) with unmodified value (note that we are not cleaning up any caf storage
        // references at the moment and will need to in the future when deletion is supported)
        ConverterUtils.removeMetadataFromDocument(fieldName, document);
        ConverterUtils.removeMetadataReferenceFromDocument(fieldName, document);

        for (ReferencedData modifiedData : modifiedDataCollection) {
            ConverterUtils.addMetadataReferenceToDocument(fieldName, modifiedData, document);
        }
    }

    private static void updateWithEmailKeyContent(DocumentInterface document, Multimap<String, ReferencedData> groupedMatches) {
        if (groupedMatches == null || groupedMatches.isEmpty()) {
            return;
        }
        //Add primary email content
        Multimap<String, String> documentMetaData = document.getMetadata();
        String fieldName = documentMetaData.get(PolicyBoilerplateFields.POLICY_BOILERPLATE_PRIMARY_FIELDNAME).stream().findFirst().orElse(null);
        Collection<ReferencedData> emailContent = groupedMatches.get(BoilerplateWorkerConstants.PRIMARY_CONTENT);
        for (ReferencedData content : emailContent) {
            ConverterUtils.addMetadataReferenceToDocument(fieldName == null ? BoilerplateWorkerConstants.PRIMARY_CONTENT : fieldName, content, document);
        }

        //Add secondary email content
        fieldName = documentMetaData.get(PolicyBoilerplateFields.POLICY_BOILERPLATE_SECONDARY_FIELDNAME).stream().findFirst().orElse(null);
        emailContent = groupedMatches.get(BoilerplateWorkerConstants.SECONDARY_CONTENT);
        for (ReferencedData content : emailContent) {
            ConverterUtils.addMetadataReferenceToDocument(fieldName == null ? BoilerplateWorkerConstants.SECONDARY_CONTENT : fieldName, content, document);
        }

        //Add tertiary email content
        fieldName = documentMetaData.get(PolicyBoilerplateFields.POLICY_BOILERPLATE_TERTIARY_FIELDNAME).stream().findFirst().orElse(null);
        emailContent = groupedMatches.get(BoilerplateWorkerConstants.TERTIARY_CONTENT);
        for (ReferencedData content : emailContent) {
            ConverterUtils.addMetadataReferenceToDocument(fieldName == null ? BoilerplateWorkerConstants.TERTIARY_CONTENT : fieldName, content, document);
        }

        //Remote temp fields from document
        ConverterUtils.removeMetadataFromDocument(PolicyBoilerplateFields.POLICY_BOILERPLATE_PRIMARY_FIELDNAME, document);
        ConverterUtils.removeMetadataFromDocument(PolicyBoilerplateFields.POLICY_BOILERPLATE_SECONDARY_FIELDNAME, document);
        ConverterUtils.removeMetadataFromDocument(PolicyBoilerplateFields.POLICY_BOILERPLATE_TERTIARY_FIELDNAME, document);
    }

    private static void updateWithEmailSignatureExtractionStatus(DocumentInterface document, BoilerplateResult boilerplateResult) {

        SignatureExtractStatus signatureExtractStatus = boilerplateResult.getSignatureExtractStatus();

        // If a signature extraction status exists add the status to the document's metadata
        if (signatureExtractStatus == null) {
            return;
        } else {
            ConverterUtils.addMetadataToDocument(BoilerplateWorkerConstants.SIGNATURE_EXTRACTION_STATUS, signatureExtractStatus.toString(), document);
            return;
        }
    }

    private static void updateWithEmailSignatures(DocumentInterface document, BoilerplateResult boilerplateResult) {
        Multimap<String, String> documentMetaData = document.getMetadata();
        String fieldName = documentMetaData.get(PolicyBoilerplateFields.POLICY_BOILERPLATE_EMAIL_SIGNATURES_FIELDNAME).stream().findFirst().orElse(BoilerplateWorkerConstants.EXTRACTED_SIGNATURES);

        // Remote temp field from document
        ConverterUtils.removeMetadataFromDocument(PolicyBoilerplateFields.POLICY_BOILERPLATE_EMAIL_SIGNATURES_FIELDNAME, document);

        Multimap<String, ReferencedData> groupedMatches = boilerplateResult.getGroupedMatches();
        // If there are no grouped matches remove metadata from the temp field and return
        if (groupedMatches == null || groupedMatches.isEmpty()) {
            return;
        }

        Collection<ReferencedData> extractedEmailSignatures = groupedMatches.get(BoilerplateWorkerConstants.EXTRACTED_SIGNATURES);

        SignatureExtractStatus signatureExtractStatus = boilerplateResult.getSignatureExtractStatus();

        // If there are extracted signatures add each of them as metadata references to the document
        if (extractedEmailSignatures.size() > 0 && signatureExtractStatus.equals(SignatureExtractStatus.SIGNATURES_EXTRACTED)) {
            for (ReferencedData content : extractedEmailSignatures) {
                ConverterUtils.addMetadataReferenceToDocument(fieldName, content, document);
            }
        }
    }
}
