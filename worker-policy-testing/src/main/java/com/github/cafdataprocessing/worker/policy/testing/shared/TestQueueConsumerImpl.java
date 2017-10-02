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

import com.google.common.base.Strings;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.util.rabbitmq.*;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to receive messages from a specified queue and make them available for inspection
 */
public class TestQueueConsumerImpl implements QueueConsumer
{

    private final static Logger logger = LoggerFactory.getLogger(TestQueueConsumerImpl.class);

    private final Map<String, Delivery> delivery = new HashMap<>();
    private final CountDownLatch latch;
    private final BlockingQueue<Event<QueueConsumer>> eventQueue;
    private final Channel channel;
    private final String taskIdFilter; // allows filtering of the messages delivered, so that items you aren't interested in aren't put into our delivered items list.

    public TestQueueConsumerImpl(final CountDownLatch latch, final BlockingQueue<Event<QueueConsumer>> queue, final Channel ch, final String taskIdFilter)
    {
        this.latch = Objects.requireNonNull(latch);
        this.eventQueue = Objects.requireNonNull(queue);
        this.channel = Objects.requireNonNull(ch);
        this.taskIdFilter = taskIdFilter;
    }

    public TestQueueConsumerImpl(final CountDownLatch latch, final BlockingQueue<Event<QueueConsumer>> queue, final Channel ch)
    {
        this.latch = Objects.requireNonNull(latch);
        this.eventQueue = Objects.requireNonNull(queue);
        this.channel = Objects.requireNonNull(ch);
        this.taskIdFilter = null;
    }

    @Override
    public void processDelivery(final Delivery delivery)
    {
        TaskMessage resultMessage = getTaskMessageFromDelivery(delivery);

        if (Strings.isNullOrEmpty(resultMessage.getTaskId())) {
            // no valid task id on this message -> why and how do we identify this one?
            throw new RuntimeException("Invalid delivery of message without a taskID");
        }

        if (!Strings.isNullOrEmpty(taskIdFilter)) {
            // check if the filter matches this taskId.
            if (!resultMessage.getTaskId().contains(taskIdFilter)) {
                // if it doesn't match, finally check does the jobtracking info match it
                if (resultMessage.getTracking() == null || !resultMessage.getTracking().getJobTaskId().contains(taskIdFilter)) {
                    // we have just received a message delivery but its not a valid item matching the taskId filter.
                    // as such throw it away its either a child message without jobtracking that we didn't want, or 
                    // an invalid message from a previous test case that was delivered after a previous test timed out and gave up.
                    logger.warn("Received message which wasn't expected by this test case -> throwing it away taskId: " + resultMessage.getTaskId() + " jobTracking: " + (resultMessage.getTracking() == null ? "null" : resultMessage.getTracking().getJobTaskId()));
                    
                    // I am using the consumer reject event here, to actually acknowledge the message in the queue, and take ownership, 
                    // so we can throw it away.
                    // we do NOT reduce the latch count here, so it keeps waiting for more messages coming in.
                    eventQueue.add(new ConsumerRejectEvent(delivery.getEnvelope().getDeliveryTag()));
                    return;
                }
            }
        }

        logger.info("Recieved message: " + resultMessage.getTaskId() + 
                    " jobTracking: " + (resultMessage.getTracking() == null ? "null" : resultMessage.getTracking().getJobTaskId()));
        
        this.delivery.put(resultMessage.getTaskId(), delivery);
        eventQueue.add(new ConsumerAckEvent(delivery.getEnvelope().getDeliveryTag()));
    }

    @Override
    public void processAck(long tag)
    {
        try {
            channel.basicAck(tag, false);
            latch.countDown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processReject(long tag)
    {
        try {
            channel.basicReject(tag, false);
            // Do NOT count down for messages we are going to reject.
            // latch.countDown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processDrop(long tag)
    { 
        try {
            logger.info("processDrop: Ack and drop message: " + tag);
            channel.basicAck(tag, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static TaskMessage getTaskMessageFromDelivery(Delivery deliveredResult)
    {
        //check that response received
        Assert.assertNotNull(deliveredResult);
        final Codec codec = new JsonCodec();
        TaskMessage resultWrapper;
        try {
            resultWrapper = codec.deserialise(deliveredResult.getMessageData(), TaskMessage.class);
        } catch (CodecException ex) {
            throw new RuntimeException("Failed to turn message delivery into a TaskMessage: ", ex);
        }
        return resultWrapper;
    }

    public Map<String, Delivery> getDelivery()
    {
        return delivery;
    }

    public Delivery getDelivery(TaskMessage originatingTaskMessage)
    {
        final String expectedTaskId = originatingTaskMessage.getTaskId();

        return getDelivery(expectedTaskId);
    }

    public Delivery getDelivery(final String expectedTaskId)
    {
        return findDeliveredMessage(expectedTaskId);
    }

    /**
     * find an already delivered message, via the taskId information whether that be a direct match to the taskId on the message, or and
     * indirect match on the child tracking information.
     *
     * @param expectedTaskId
     * @return
     */
    private Delivery findDeliveredMessage(final String expectedTaskId)
    {
        if (delivery.isEmpty()) {
            // no items in the queue, just return null for now, or wait in this layer?
            return null;
        }

        // try to find a response to our originating task message -> it should have the same taskId.
        logger.debug("trying to find task: " + expectedTaskId);

        Delivery del = delivery.get(expectedTaskId);

        if (del != null) {
            delivery.remove(expectedTaskId);
            return del;
        }

        // failed to find it on the queue yet.
        logger.debug("Failed to find a direct taskId match on the queue.");

        // do we have any items on the queue at all, if so, output what we have?
        for (Map.Entry<String, Delivery> deliveredItem : delivery.entrySet()) {
            del = deliveredItem.getValue();
            TaskMessage tm = getTaskMessageFromDelivery(del);

            logger.debug(
                String.format("Queue contains other items: {%s} TrackingInfo jobTaskId: {%s}",
                              tm.getTaskId(), tm.getTracking() == null ? "null" : tm.getTracking().getJobTaskId()));

            // check does its tracking info contain such as match.
            if (tm.getTracking() == null) {
                continue;
            }

            if (!tm.getTracking().getJobTaskId().contains(expectedTaskId)) {
                continue;
            }

            // found the item we are after in the tracking information -> return it instead.
            logger.debug("Queue contains match in tracking info: " + tm.getTracking().getJobTaskId() + " with actual taskId:" + tm.getTaskId());
            delivery.remove(tm.getTaskId());
            return del;
        }

        return null;
    }
}
