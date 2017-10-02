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
package com.github.cafdataprocessing.worker.policy.shared.utils;

import com.github.cafdataprocessing.entity.fields.FieldValue;
import com.github.cafdataprocessing.entity.fields.accessor.FieldValueAccessor;
import com.github.cafdataprocessing.entity.fields.factory.FieldValueFactory;
import com.hpe.caf.util.ref.ReferencedData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author getty
 */
public class CollectionConversionTest {


    @Test
    public void testNullCollectionCreationReturnsNull_StringCollection(){
       
        Collection<?> result = CollectionConversion.createAsStringCollection( null );
        
        Assert.assertNull ( result );
    }
    
    @Test
    public void testNullCollectionCreationReturnsNull_PluralObject(){
        
        Collection<?> result = CollectionConversion.createCollectionFromPluralObject( null );
        
        Assert.assertNull ( result );
    }
    
    @Test
    public void testNullCollectionCreationReturnsNull_ReferencedDataCollection(){
        
        Collection<?> result = CollectionConversion.createAsReferencedDataCollection(null );
        
        Assert.assertNull ( result );
    }
    
    @Test
    public void testNullObjectCreationReturnsNull_ReferencedDataObject(){
        
        ReferencedData result = CollectionConversion.createAsReferencedDataObject(null );
        
        Assert.assertNull ( result );
    }
    
    @Test
    public void testNullObjectCreationReturnsNull_StringObject(){
        
        String result = CollectionConversion.createAsString(null );
        
        Assert.assertNull ( result );
    }
    
    @Test
    public void testConversionOfStringToReferencedDataCollection(){
        
        Collection<String> initialValues = Arrays.asList( "abc", "def");
        
        Collection<ReferencedData> refDataCollection = CollectionConversion.createAsReferencedDataCollection( initialValues );
        
        checkAllValuesArePresent(initialValues , refDataCollection);
    }

    
    @Test
    public void testConversionOfReferencedDataCollectionToStringCollection(){
        
        Collection<String> initialValues = Arrays.asList( "abc", "def");
        
        Collection<ReferencedData> refDataCollection = CollectionConversion.createAsReferencedDataCollection( initialValues );
        
        // now from the refDataCollection, get back to the string collection.
        Collection<String> convertedValues = CollectionConversion.createAsStringCollection( refDataCollection );
        
        checkAllValuesArePresentFromStrings( initialValues, convertedValues );
    }
     
    @Test
    public void testConversionOfCollectionObjectsToStringCollection(){
        
        final Long value1 = 1L;
        final Long value2 = 2L;
        final Long value3 = 5L;
        Collection<Long> initialValues = Arrays.asList( value1, value2, value3 );
        
        // now from the refDataCollection, get back to the string collection.
        Collection<String> convertedValues = CollectionConversion.createAsStringCollection( initialValues );
        
        Assert.assertEquals( "Expect x string values from collection of longs.", 3, convertedValues.size());
    }
    
    
    
    private void checkAllValuesArePresent( Collection<String> initialValues,
                                           Collection<ReferencedData> refDataCollection ) {
        Collection<String> removeItems = copyCollection( initialValues );
        
        for ( ReferencedData data : refDataCollection )
        {
            // make sure the data from this is in our intial values, leaving nothing over
            // I am jumping through other classes, to make the comparison, but this keeps logic out of this testing class...
            FieldValue encodedValue = FieldValueFactory.create( data );
            String stringRepresentationOfData = FieldValueAccessor.createStringFromFieldValue( encodedValue );
            
            if ( removeItems.remove( stringRepresentationOfData ) )
            {
                // we removed it, so it was present, go onto next item.
                continue;
            }
            
            // otherwise it wasn't there - throw.
            Assert.assertTrue( "String value of refernced data, didn't match any value given to us originally : " + stringRepresentationOfData, removeItems.contains( stringRepresentationOfData ));
        }
        
        Assert.assertTrue( "All items supplied must have been found: ", ( removeItems == null || removeItems.isEmpty() ) );
    }

     private void checkAllValuesArePresentFromStrings( Collection<String> initialValues,
                                           Collection<String> compareCollection ) {
        Collection<String> removeItems = copyCollection( initialValues );
        
        for ( String data : compareCollection )
        {
            if ( removeItems.remove( data ) )
            {
                // we removed it, so it was present, go onto next item.
                continue;
            }
            
            // otherwise it wasn't there - throw.
            Assert.assertTrue( "String value of refernced data, didn't match any value given to us originally : " + data, removeItems.contains( data ));
        }
        
        Assert.assertTrue( "All items supplied must have been found: ", ( removeItems == null || removeItems.isEmpty() ) );
    }
     
    private Collection<String> copyCollection( Collection<String> initialValues ) {
        // dont change source collection, so copy items first!
        Collection<String> removeItems = new ArrayList<>();
        removeItems.addAll( initialValues );
        return removeItems;
    }
}
