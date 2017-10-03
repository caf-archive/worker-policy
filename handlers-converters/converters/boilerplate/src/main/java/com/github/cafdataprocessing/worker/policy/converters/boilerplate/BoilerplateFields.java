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

/**
 * Provides the names of fields that are added by the boilerplate converter.
 */
public class BoilerplateFields {
    public final static String BOILERPLATE_MATCH_ID = "BOILERPLATE_MATCH_ID";
    private final static String BOILERPLATE_MATCH_VALUE_PREFIX = "BOILERPLATE_MATCH_";
    private final static String BOILERPLATE_MATCH_VALUE_SUFFIX = "_VALUE";

    public static String getMatchValueFieldName(String boilerplateId){
        //example output given fieldName=CONTENT, boilerplateId=1 -> BOILERPLATE_MATCH_1_VALUE
        return BOILERPLATE_MATCH_VALUE_PREFIX + boilerplateId + BOILERPLATE_MATCH_VALUE_SUFFIX;
    }
}
