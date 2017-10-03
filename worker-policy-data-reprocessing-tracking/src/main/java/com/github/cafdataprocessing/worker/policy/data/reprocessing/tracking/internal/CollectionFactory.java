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
package com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Utility class, to create Collection object, using different source objects.
 * 
 * @author trevor.getty@hpe.com
 */
class CollectionFactory {


    /**
     * Utility class, to allow the user to change an iterable object into a collection.
     * @param <K>
     * @param newValues
     * @return 
     */
    public static final <K> Collection<K> toCollection( Iterable newValues ) {
        Collection<K> newValuesCollection = new ArrayList<>();
        Iterator iter = newValues.iterator();
        // create a collection using the iterable, and use it instead.
        while ( iter.hasNext() )
        {
            newValuesCollection.add( (K)iter.next() );
        }
        return newValuesCollection;
    }
    
     /**
     * Utility class, to allow the user to change an iterable object into a collection.
     * @param <K>
     * @param newValues
     * @return 
     */
    public static final <K> Collection<K> toCollection( Collection<?> newValues ) {
        Collection<K> newValuesCollection = new ArrayList<>();
        
        // create a collection using each item, cast to the requested type.
        for ( Object item : newValues )
        {
            newValuesCollection.add( (K)item );
        }
        
        return newValuesCollection;
    }
    
    /**
     * Utility class, to allow the user to change an iterable object into a collection.
     * @param <K>
     * @param newValues
     * @return 
     */
    public static final <K> Collection<K> toCollection( Object newValue ) {
        
        // Create a collection of the items, depending upon the incoming type.
        if( newValue instanceof Iterable )
        {
            return toCollection( (Iterable) newValue );
        }
        
        if ( newValue instanceof Collection )
        {
            return toCollection( (Collection) newValue );
        }
        
        Collection<K> newValuesCollection = new ArrayList<>();
        
        // create a collection and use the object as the only item in the collection?
        Collection<K> values = new ArrayList<>();
        values.add( (K)newValue );
        
        return values;
    }
        
}
