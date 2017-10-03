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
package com.github.cafdataprocessing.entity.fields.factory;

import com.github.cafdataprocessing.entity.fields.FieldChangeRecord;
import com.github.cafdataprocessing.entity.fields.FieldChangeType;
import com.github.cafdataprocessing.entity.fields.exceptions.FieldChangeTypeException;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author trevor
 */
public final class FieldChangeRecordFactory {
    
     // Factory method to create a fieldValue from a string, defaults to utf8 encoding.
    public static final FieldChangeRecord create(FieldChangeType fieldChangeType, String originalValue) throws FieldChangeTypeException {
        
        FieldChangeRecord fieldChangeRecord = new FieldChangeRecord();
        fieldChangeRecord.changeType = fieldChangeType;
        
        switch ( fieldChangeType ) {
            case added:
                // we dont need any original state - just the type
                return fieldChangeRecord;

            case updated:
            case deleted:
                // in the case of an update or deleted field change type, 
                // need to record the original state of the document, we only do this for the first record, after this
                // the (originalValues aren't really that, they are only the values currently on the document, and as
                // such we dont create an record from this info, only update an exising record to a different state potentially.
                fieldChangeRecord.changeValues = new ArrayList<>();
                fieldChangeRecord.changeValues.add( FieldValueFactory.create( originalValue ) );
                return fieldChangeRecord;

            default:
                throw new FieldChangeTypeException( "Unknown fieldChangeType: " + fieldChangeType );

        }
    }
    
    // Factory method to create a fieldValue from a string, defaults to utf8 encoding.
    public static final FieldChangeRecord create(FieldChangeType fieldChangeType, Collection<?> originalValues ) throws FieldChangeTypeException {
        
        FieldChangeRecord fieldChangeRecord = new FieldChangeRecord();
        fieldChangeRecord.changeType = fieldChangeType;
  
        switch ( fieldChangeType ) {
            case added:
                // we dont need any original state - just the type, but I am instead stripping them off before adding a 
                // single record to the policyDataProcessingRecord collection - so that I can use the originalValues 
                // internally.
                fieldChangeRecord.changeValues = new ArrayList<>();
                fieldChangeRecord.changeValues.addAll( FieldValueFactory.create( originalValues ) );
                return fieldChangeRecord;

            case updated:
            case deleted:
                // in the case of an update or deleted field change type, 
                // need to record the original state of the document, we only do this for the first record, after this
                // the (originalValues aren't really that, they are only the values currently on the document, and as
                // such we dont create an record from this info, only update an exising record to a different state potentially.
                fieldChangeRecord.changeValues = new ArrayList<>();
                fieldChangeRecord.changeValues.addAll( FieldValueFactory.create( originalValues ) );
                return fieldChangeRecord;

            default:
                throw new FieldChangeTypeException( "Unknown fieldChangeType: " + fieldChangeType );

        }

    }
}
