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
package com.github.cafdataprocessing.worker.policy.common;

import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.worker.policy.shared.PolicyWorkerConstants;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * Utility class for getting / setting information from the field that we place onto the document.
 * Grouped these together so we know what we have placed usually as temporary fields onto the document.
 *
 * As there are 2 types of documents in policy worker ( corepolicy common.document and policyworker.shared.document )
 * which both use different multimaps, code is duplicated where we read / write to the map.  If possible wrap this logic into
 * a single helper class which can abstact the map type away.
 */
public class DocumentFields {

    private final static Logger logger = LoggerFactory.getLogger(DocumentFields.class);

    public final static String getTagExecutedPoliciesPerCollectionSequence(Long collectionSequenceId)
    {
        return ApiStrings.POLICYWORKER_COLLECTION_SEQUENCE_POLICY + collectionSequenceId.toString();
    }

    public final static void addExecutedPolicyInfoToDocument(Long collectionSequenceId, Collection<Long> resolvedPolicies,
                                                             com.github.cafdataprocessing.corepolicy.common.Document executedDocument) {
        Multimap<String, String> metadata = executedDocument.getMetadata();
        boolean collectionSequenceAlreadyExists = false;

        for (String fieldValue : metadata.get(ApiStrings.POLICYWORKER_COLLECTION_SEQUENCE)) {
            try {
                if (Strings.isNullOrEmpty(fieldValue))
                    continue;

                long matchedCollectionSequenceId = Long.parseLong(fieldValue);

                if (matchedCollectionSequenceId != collectionSequenceId)
                    continue;

                collectionSequenceAlreadyExists = true;
            } catch (NumberFormatException e) {
                logger.debug("Unexpected value parsing document for matched collection sequence information, skipping: " + fieldValue);
            }

        }

        // add on the collection sequence id onto the mv list.
        if (!collectionSequenceAlreadyExists) {
            metadata.put(ApiStrings.POLICYWORKER_COLLECTION_SEQUENCE, collectionSequenceId.toString());
        }

        Collection<String> policiesToBeAdded = new ArrayList<>();

        //add the specified policies to the list which may already have ran for this CollectionSequence.
        if (!collectionSequenceAlreadyExists) {

            policiesToBeAdded.addAll(resolvedPolicies.stream().map(Object::toString).collect(Collectors.toList()));

        } else {

            // this uses the PolicyWorker metadata, if it ever matches that in corepolicy,
            // then use getPoliciesAlreadyExecutedForSequence instead.
            Collection<Long> policyIdsExecuted = DocumentFields.getPoliciesAlreadyExecutedForStringMap(collectionSequenceId, metadata);

            // resolve any items not yet added.
            policiesToBeAdded.addAll(resolvedPolicies.stream().map(Object::toString).collect(Collectors.toList()));
            policiesToBeAdded.removeAll(policyIdsExecuted);
        }

        if ( policiesToBeAdded.size() > 0 ) {
            metadata.putAll(DocumentFields.getTagExecutedPoliciesPerCollectionSequence(collectionSequenceId), policiesToBeAdded);
        }
    }

    public final static List<Long> getCollectionSequencesAlreadyCompleted( com.github.cafdataprocessing.corepolicy.common.Document document ){

        Multimap<String, String> metadata = document.getMetadata();

        // get ids from property = "POLICYWORKER_COLLECTIONSEQUENCE_COMPLETED" - mv field of sequenceids.
        return getIdsFromMetadataPropertyStringMap(metadata, ApiStrings.POLICYWORKER_COLLECTION_SEQUENCES_COMPLETED);
    }

    public final static List<Long> getCollectionSequencesAlreadyStarted( com.github.cafdataprocessing.corepolicy.common.Document document ){

        Multimap<String, String> metadata = document.getMetadata();

        // get ids from property = "POLICYWORKER_COLLECTIONSEQUENCE" - mv field of sequenceids.
        return getIdsFromMetadataPropertyStringMap(metadata, ApiStrings.POLICYWORKER_COLLECTION_SEQUENCE);
    }

    /***
     * Add the collection sequence id when we have completed executing all resolved policies for its classification.
     * @param document
     * @param collectionSequenceId
     */
    public final static void addCollectionSequenceCompletedInfo( com.github.cafdataprocessing.corepolicy.common.Document document, Long collectionSequenceId )
    {
        Multimap<String, String> metadata = document.getMetadata();

        // get all completed collection sequences - "POLICYWORKER_COLLECTION_SEQUENCES_COMPLETED" - mv field
        List<Long> csIds = getIdsFromMetadataPropertyStringMap(metadata, ApiStrings.POLICYWORKER_COLLECTION_SEQUENCES_COMPLETED);

        // if the specified sequence already exists in this list, just return, we have nothing to do.
        if ( csIds.contains( collectionSequenceId ))
            return;

        // add on the collection sequence id onto the mv list.
        metadata.put(ApiStrings.POLICYWORKER_COLLECTION_SEQUENCES_COMPLETED, collectionSequenceId.toString() );
    }

    /***
     * Returns the policies already executed for a given collection sequence, by checking document metadata
     * @param collectionSequenceId
     * @param metadata
     * @return
     */
    public static Collection<Long> getPoliciesAlreadyExecutedForStringMap(Long collectionSequenceId, Multimap<String, String> metadata) {
        return getIdsFromMetadataPropertyStringMap(metadata, DocumentFields.getTagExecutedPoliciesPerCollectionSequence(collectionSequenceId));
    }

    /***
     * Returns the policies already executed for a given collection sequence, by checking document metadata
     * @param collectionSequenceId
     * @param metadata
     * @return
     */
    public static Collection<Long> getPoliciesAlreadyExecutedForObjectMap(Long collectionSequenceId, Multimap<String, String> metadata) {
        return getIdsFromMetadataPropertyStringMap(metadata, DocumentFields.getTagExecutedPoliciesPerCollectionSequence(collectionSequenceId));
    }

    /**
     * Returns the list of temporary metadata fields used on the document
     * @return
     */
    public static Collection<String> getListOfKnownTemporaryData( Multimap<String, String> metadata ) {

        Collection<String> knownFields = new ArrayList<>();

        knownFields.addAll( Arrays.asList(ApiStrings.POLICYWORKER_COLLECTION_SEQUENCE,
                ApiStrings.POLICYWORKER_COLLECTION_SEQUENCES_COMPLETED, PolicyWorkerConstants.POLICYWORKER_FAILURE));

        if ( metadata == null )
            return knownFields;

        // Now one of our fields, is made up with a key of the matching collection sequence.
        // Work out all of these field names.
        List<Long> csIds = getIdsFromMetadataPropertyStringMap(metadata, ApiStrings.POLICYWORKER_COLLECTION_SEQUENCE);

        // now just add a field, per id.
        knownFields.addAll(csIds.stream().map(DocumentFields::getTagExecutedPoliciesPerCollectionSequence).collect(Collectors.toList()));

        return knownFields;
    }

    /**
     * Removes fields that are added to track working progress through Collection Sequence execution.
     * @param document The Document to remove temporary working data from.
     * @return The temporary data that was removed.
     */
    public static Multimap<String, String> removeTemporaryWorkingData(com.github.cafdataprocessing.corepolicy.common.Document document )
    {
        // using same map type as the used inside our document object.
        Multimap<String, String> temporaryData = ArrayListMultimap.create();
        Multimap<String, String> docData = document.getMetadata();

        getListOfKnownTemporaryData(docData).stream().filter(propName -> docData.containsKey(propName)).forEach(propName -> {
            temporaryData.putAll(propName, docData.get(propName));
            docData.removeAll(propName);
        });

        return temporaryData;
    }

    /**
     * Removes fields that are added to track working progress through Collection Sequence execution.
     * @param document The Document to remove temporary working data from.
     * @return The temporary data that was removed.
     */
    public static Multimap<String, String> removeTemporaryWorkingData(Document document){
        // using same map type as the used inside our document object.
        Multimap<String, String> temporaryData = ArrayListMultimap.create();
        Multimap<String, String> docData = document.getMetadata();

        getListOfKnownTemporaryData(docData).stream().filter(propName -> docData.containsKey(propName)).forEach(propName -> {
            temporaryData.putAll(propName, docData.get(propName));
            docData.removeAll(propName);
        });

        return temporaryData;
    }

    /**
     * Returns all the fields added to the Document to tracking working progress through Workflow/Collection Sequence execution.
     * @param document The Document to return temporary working data from.
     * @return The temporary data on the Document.
     */
    public static Multimap<String, String> getTemporaryWorkingData(Document document){
        Multimap<String, String> temporaryData = ArrayListMultimap.create();
        Multimap<String, String> docData = document.getMetadata();

        getListOfKnownTemporaryData(docData).stream().filter(propName -> docData.containsKey(propName)).forEach(propName -> {
            temporaryData.putAll(propName, docData.get(propName));
        });

        return temporaryData;
    }

    public static void reapplyTemporaryWorkingData(com.github.cafdataprocessing.corepolicy.common.Document document,
                                                   Multimap<String, String> temporaryData )
    {
        Multimap<String, String> docData = document.getMetadata();

        for( String propName : temporaryData.keySet() )
        {
            docData.putAll( propName, temporaryData.get(propName));
        }
    }

    /********************************************
     * Private Methods
     *******************************************/


    private static List<Long> getIdsFromMetadataPropertyStringMap(Multimap<String, String> metadata, String propertyName) {
        List<Long> ids = new ArrayList<>();

        for(String fieldValue : metadata.get(propertyName)) {
            try {
                if (Strings.isNullOrEmpty(fieldValue))
                    continue;

                long matchedCollectionSequenceId = Long.parseLong(fieldValue);

                if (ids.contains(matchedCollectionSequenceId))
                    continue;

                // otherwise, place this new id on the list.
                ids.add(matchedCollectionSequenceId);
            } catch (NumberFormatException e) {
                logger.debug("Unexpected value parsing document for matched collection sequences information, skipping: " + fieldValue);
            }
        }
        return ids;
    }
}
