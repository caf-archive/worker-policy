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
package com.github.cafdataprocessing.entity.fields.printer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.github.cafdataprocessing.entity.fields.FieldChangeRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets the fieldChangeRecord in a more userfriendly / readible format, for logging / printing.
 * @author getty
 */
public class FieldChangeRecordPrinter {

        private static ObjectMapper objectMapper = null;
        private static final Logger logger = LoggerFactory.getLogger(FieldChangeRecordPrinter.class);
        
        public static void checkObjectInitialized(){
            if ( objectMapper != null )
            {
                return;
            }
            
            objectMapper =  new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
            objectMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING,true);
            objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.registerModule(new JodaModule());
            objectMapper.registerModule(new GuavaModule());
        }
        
        /**
         * simple toString on an existing fieldChangeRecord, for text only logging
         * @param fieldChange
         * @return 
         */
       public static String toString( FieldChangeRecord fieldChange ){
           
           return String.format( "FieldChangeRecord changeType: {%s} changeValues: {%s}",
                                     fieldChange.changeType, fieldChange.changeValues.toString() );
       }
       
       /**
        * Utility method, to output the object, it the format used for serialization.
        * @param fieldChange
        * @return 
        */
       public static String toFormattedString( FieldChangeRecord fieldChange ){
              
           // check we have setup our static objectMapper for correct use, and no failing 
           // on unknown fields, as this is only for debug.  Also dont throw out from these methods!
           checkObjectInitialized();
           
           JsonNode node = objectMapper.valueToTree( fieldChange );
            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString( node );
            }
            catch ( JsonProcessingException ex ) {
                logger.warn("json processing exception: ", ex);
                return "Unable to use a formatted string";
            }
       }
}
