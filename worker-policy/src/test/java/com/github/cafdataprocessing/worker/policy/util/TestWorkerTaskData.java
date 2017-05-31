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
package com.github.cafdataprocessing.worker.policy.util;

import com.hpe.caf.api.worker.TaskSourceInfo;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.api.worker.WorkerTaskData;

/**
 * Implementation of WorkerTaskData for test purposes
 */
public class TestWorkerTaskData implements WorkerTaskData{
    private final String classifier;
    private final int version;
    private final TaskStatus taskStatus;
    private byte[] data;
    private byte[] context;
    private final TrackingInfo trackingInfo;
    private final TaskSourceInfo sourceInfo;

    public TestWorkerTaskData(String classifier, int version, TaskStatus taskStatus, byte[] data, byte[] context,
                          TrackingInfo trackingInfo, TaskSourceInfo sourceInfo){
        this.classifier = classifier;
        this.version = version;
        this.taskStatus = taskStatus;
        this.data = data;
        this.context = context;
        this.trackingInfo = trackingInfo;
        this.sourceInfo = sourceInfo;
    }

    /**
     * Retrieves an indicator of the type of the task
     */
    @Override
    public String getClassifier() {
        return classifier;
    }

    /**
     * Retrieves the version of the task message used
     */
    @Override
    public int getVersion() {
        return version;
    }

    /**
     * Retrieves the task status
     */
    @Override
    public TaskStatus getStatus() {
        return taskStatus;
    }

    /**
     * Retrieves the actual task data in a serialised form
     */
    @Override
    public byte[] getData() {
        return data;
    }

    /**
     * Allows for setting the data. Convenient for test purposes.
     * @param data The byte array to set as the value of 'data' on the TestWorkerTaskData object.
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Retrieves any task specific context associated with the task
     */
    @Override
    public byte[] getContext() {
        return context;
    }

    /**
     * Allows for setting the context. Convenient for test purposes.
     * @param context The byte array to set as the value of 'context' on the TestWorkerTaskData object.
     */
    public void setContext(byte[] context) {
        this.context = context;
    }

    /**
     * Retrieves tracking information associated with the task
     */
    @Override
    public TrackingInfo getTrackingInfo() {
        return trackingInfo;
    }

    /**
     * Retrieves information relating to the source of the task
     */
    @Override
    public TaskSourceInfo getSourceInfo() {
        return sourceInfo;
    }
}