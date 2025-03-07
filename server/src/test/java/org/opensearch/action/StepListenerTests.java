/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.action;

import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.TestThreadPool;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.RemoteTransportException;
import org.junit.After;
import org.junit.Before;
import org.opensearch.action.StepListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.equalTo;

public class StepListenerTests extends OpenSearchTestCase {
    private ThreadPool threadPool;

    @Before
    public void setUpThreadPool() {
        threadPool = new TestThreadPool(getTestName());
    }

    @After
    public void tearDownThreadPool() {
        terminate(threadPool);
    }

    public void testSimpleSteps() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Consumer<Exception> onFailure = e -> {
            latch.countDown();
            fail("test a happy path");
        };

        StepListener<String> step1 = new StepListener<>(); //[a]sync provide a string
        executeAction(() -> step1.onResponse("hello"));
        StepListener<Integer> step2 = new StepListener<>(); //[a]sync calculate the length of the string
        step1.whenComplete(str -> executeAction(() -> step2.onResponse(str.length())), onFailure);
        step2.whenComplete(length -> executeAction(latch::countDown), onFailure);
        latch.await();
        assertThat(step1.result(), equalTo("hello"));
        assertThat(step2.result(), equalTo(5));
    }

    public void testAbortOnFailure() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        int failedStep = randomBoolean() ? 1 : 2;
        AtomicInteger failureNotified = new AtomicInteger();
        Consumer<Exception> onFailure = e -> {
            failureNotified.getAndIncrement();
            latch.countDown();
            assertThat(e.getMessage(), equalTo("failed at step " + failedStep));
        };

        StepListener<String> step1 = new StepListener<>(); //[a]sync provide a string
        if (failedStep == 1) {
            executeAction(() -> step1.onFailure(new RuntimeException("failed at step 1")));
        } else {
            executeAction(() -> step1.onResponse("hello"));
        }

        StepListener<Integer> step2 = new StepListener<>(); //[a]sync calculate the length of the string
        step1.whenComplete(str -> {
            if (failedStep == 2) {
                executeAction(() -> step2.onFailure(new RuntimeException("failed at step 2")));
            } else {
                executeAction(() -> step2.onResponse(str.length()));
            }
        }, onFailure);

        step2.whenComplete(length -> latch.countDown(), onFailure);
        latch.await();
        assertThat(failureNotified.get(), equalTo(1));

        if (failedStep == 1) {
            assertThat(expectThrows(RuntimeException.class, step1::result).getMessage(),
                equalTo("failed at step 1"));
            assertThat(expectThrows(RuntimeException.class, step2::result).getMessage(),
                equalTo("step is not completed yet"));
        } else {
            assertThat(step1.result(), equalTo("hello"));
            assertThat(expectThrows(RuntimeException.class, step2::result).getMessage(),
                equalTo("failed at step 2"));
        }
    }

    private void executeAction(Runnable runnable) {
        if (randomBoolean()) {
            threadPool.generic().execute(runnable);
        } else {
            runnable.run();
        }
    }

    /**
     * This test checks that we no longer unwrap exceptions when using StepListener.
     */
    public void testNoUnwrap() {
        StepListener<String> step = new StepListener<>();
        step.onFailure(new RemoteTransportException("test", new RuntimeException("expected")));
        AtomicReference<RuntimeException> exception = new AtomicReference<>();
        step.whenComplete(null, e -> {
            exception.set((RuntimeException) e);
        });

        assertEquals(RemoteTransportException.class, exception.get().getClass());
        RuntimeException e = expectThrows(RuntimeException.class, () -> step.result());
        assertEquals(RemoteTransportException.class, e.getClass());
    }
}
