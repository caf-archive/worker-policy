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
package com.github.cafdataprocessing.worker.policy.testing.shared;

import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueue;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;
import com.hpe.caf.configs.RabbitConfiguration;

/**
 * Utility class for returning a RabbitWorkerQueue ready to be started
 */
public class TestQueueHelper {
    public static RabbitWorkerQueue getRabbitWorkerQueue(String inputQueueName, String deadLetterExchange) throws QueueException
    {
        RabbitWorkerQueueConfiguration QueueConfig = new RabbitWorkerQueueConfiguration();
        QueueConfig.setPrefetchBuffer(0);
        QueueConfig.setInputQueue(inputQueueName);
        RabbitConfiguration rabbitConfiguration = new RabbitConfiguration();
        rabbitConfiguration.setRabbitHost("localhost");
        rabbitConfiguration.setRabbitUser("guest");
        rabbitConfiguration.setRabbitPassword("guest");
        QueueConfig.setRabbitConfiguration(rabbitConfiguration);
        return new RabbitWorkerQueue(QueueConfig, 100);
    }

    public static RabbitWorkerQueue getRabbitWorkerQueue(String inputQueueName) throws QueueException
    {
        return getRabbitWorkerQueue(inputQueueName, "my-dlx");
    }

    public static RabbitWorkerQueue getRabbitWorkerQueue() throws QueueException
    {
        return getRabbitWorkerQueue("WorkerInput");
    }
}
