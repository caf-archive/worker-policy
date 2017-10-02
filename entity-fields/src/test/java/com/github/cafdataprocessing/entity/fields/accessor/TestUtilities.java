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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.cafdataprocessing.entity.fields.accessor;

import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base64;

/**
 * Utilty methods used in testing the project.
 */
class TestUtilities {

    public static String getBase64EncodedByteArrayFromString( String value ) {
        if ( value == null ) {
            return "";
        }
        return getBase64EncodedByteArray( org.apache.commons.codec.binary.Base64.encodeBase64( getByteArrayFromString( 
                value ) ) );
    }

    public static String getBase64EncodedByteArray( byte[] value ) {
        return new String( Base64.encodeBase64( value ), StandardCharsets.UTF_8 );
    }

    public static byte[] getByteArrayFromString( String value ) {
        
        if( value == null )
        {
            return null;
        }
        
        return value.getBytes( StandardCharsets.UTF_8 );
    }

}
