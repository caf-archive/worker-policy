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
package com.github.cafdataprocessing.worker.policy;

import com.github.cafdataprocessing.corepolicy.api.ClassifyDocumentApiDirectImpl;
import com.github.cafdataprocessing.corepolicy.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * Logic for setting up handlers for use by worker.
 */
public class PolicyHandlerSetup {

    private static Logger logger = LoggerFactory.getLogger(PolicyHandlerSetup.class);
    private CorePolicyApplicationContext corePolicyApplicationContext;
    private ClassifyDocumentApi classifyDocumentApi;
    private PolicyApi policyApi;
    private AdminApi adminApi;
    PolicyTypeRegister register;

    PolicyHandlerSetup(CorePolicyApplicationContext applicationContext) {
        this.corePolicyApplicationContext = applicationContext;
        this.classifyDocumentApi = corePolicyApplicationContext.getBean(ClassifyDocumentApi.class);
        this.policyApi = corePolicyApplicationContext.getBean(PolicyApi.class);
        this.adminApi = corePolicyApplicationContext.getBean(AdminApi.class);
        this.register = new PolicyTypeRegister(this.corePolicyApplicationContext,this.policyApi,this.adminApi);
    }

    public void checkBasePolicies() {
        if (classifyDocumentApi instanceof ClassifyDocumentApiDirectImpl) {
            logger.debug("Registering Handlers");

            UserContext userContext = corePolicyApplicationContext.getBean(UserContext.class);

            // Dont run under a particular user for this! Its about system base data.
            userContext.setProjectId(null);

            ServiceLoader<WorkerPolicyHandler> loader = ServiceLoader.load(WorkerPolicyHandler.class);

            // This is per user registration of policy handlers per policy type.
            // not to be confused with the base data creation on startup.
            for (WorkerPolicyHandler handler : loader) {
                register.checkAndRegisterHandlerPolicyTypes(handler);
            }
        }
    }

}

