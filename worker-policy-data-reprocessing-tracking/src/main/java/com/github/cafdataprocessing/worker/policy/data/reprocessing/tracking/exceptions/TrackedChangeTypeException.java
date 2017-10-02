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
package com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.exceptions;

/**
 * Specific Exception thrown during the change of field information on a trackeddocument object when the type of 
 * value being set is not of the correct type.  E.g. setting a reference field using a referenceData object.
 * 
 * @author trevor.getty@hpe.com
 */
public class TrackedChangeTypeException extends RuntimeException {
  
    public TrackedChangeTypeException(String message) {
        super(message);
    }

    public TrackedChangeTypeException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
