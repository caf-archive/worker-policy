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
package com.github.cafdataprocessing.worker.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cafdataprocessing.corepolicy.common.Document;
import com.github.cafdataprocessing.corepolicy.common.dto.Policy;
import com.github.cafdataprocessing.corepolicy.domainModels.FieldAction;
import com.github.cafdataprocessing.corepolicy.policy.TagPolicy.TagPolicy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Abstract class, used to add document tag ability to a policy handler.
 */
public abstract class TagPolicyHandlerBase extends WorkerPolicyHandler {

    @JsonProperty("fieldActions")
    private Collection<FieldAction> fieldActions = new ArrayList();

    public Collection<FieldAction> getFieldActions() {
        return fieldActions;
    }

    public void setFieldActions(Collection<FieldAction> fieldActions) {
        this.fieldActions = fieldActions;
    }

    protected TagPolicy getTagPolicy(Policy policy) {
        TagPolicy returnPolicy;
        try {
            ObjectMapper mapper = new ObjectMapper();
            returnPolicy = mapper.treeToValue(policy.details, TagPolicy.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return returnPolicy;
    }

    // Utility method to apply field actions to a document metadata.
    protected void applyFieldActions(Document document, Collection<FieldAction> fieldActions) {
        for (FieldAction fieldAction : fieldActions) {

            FieldAction.Action action = fieldAction.getAction();

            // N.B. Weird javac behaviour whereby if a switch is used below 2 ProcessDocument class files are
            // generated, once called ProcessDocument$1.class and once ProcessDocument.class which stops us from
            // correctly running javah on this class.
            if ( action == FieldAction.Action.SET_FIELD_VALUE ){
                document.getMetadata().get(fieldAction.getFieldName()).clear();
                document.getMetadata().put(fieldAction.getFieldName(), fieldAction.getFieldValue());
            }
            else if ( action == FieldAction.Action.ADD_FIELD_VALUE ) {
                document.getMetadata().put(fieldAction.getFieldName(), fieldAction.getFieldValue());
            }
        }
    }
}
