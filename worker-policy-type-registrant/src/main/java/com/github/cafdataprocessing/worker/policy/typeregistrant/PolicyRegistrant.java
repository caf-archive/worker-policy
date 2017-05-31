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
package com.github.cafdataprocessing.worker.policy.typeregistrant;

import com.github.cafdataprocessing.corepolicy.common.AdminApi;
import com.github.cafdataprocessing.corepolicy.common.CorePolicyApplicationContext;
import com.github.cafdataprocessing.corepolicy.common.PolicyApi;
import com.github.cafdataprocessing.corepolicy.common.UserContext;
import com.github.cafdataprocessing.worker.policy.PolicyHandlerSetup;
import com.github.cafdataprocessing.worker.policy.PolicyTypeRegister;
import com.github.cafdataprocessing.worker.policy.WorkerPolicyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * Entry point for application logic. Detects any handlers on the classpath and registers them in the policy database as appropriate.
 */
public class PolicyRegistrant {
    private static Logger LOGGER = LoggerFactory.getLogger(PolicyHandlerSetup.class);
    private CorePolicyApplicationContext corePolicyApplicationContext;
    private PolicyApi policyApi;
    private AdminApi adminApi;
    PolicyTypeRegister register;

    public static void main(String[] args){
        new PolicyRegistrant();
    }

    public PolicyRegistrant() {
        this.corePolicyApplicationContext = new CorePolicyApplicationContext();
        this.corePolicyApplicationContext.refresh();
        this.policyApi = corePolicyApplicationContext.getBean(PolicyApi.class);
        this.adminApi = corePolicyApplicationContext.getBean(AdminApi.class);
        this.register = new PolicyTypeRegister(this.corePolicyApplicationContext,this.policyApi,this.adminApi);

        UserContext userContext = corePolicyApplicationContext.getBean(UserContext.class);

        // Dont run under a particular user for this! Its about system base data.
        userContext.setProjectId(null);

        ServiceLoader<WorkerPolicyHandler> loader = ServiceLoader.load(WorkerPolicyHandler.class);
        LOGGER.info("About to register policy types from any detected policy handlers.");
        // This is per user registration of policy handlers per policy type.
        // not to be confused with the base data creation on startup.
        for (WorkerPolicyHandler handler : loader) {
            register.checkAndRegisterHandlerPolicyTypes(handler);
            LOGGER.info("Registered policy type: "+handler.getPolicyType().shortName);
        }
        LOGGER.info("Completed registration of any policy types from detected policy handlers.");
    }
}