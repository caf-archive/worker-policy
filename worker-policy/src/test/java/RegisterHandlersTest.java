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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cafdataprocessing.corepolicy.common.AdminApi;
import com.github.cafdataprocessing.corepolicy.common.CorePolicyApplicationContext;
import com.github.cafdataprocessing.corepolicy.common.PolicyApi;
import com.github.cafdataprocessing.corepolicy.common.dto.PolicyType;
import com.github.cafdataprocessing.worker.policy.PolicyTypeRegister;
import com.github.cafdataprocessing.worker.policy.WorkerTaskResponsePolicyHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

/**
 * Tests for RegisterHandlers class
 */
@RunWith(MockitoJUnitRunner.class)
public class RegisterHandlersTest {
    @Mock
    PolicyApi policyApi;
    @Mock
    CorePolicyApplicationContext applicationContext;
    @Mock
    AdminApi adminApi;
    PolicyTypeRegister register;
    @Mock
    WorkerTaskResponsePolicyHandler handler;
    PolicyType testType;
    PolicyType storedType;

    @Before
    public void setup() throws IOException {
        register = new PolicyTypeRegister(applicationContext,policyApi,adminApi);
        testType = new PolicyType();
        testType.shortName = "testPolicyType";
        testType.description = "Description 1";
        testType.name = "Test Policy";
        ObjectMapper m = new ObjectMapper();
        testType.definition = m.readTree("{\"description\": \"Testing Policy Type\", \"properties\":{\"value\":{\"type\":\"string\"}},\"title\":\"TestingPolicyType\",\"type\":\"object\"}");
        storedType = new PolicyType();
        storedType.shortName = "storedPolicyType";
        storedType.definition = m.readTree("{\"description\": \"A different Policy Type\", \"properties\":{\"value\":{\"type\":\"number\"}},\"title\":\"TestingPolicyType\",\"type\":\"object\"}");
        storedType.description = "Old Description";
        storedType.name = "Old Name";
        storedType.id = 1L;
    }

    @Test
    public void registerNewHandler(){
        Mockito.when(handler.getPolicyType()).thenReturn(testType);
        Mockito.when(adminApi.create(Matchers.any())).thenReturn(storedType);
        register.checkAndRegisterHandlerPolicyTypes(handler);
        Mockito.verify(adminApi).create(handler.getPolicyType());
    }

    @Test
    public void handlerAlreadyRegisteredAndUnchanged(){
        Mockito.when(handler.getPolicyType()).thenReturn(testType);
        Mockito.when(policyApi.retrievePolicyTypeByName(testType.shortName)).thenReturn(testType);
        Mockito.when(adminApi.update(testType)).thenThrow(new RuntimeException("The update method should not have been called as the Policy Types are identical"));
        register.checkAndRegisterHandlerPolicyTypes(handler);
    }

    @Test
    public void registeredHandlerChanged(){
        Mockito.when(handler.getPolicyType()).thenReturn(testType);
        //Return a different type to simulate a change in policy type
        Mockito.when(policyApi.retrievePolicyTypeByName(testType.shortName)).thenReturn(storedType);
        register.checkAndRegisterHandlerPolicyTypes(handler);
        //Update should be called with the updated policy type
        Mockito.verify(adminApi).update(storedType);
        Assert.assertEquals("The stored policy type should have been updated with the handler's policy type definition", testType.definition, storedType.definition);
        Assert.assertEquals("The stored policy description should have been updated with the handler", testType.description, storedType.description);
        Assert.assertEquals("The stored policy name should have been updated with the handler", testType.name, storedType.name);
    }

    @Test
    public void registerHandlerNameChanged(){
        //create a PolicyType that has same description and definition but a different name
        PolicyType newNameType = new PolicyType();
        newNameType.definition = storedType.definition;
        newNameType.shortName = storedType.shortName;
        newNameType.description = storedType.description;
        newNameType.name = "New name";

        Mockito.when(handler.getPolicyType()).thenReturn(newNameType);

        Mockito.when(policyApi.retrievePolicyTypeByName(newNameType.shortName)).thenReturn(storedType);
        register.checkAndRegisterHandlerPolicyTypes(handler);
        Mockito.verify(adminApi).update(storedType);
        Assert.assertEquals("The stored policy name should been updated with the new policy type name", newNameType.name, storedType.name);
        Assert.assertEquals("The stored policy definition should be the same as it was before.", newNameType.definition, storedType.definition);
        Assert.assertEquals("The stored policy description should be the same as it was before.", newNameType.description, storedType.description);
    }

    @Test
    public void registerHandlerDescriptionChanged(){
        //create a PolicyType that has same description and definition but a different name
        PolicyType newNameType = new PolicyType();
        newNameType.definition = storedType.definition;
        newNameType.shortName = storedType.shortName;
        newNameType.description = "New description";
        newNameType.name = storedType.name;

        Mockito.when(handler.getPolicyType()).thenReturn(newNameType);

        Mockito.when(policyApi.retrievePolicyTypeByName(newNameType.shortName)).thenReturn(storedType);
        register.checkAndRegisterHandlerPolicyTypes(handler);
        Mockito.verify(adminApi).update(storedType);
        Assert.assertEquals("The stored policy description should been updated with the new policy type description", newNameType.description, storedType.description);
        Assert.assertEquals("The stored policy definition should be the same as it was before.", newNameType.definition, storedType.definition);
        Assert.assertEquals("The stored policy name should be the same as it was before.", newNameType.name, storedType.name);
    }

    @Test
    public void registerHandlerDefinitionChanged(){
        //create a PolicyType that has same description and definition but a different name
        PolicyType newNameType = new PolicyType();
        newNameType.definition = testType.definition;
        newNameType.shortName = storedType.shortName;
        newNameType.description = storedType.description;
        newNameType.name = storedType.name;

        Mockito.when(handler.getPolicyType()).thenReturn(newNameType);

        Mockito.when(policyApi.retrievePolicyTypeByName(newNameType.shortName)).thenReturn(storedType);
        register.checkAndRegisterHandlerPolicyTypes(handler);
        Mockito.verify(adminApi).update(storedType);
        Assert.assertEquals("The stored policy definition should been updated with the new policy type name", newNameType.definition, storedType.definition);
        Assert.assertEquals("The stored policy name should be the same as it was before.", newNameType.name, storedType.name);
        Assert.assertEquals("The stored policy description should be the same as it was before.", newNameType.description, storedType.description);
    }

}
