/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.github.cafdataprocessing.worker.policy.data.reprocessing.tracking.internal;

import com.github.cafdataprocessing.worker.policy.shared.Document;
import com.github.cafdataprocessing.worker.policy.shared.DocumentInterface;

/**
 *
 * @author trevor.getty@hpe.com
 */
public class TrackedDocumentInternal extends TrackedChange {

    // Instead of holding the document interface, we extend from the TrackedChange object as our base class.
    // The TrackedChange wraps the DocumentFieldChanger wrapper, which gives
    // additional utility methods to get/add/set/delete values from a document while maintaining all changes
    // as policy data processing records.  It also bridges to the internal namespace, 
    

    /**
     * Create a trackedDocument wrapper on a new blank document object.
     */
    public TrackedDocumentInternal() {
        // if you create a blank TrackedDocument, allow this to create its own internal implementation
        // using a shared.document instead of using an existing one.
        super(new Document());
    }

    /**
     * Create a trackedDocument wrapper on an existing document object.
     *
     * @param existingDocument The existing document ( interface ), which should be tracked.
     */
    public TrackedDocumentInternal( DocumentInterface existingDocument ) {
        super( existingDocument );
    }
}
