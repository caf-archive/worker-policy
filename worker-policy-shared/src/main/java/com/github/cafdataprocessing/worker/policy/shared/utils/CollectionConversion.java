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

import com.google.common.base.Strings;
import com.github.cafdataprocessing.entity.fields.FieldValue;
import com.github.cafdataprocessing.entity.fields.accessor.FieldValueAccessor;
import com.github.cafdataprocessing.entity.fields.factory.FieldValueFactory;
import com.hpe.caf.util.ref.ReferencedData;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author trevor
 */
public final class CollectionConversion {
    
    // do not create an instance of the object, its for static methods only
    private CollectionConversion(){
        
    }
    
    /** 
     * Utility method to get the first value to set from a list, or null, if values are empty.
     */
    public final static String getFirstValue ( Collection<String> values )
    {
       if ( values == null || values.isEmpty() ) 
       {
           return null;
       }
       
       return values.stream().findFirst().get();
    }
    
    public final static boolean isObjectPlural( Object specifiedValue ){
     
        if ( specifiedValue == null )
        {
            return false;
        }
        
        // is the object a plural object type like collection / iterable where we should
        // be setting values instead of value, on the map.
        if ( specifiedValue instanceof Collection )
        {
            return true;
        }
        
        if ( specifiedValue instanceof Iterable )
        {
            return true;
        }
        
        // if you are adding any new plural types here, you must also add it to the createCollectionFromPluralObject
        // below, to ensure it returns the collection once we indicate true, that it is plural.
        return false;
    }
        
    public final static Collection<?> createCollectionFromPluralObject( Object specifiedValue ){
        
        if ( specifiedValue ==  null )
        {
            return null;
        }
        
        if ( specifiedValue instanceof Collection )
        {
            return (Collection) specifiedValue;
        }
        
        Collection<Object> newValues = new ArrayList<>();
     
        if ( specifiedValue instanceof Iterable ) {
            Iterable<?> collectionRepresenation = ( ( Iterable ) specifiedValue );
            for ( Object item : collectionRepresenation ) {
                newValues.add( item );
            }
            return newValues;
        }
   
        throw new InvalidParameterException("Object requested as a collection type is not supported.");
    }
    
    
    public final static String createAsString( Object value ) {
        if ( value == null ) {
            return null;
        }

        if ( value instanceof String ) {
            return ( String ) value;
        }

        if ( value instanceof FieldValue ) {
            return FieldValueAccessor.createStringFromFieldValue( ( FieldValue ) value );
        }

        if ( value instanceof ReferencedData ) {
      
            if ( !Strings.isNullOrEmpty(((ReferencedData) value).getReference()))
            {
                // we have a storage reference, I am just returning it as the string.
                // this isn't a decode method, to retrieve the actual file content from the reference.
                return ((ReferencedData) value).getReference();
            }
            
            // create a string from the bytes, and return this to the caller.
            return new String( ((ReferencedData )value).getData(), StandardCharsets.UTF_8 );    
        }
    
        // any other object, try to turn into a user friendly string if possible.
        return String.valueOf( value );
    }

    public final static Collection<String> createAsStringCollection( Object value ) {
        // the object maybe a collection or iterable, if so turn into fixed collection type now
        if ( value == null ) {
            return null;
        }

        Collection<String> newValues = new ArrayList<>();

        if ( value instanceof Collection ) {
            // like instanceof Collection<String> or something, to prevent this copy!
            Collection<?> collectionRepresenation = ( ( Collection ) value );
            for ( Object item : collectionRepresenation ) {
                newValues.add( createAsString( item ) );
            }
            return newValues;
        }

        if ( value instanceof Iterable ) {
            Iterable<?> collectionRepresenation = ( ( Iterable ) value );
            for ( Object item : collectionRepresenation ) {
                newValues.add( createAsString( item ) );
            }
            return newValues;
        }

        // try to decode using single object
        newValues.add( createAsString( value ) );
        return newValues;
    }

    public final static Collection<ReferencedData> createAsReferencedDataCollection( Object value ) {
        // the object maybe a collection or iterable, if so turn into fixed collection type now
        if ( value == null ) {
            return null;
        }

        Collection<ReferencedData> newValues = new ArrayList<>();

        if ( value instanceof Collection ) {
            Collection<?> collectionRepresenation = ( ( Collection ) value );
            for ( Object item : collectionRepresenation ) {
                newValues.add( createAsReferencedDataObject( item ) );
            }
            return newValues;
        }

        if ( value instanceof Iterable ) {
            Iterable<?> collectionRepresenation = ( ( Iterable ) value );
            for ( Object item : collectionRepresenation ) {
                newValues.add( createAsReferencedDataObject( item ) );
            }
            return newValues;
        }

        // try to decode using single object
        newValues.add( createAsReferencedDataObject( value ) );
        return newValues;
    }

    public final static ReferencedData createAsReferencedDataObject( Object value ) {
        
        if ( value == null )
        {
            return null;
        }
        
        if ( value instanceof ReferencedData ) {
            return ( ReferencedData ) value;
        }

        if ( value instanceof FieldValue ) {
            return FieldValueAccessor.createReferencedDataFromFieldValue( ( FieldValue ) value );
        }

        // anything else, get string equivilent, and get referencedData from the bytes.
        if ( value instanceof String ) {
            // keep decoding in one place for string ->referenceData.
            return FieldValueAccessor.createReferencedDataFromFieldValue( FieldValueFactory.create( ( String ) value ) );

        }

        // any other object, try to turn into a user friendly string if possible.
        return ReferencedData.getWrappedData( String.valueOf( value ).getBytes() );
    }
  
}
