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
package com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking;

import java.util.ArrayList;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the TrackedChange class
 * @author trevor.getty@hpe.com
 */
public class TrackedChangeTest {
    
    // Some testing to confirm behaviour of the collection / map objects so our tests are what is expected!
    
    @Test
    public void validateCollectionComparison(){
        // in order that we know how the map behaves, run simple tests here, to confirm our behaviour!
        Collection<Object> values = new ArrayList<>();
        final String value1 = "abc";
        values.add( value1 );
        
        Collection<Object> otherValues = new ArrayList<>();
        
        Assert.assertNotEquals( "values collections cant match at present", values, otherValues );
        Assert.assertTrue( "values collections cant match at present", !values.equals( otherValues ) );
        
        // now add the other value and compare again
        otherValues.add( value1 );
        
        Assert.assertEquals( "values collections cant match at present", values, otherValues );
        Assert.assertTrue( "values collections cant match at present", values.equals( otherValues ) );
    }
    
    @Test
    public void validateRemoveValueFromCollectionDeletesSingleValue(){
        // in order that we know how the map behaves, run simple tests here, to confirm our behaviour!
        Collection<Object> values = new ArrayList<>();
        final String value1 = "abc";
        final String value2 = "def";
        
        values.add( value1 );
        values.add( value2 );
        values.add( value1 );
        
        
        Assert.assertEquals( "values collections cant match at present", 3, values.size() );
        
        // now remove value 1, and check it drops to 2 values, even though there are 2 with that value?
        values.remove(  value1 );
        
        Assert.assertEquals( "values collections cant match at present", 2, values.size() );
        Assert.assertTrue( "Values must contain value1", values.contains( value1 ));
        Assert.assertTrue( "Values must contain value2", values.contains( value2 ));
    }
    
    
}
