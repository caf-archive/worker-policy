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

import com.github.cafdataprocessing.corepolicy.common.CorePolicyApplicationContext;
import com.github.cafdataprocessing.corepolicy.common.EngineProperties;
import com.github.cafdataprocessing.worker.policy.shared.PolicyWorkerConstants;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.DataStore;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Testing that configuration read in is honoured.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({PolicyWorkerFactory.class})
public class ConfigurationTest {

    /**
     * Testing that when the registerHandler property of config is set to false that the PolicyHandlerSetup
     * is not invoked.
     * @throws Exception
     */
    @Test
    public void registerHandlersFalse() throws Exception {
        PolicyWorkerConfiguration config = prepareConfig(false);
        DataStore store = Mockito.mock(DataStore.class);
        Codec codec = Mockito.mock(Codec.class);
        CorePolicyApplicationContext mockContext = Mockito.mock(CorePolicyApplicationContext.class);
        PowerMockito.whenNew(CorePolicyApplicationContext.class).withAnyArguments().thenReturn(mockContext);

        EngineProperties engineProperties = Mockito.mock(EngineProperties.class);
        Mockito.when(engineProperties.getEnvironmentCacheVerifyPeriod()).thenReturn("PT5M");
        Mockito.when(mockContext.getBean(EngineProperties.class)).thenReturn(engineProperties);

        PolicyHandlerSetup mockHandlerSetup = Mockito.mock(PolicyHandlerSetup.class);
        PowerMockito.whenNew(PolicyHandlerSetup.class).withAnyArguments().thenReturn(mockHandlerSetup);
        Mockito.doAnswer(invocation -> {
            Assert.fail("PolicyHandlerSetup should not have been invoked when registerHandlers is false.");
            return null;
        }).when(mockHandlerSetup).checkBasePolicies();

        new PolicyWorkerFactory(codec, store, config);
    }

    /**
     * Testing that when the registerHandler property of config is set to true that the PolicyHandlerSetup
     * is invoked.
     * @throws Exception
     */
    @Test
    public void registerHandlersTrue() throws Exception {
        PolicyWorkerConfiguration config = prepareConfig(true);
        DataStore store = Mockito.mock(DataStore.class);
        Codec codec = Mockito.mock(Codec.class);
        CorePolicyApplicationContext mockContext = Mockito.mock(CorePolicyApplicationContext.class);
        PowerMockito.whenNew(CorePolicyApplicationContext.class).withAnyArguments().thenReturn(mockContext);

        EngineProperties engineProperties = Mockito.mock(EngineProperties.class);
        Mockito.when(engineProperties.getEnvironmentCacheVerifyPeriod()).thenReturn("PT5M");
        Mockito.when(mockContext.getBean(EngineProperties.class)).thenReturn(engineProperties);

        PolicyHandlerSetup mockHandlerSetup = Mockito.mock(PolicyHandlerSetup.class);
        PowerMockito.whenNew(PolicyHandlerSetup.class).withAnyArguments().thenReturn(mockHandlerSetup);
        new PolicyWorkerFactory(codec, store, config);
        Mockito.verify(mockHandlerSetup, Mockito.times(1)).checkBasePolicies();
    }

    private PolicyWorkerConfiguration prepareConfig(boolean registerHandlers){
        PolicyWorkerConfiguration config = new PolicyWorkerConfiguration();
        config.setWorkerIdentifier(PolicyWorkerConstants.WORKER_NAME);
        config.setWorkerThreads(1);
        config.setResultQueue("test");
        config.setRegisterHandlers(registerHandlers);
        return config;
    }
}
