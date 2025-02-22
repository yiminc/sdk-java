/*
 *  Copyright (C) 2020 Temporal Technologies, Inc. All Rights Reserved.
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.internal.worker;

import static org.junit.Assert.assertEquals;

import io.temporal.api.common.v1.RetryPolicy;
import io.temporal.common.RetryOptions;
import io.temporal.internal.common.ProtobufTimeUtils;
import java.time.Duration;
import org.junit.Test;

public class LocalActivityWorkerTest {
  @Test
  public void buildRetryOptions() {
    Duration initialInterval = Duration.ofSeconds(2);
    Duration maxInterval = Duration.ofSeconds(5);
    RetryPolicy retryPolicy =
        RetryPolicy.newBuilder()
            .setInitialInterval(ProtobufTimeUtils.toProtoDuration(initialInterval))
            .setMaximumInterval(ProtobufTimeUtils.toProtoDuration(maxInterval))
            .setMaximumAttempts(5)
            .setBackoffCoefficient(2)
            .addNonRetryableErrorTypes(IllegalStateException.class.getName())
            .build();

    RetryOptions retryOptions = LocalActivityWorker.buildRetryOptions(retryPolicy);
    assertEquals(initialInterval, retryOptions.getInitialInterval());
    assertEquals(maxInterval, retryOptions.getMaximumInterval());
    assertEquals(5, retryOptions.getMaximumAttempts());
    assertEquals(2, retryOptions.getBackoffCoefficient(), 0.001);
    assertEquals(IllegalStateException.class.getName(), retryOptions.getDoNotRetry()[0]);
  }
}
