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

package io.temporal.testserver.functional.timeskipping;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.TestServiceStubs;
import io.temporal.serviceclient.TestServiceStubsOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.testing.internal.SDKTestOptions;
import io.temporal.testserver.TestServer;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Async;
import io.temporal.workflow.Workflow;
import org.junit.*;

public class TimeSkippingFromAnActivityTest {
  private static final String TASK_QUEUE = "task-queue";

  public static class SleepingActivityImpl implements SleepingActivity {

    @Override
    public void sleep() {
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class TestWorkflowImpl implements TestWorkflow {
    @Override
    public void execute() {
      SleepingActivity activity =
          Workflow.newActivityStub(
              SleepingActivity.class, SDKTestOptions.newActivityOptionsForTaskQueue(TASK_QUEUE));
      Async.procedure(activity::sleep);
    }
  }

  private TestServer.InProcessTestServer server;
  private WorkflowServiceStubs workflowServiceStubs;
  private TestServiceStubs testServiceStubs;
  private WorkflowClient workflowClient;
  private WorkerFactory wf;

  @Before
  public void setUp() {
    this.server = TestServer.createServer(false, 0);
    this.workflowServiceStubs =
        WorkflowServiceStubs.newInstance(
            WorkflowServiceStubsOptions.newBuilder().setChannel(server.getChannel()).build());
    this.testServiceStubs =
        TestServiceStubs.newInstance(
            TestServiceStubsOptions.newBuilder()
                .setChannel(workflowServiceStubs.getRawChannel())
                .validateAndBuildWithDefaults());

    this.workflowClient = WorkflowClient.newInstance(workflowServiceStubs);
    this.wf = WorkerFactory.newInstance(workflowClient);
    Worker worker = wf.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(TestWorkflowImpl.class);
    worker.registerActivitiesImplementations(new SleepingActivityImpl());
    this.wf.start();
  }

  @After
  public void tearDown() {
    this.wf.shutdownNow();
    this.testServiceStubs.shutdownNow();
    this.workflowServiceStubs.shutdownNow();
    this.server.close();
  }

  @Test
  public void testAbandonActivity() {
    WorkflowClient.newInstance(workflowServiceStubs)
        .newWorkflowStub(
            TestWorkflow.class, SDKTestOptions.newWorkflowOptionsWithTimeouts(TASK_QUEUE))
        .execute();
    // time skipping is locked here. Workflow is done, but the activity is not
  }
}
