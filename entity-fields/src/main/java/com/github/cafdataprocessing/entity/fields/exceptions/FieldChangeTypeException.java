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
package com.github.cafdataprocessing.entity.fields.exceptions;

/**
 * Useful to find out if the fieldChangeRecord has something wrong during its creationg, usually due to 
 * the FieldChangeRecord-{@literal >}changeType being invalid.
 * 
 * @author trevor
 */

public class FieldChangeTypeException extends RuntimeException {
  
    public FieldChangeTypeException(String message) {
        super(message);
    }

    public FieldChangeTypeException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
