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
package com.github.cafdataprocessing.worker.policy.handlers.boilerplate;

/**
 * Class that defines how email segregation should be carried out in the workflow.
 */
public class EmailSegregationRules {
    /**
     * The field name to add to the document for primary content.
     */
    public String primaryFieldName;

    /**
     * The field name to add to the document for secondary content.
     */
    public String secondaryFieldName;

    /**
     * The field name to add to the document for tertiary content.
     */
    public String tertiaryFieldName;

    /**
     * The expression to define the primary content of the email.
     */
    public String primaryExpression;

    /**
     * The expression to define the secondary content of the email.
     */
    public String secondaryExpression;

    /**
     * The expression to define the tertiary content of the email.
     */
    public String tertiaryExpression;
}
