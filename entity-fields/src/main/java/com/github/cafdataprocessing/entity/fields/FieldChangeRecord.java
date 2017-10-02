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

import java.util.Collection;
import javax.validation.constraints.NotNull;

/**
 * Contract definition which makes up a FieldChangeRecord.  This is a record of a field, and how to set it back to its
 * original value.  
 * For example changeType:  
 *  Added, means just delete it.  
 *  Updated, means delete fields of that name, and add these values.
 *  Deleted, means just add these values.
 * 
 * @author getty
 */
public class FieldChangeRecord {
    
    @NotNull
    public FieldChangeType changeType;
    
    public Collection<FieldValue> changeValues;
    
}
