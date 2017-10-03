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
package com.github.cafdataprocessing.worker.policy.converter.qa;

import com.hpe.caf.api.worker.TaskSourceInfo;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.api.worker.WorkerTaskData;

/**
 * Implementation of WorkerTaskData for test purposes
 */
public class TestWorkerTaskData implements WorkerTaskData
{
    private final String classifier;
    private final int version;
    private final TaskStatus taskStatus;
    private final byte[] data;
    private final byte[] context;
    private final TrackingInfo trackingInfo;
    private final TaskSourceInfo sourceInfo;

    public TestWorkerTaskData(String classifier, int version, TaskStatus taskStatus, byte[] data, byte[] context,
                              TrackingInfo trackingInfo, TaskSourceInfo sourceInfo)
    {
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
    public String getClassifier()
    {
        return classifier;
    }

    /**
     * Retrieves the version of the task message used
     */
    @Override
    public int getVersion()
    {
        return version;
    }

    /**
     * Retrieves the task status
     */
    @Override
    public TaskStatus getStatus()
    {
        return taskStatus;
    }

    /**
     * Retrieves the actual task data in a serialised form
     */
    @Override
    public byte[] getData()
    {
        return data;
    }

    /**
     * Retrieves any task specific context associated with the task
     */
    @Override
    public byte[] getContext()
    {
        return context;
    }

    /**
     * Retrieves tracking information associated with the task
     */
    @Override
    public TrackingInfo getTrackingInfo()
    {
        return trackingInfo;
    }

    /**
     * Retrieves information relating to the source of the task
     */
    @Override
    public TaskSourceInfo getSourceInfo()
    {
        return sourceInfo;
    }

    @Override
    public void addResponse(WorkerResponse workerResponse, boolean b) {
    }
}
