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
package com.github.cafdataprocessing.entity.fields;

import javax.annotation.Nullable;

/**
 * Allows a field value to represented in various different ways.
 * @author getty
 */
public class FieldValue {

    public String value;
    
    /**
     * optional field which specifies the encoding to use when reading the value field.
     */
    @Nullable
    public FieldEncoding valueEncoding;    
}
