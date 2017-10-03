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
package com.github.cafdataprocessing.worker.policy.handlers.classification;

import com.github.cafdataprocessing.corepolicy.common.CorePolicyApplicationContext;
import com.github.cafdataprocessing.worker.policy.WorkerRequestHolder;
import com.github.cafdataprocessing.worker.policy.WorkerResponseHolder;
import com.github.cafdataprocessing.worker.policy.shared.TaskData;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Contains reusable logic to set up an application context for ElasticSearchClassificationHandler.
 */
public class ApplicationContextSetup {
    public static CorePolicyApplicationContext Create() {
        CorePolicyApplicationContext applicationContext = new CorePolicyApplicationContext();

        createBeanDefinition(WorkerResponseHolder.class, "WorkerResponseHolder", "thread", applicationContext);
        createBeanDefinition(WorkerRequestHolder.class, "WorkerRequestHolder", "thread", applicationContext);

        applicationContext.refresh();
        applicationContext.getBean(WorkerRequestHolder.class).clear();
        applicationContext.getBean(WorkerResponseHolder.class).setTaskData(new TaskData());
        return applicationContext;
    }

    private static void createBeanDefinition(Class<?> beanClass, String beanClassName, String beanScope,
                                             CorePolicyApplicationContext applicationContext) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setScope(beanScope);
        beanDefinition.setBeanClass(beanClass);

        applicationContext.registerBeanDefinition(beanClassName, beanDefinition);
    }
}
