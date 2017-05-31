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
package com.github.cafdataprocessing.worker.policy.testing.shared;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log the currently running test.
 *
 * <p>
 * Typical usage:
 *
 * <p>
 * {@code @Rule public LogTestName logTestName = new LogTestName();}
 *
 * <p>
 * See also:
 * <br>{@link org.junit.Rule}
 * <br>{@link org.junit.rules.TestWatcher}
 */
public class LogTestName extends TestWatcher
{

    private final static Logger log = LoggerFactory.getLogger(LogTestName.class);

    /**
     * Invoked when a test is about to start
     * @param description
     */
    @Override
    protected void starting(Description description)
    {
        log.info("Starting Test {}", description.getMethodName());
    }

    /**
     * Invoked when a test succeeds
     * @param description
     */
    @Override
    protected void succeeded(Description description)
    {
        log.info("Succeeded Test {}", description.getMethodName());
    }

    /**
     * Invoked when a test fails
     * @param e
     * @param description
     */
    @Override
    protected void failed(Throwable e, Description description)
    {
        log.info("Failed Test {} due to exception {}", description.getMethodName(), ExceptionUtils.getStackTrace(e) );
    }

    /**
     * Invoked when a test is skipped due to a failed assumption.
     * @param e
     * @param description
     */
    @Override
    protected void skipped(AssumptionViolatedException e, Description description)
    {
        log.info("Skipped Test {}", description.getMethodName());
    }

    /**
     * Invoked when a test method finishes (whether passing or failing)
     * @param description
     */
    @Override
    protected void finished(Description description)
    {
        log.info("Finished Test {}", description.getMethodName());
    }

}
