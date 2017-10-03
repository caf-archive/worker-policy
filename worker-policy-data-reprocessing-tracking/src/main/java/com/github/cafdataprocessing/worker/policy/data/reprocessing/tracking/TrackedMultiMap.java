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
package com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking;

import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.exceptions.TrackedChangeTypeException;
import com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.internal.TrackedDocumentInternal;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;
import com.github.cafdataprocessing.worker.policy.shared.DocumentProcessingFieldType;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * TrackedMultimap, provided a tracking implementation to the Multimap interface.
 * This class will wrap whatever type of Multimap is provided, and track all changes being made to the items within it,
 * into a policyDataProcessingRecord field.
 *
 * @author trevor.getty@hpe.com
 * @param <K> The type of the key of the multimap.
 * @param <V> The type of the values within the multimap.
 */
public class TrackedMultiMap<K, V> implements Multimap<K, V> {

    private final Multimap internalMap;
    private final DocumentProcessingFieldType trackedMapType;
    private final TrackedDocumentInternal internalTrackedDocument;

    
    // Make sure you can only create this object, with a wrapped implementation.
    // also we need to have a document object, to ensure we have a dataProcessingRecord to change.
    public TrackedMultiMap( DocumentInterface documentToWrap, DocumentProcessingFieldType documentProcessingFieldType ) {

        // Depending upon the DocumentFieldType pick the correct map now to be wrapped.
        Multimap mapToWrap = null;

        switch ( documentProcessingFieldType ) {
            case METADATA:
                mapToWrap = documentToWrap.getMetadata();
                break;
            case METADATA_REFERENCE:
                mapToWrap = documentToWrap.getMetadataReferences();
                break;
            case REFERENCE:
                throw new TrackedChangeTypeException(
                        "Unable to track changes via a TrackedMap to the document->reference field" );
            default:
                throw new TrackedChangeTypeException( 
                        "Invalid type to attempt to track with a TrackedMap documentProcessingFieldType: " + documentProcessingFieldType );
        }

        trackedMapType = documentProcessingFieldType;
        internalMap = mapToWrap;
        internalTrackedDocument = new TrackedDocumentInternal( documentToWrap );
    }

    // disallow anyone from creating this directly.
    private TrackedMultiMap() {
        throw new RuntimeException( new Exception( "Unable to create a blank TrackedMultimap - you must wrap an existing map." ));
    }

    @Override
    public int size() {
        return internalMap.size();
    }

    @Override
    public boolean isEmpty() {
        return internalMap.isEmpty();
    }

    @Override
    public boolean containsKey( Object key ) {
        return internalMap.containsKey( key );
    }

    @Override
    public boolean containsValue( Object value ) {
        return internalMap.containsValue( value );
    }

    @Override
    public boolean containsEntry( Object key, Object value ) {
        return internalMap.containsEntry( key, value );
    }

    @Override
    public boolean put( Object key, Object value ) {

        // anything which changes the state of the map, needs to actually perform tracking on the field change!
        return convertCollectionResponseToChangeIndicator( internalTrackedDocument.addFieldValue( String.valueOf( key ), value, trackedMapType ) );
    }

    @Override
    public boolean remove( Object key, Object value ) {
        // anything which changes the state of the map, needs to actually perform tracking on the field change!
        return convertCollectionResponseToChangeIndicator(internalTrackedDocument.deleteFieldValue( String.valueOf( key ), value, trackedMapType ) );
    }

    @Override
    public boolean putAll( Object key, Iterable values ) {
        // anything which changes the state of the map, needs to actually perform tracking on the field change!
        // call addFieldValue - but if it recognizes the object as an iterable it will correctly add all values.
        return convertCollectionResponseToChangeIndicator( internalTrackedDocument.addFieldValue( String.valueOf( key ), values, trackedMapType ) );
    }

    @Override
    public boolean putAll( Multimap newValuesMultimap ) {
        // anything which changes the state of the map, needs to actually perform tracking on the field change!
        return convertCollectionResponseToChangeIndicator ( internalTrackedDocument.addFieldValues( newValuesMultimap, trackedMapType ) );
    }

    @Override
    public Collection replaceValues( Object key, Iterable values ) {
        // anything which changes the state of the map, needs to actually perform tracking on the field change!
        return internalTrackedDocument.setFieldValues( String.valueOf( key ), values, trackedMapType );
    }

    @Override
    public Collection removeAll( Object key ) {
        // anything which changes the state of the map, needs to actually perform tracking on the field change!
       return internalTrackedDocument.deleteFieldValues( String.valueOf( key ), trackedMapType );
    }

    @Override
    public void clear() {
        // anything which changes the state of the map, needs to actually perform tracking on the field change!
        // we dont want anyone to delete all fields in this way.
        throw new UnsupportedOperationException("Not implemented, do not clear out all fields on a tracked document.");
    }

    @Override
    public Collection get( Object key ) {
        return internalMap.get( key );
    }

    @Override
    public Set keySet() {
        return internalMap.keySet();
    }

    @Override
    public Multiset keys() {
        return internalMap.keys();
    }

    @Override
    public Collection values() {
        return internalMap.values();
    }

    @Override
    public Collection entries() {
        return internalMap.entries();
    }

    @Override
    public Map asMap() {
        return internalMap.asMap();
    }

    private static boolean convertCollectionResponseToChangeIndicator( Collection<?> values )
    {
        return ( values != null && !values.isEmpty() );
    }
        
}
