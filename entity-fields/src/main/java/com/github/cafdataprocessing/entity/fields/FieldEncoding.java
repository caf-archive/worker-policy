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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Encodings used with our FieldValue object, this informs the consumer how the value field is encoded.
 * @author getty
 */
public enum FieldEncoding {

    // As these values are actually used for the serialization of the information, please use lowercase, as this is how it
    // appears when transmitted.  Even though java coding recommends uppercase, we want lower in transmitted properties.
    utf8,
    base64,
    caf_storage_reference;
};