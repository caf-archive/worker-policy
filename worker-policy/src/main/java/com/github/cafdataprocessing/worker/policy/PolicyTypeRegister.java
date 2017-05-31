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

import com.github.cafdataprocessing.corepolicy.common.AdminApi;
import com.github.cafdataprocessing.corepolicy.common.CorePolicyApplicationContext;
import com.github.cafdataprocessing.corepolicy.common.PolicyApi;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deals with registration/updating of Policy Types against the backend.
 */
public class PolicyTypeRegister {

    private static Logger logger = LoggerFactory.getLogger(PolicyTypeRegister.class);
    private CorePolicyApplicationContext corePolicyApplicationContext;
    private PolicyApi policyApi;
    private AdminApi adminApi;

    public PolicyTypeRegister(CorePolicyApplicationContext corePolicyApplicationContext, PolicyApi policyApi, AdminApi adminApi){
        this.corePolicyApplicationContext = corePolicyApplicationContext;
        this.policyApi = policyApi;
        this.adminApi = adminApi;
    }

    public void checkAndRegisterHandlerPolicyTypes(WorkerPolicyHandler workerPolicyHandler) {

        workerPolicyHandler.setApplicationContext(corePolicyApplicationContext);

        //Register policy
        PolicyType policyTypeToRegister = workerPolicyHandler.getPolicyType();
        PolicyType registeredPolicyType = null;

        logger.debug("Registering handler - " + policyTypeToRegister.getClass().toString());

        try {
            registeredPolicyType = policyApi.retrievePolicyTypeByName(policyTypeToRegister.shortName);
        } catch (Exception e) {
            logger.trace("Base PolicyType for name " + policyTypeToRegister.shortName + " not found.", e);
        }

        // policy type for this worker aren't found in the system, create a base data type now.
        if (registeredPolicyType == null) {
            try {
                // Just to the admin api, to create the required policy types.
                registeredPolicyType = adminApi.create(policyTypeToRegister);
            }
            catch (Exception e) {
                logger.trace("Creation of PolicyType for name " + policyTypeToRegister.shortName + " failed.", e);
                throw e;
            }
            logger.info("Created base policy type: " + registeredPolicyType.shortName + " ID: " + registeredPolicyType.id);
        }
        else if(hasPolicyChanged(registeredPolicyType,policyTypeToRegister)){
            try{
                registeredPolicyType.definition = policyTypeToRegister.definition;
                registeredPolicyType.description = policyTypeToRegister.description;
                registeredPolicyType.name = policyTypeToRegister.name;
                adminApi.update(registeredPolicyType);
            }
            catch (Exception e){
                logger.trace("Update of PolicyType for name "+ policyTypeToRegister.shortName + " failed.", e);
                throw e;
            } //Note changing the policy type can break policies that used the previous type
        }
    }

    private boolean hasPolicyChanged(PolicyType registeredPolicy, PolicyType policyToRegister) {
        if(policyToRegister.definition==null) {
            throw new IllegalArgumentException("Error when checking if Policy Type definition has changed. The policy type's definition cannot be null");
        }

        return !registeredPolicy.definition.toString().contentEquals(policyToRegister.definition.toString()) ||
                registeredPolicy.description != policyToRegister.description ||
                registeredPolicy.name != policyToRegister.name;
    }

}
