/**
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.security.scanner.servlets;

import com.google.appengine.api.utils.SystemProperty;
import com.google.cloud.dataflow.sdk.io.TextIO;
import com.google.cloud.dataflow.sdk.options.DataflowPipelineOptions;
import com.google.cloud.dataflow.sdk.options.PipelineOptions;
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory;
import com.google.cloud.dataflow.sdk.runners.BlockingDataflowPipelineRunner;
import com.google.cloud.security.scanner.common.CloudUtil;
import com.google.cloud.security.scanner.common.Constants;
import com.google.cloud.security.scanner.pipelines.OnDemandLiveStateChecker;
import com.google.cloud.security.scanner.sources.GCSFilesSource;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * For running the Dataflow pipeline directly using the Dataflow SDK.
 */
public class LiveStateCheckerRunner {

  /**
   * Main function for the runner.
   * @param args The args this program was called with.
   * @throws IOException Thrown if there's an error reading from one of the APIs.
   */
  public static void main(String[] args) throws IOException {
    Preconditions.checkNotNull(Constants.ORG_NAME);
    Preconditions.checkNotNull(Constants.POLICY_BUCKET);
    Preconditions.checkNotNull(Constants.OUTPUT_PREFIX);
    Preconditions.checkNotNull(Constants.DATAFLOW_STAGING);
    GCSFilesSource source = null;
    try {
      source = new GCSFilesSource(Constants.POLICY_BUCKET, Constants.ORG_NAME);
    } catch (GeneralSecurityException e) {
      throw new IOException("SecurityException: Cannot create GCSFileSource");
    }
    PipelineOptions options;
    if (CloudUtil.willExecuteOnCloud()) {
      options = getCloudExecutionOptions(Constants.DATAFLOW_STAGING);
    } else {
      options = getLocalExecutionOptions();
    }
    new OnDemandLiveStateChecker(options, source)
        .attachSink(TextIO.Write.named("Write messages to GCS").to(Constants.OUTPUT_PREFIX))
        .run();
  }

  private static PipelineOptions getCloudExecutionOptions(String stagingLocation) {
    DataflowPipelineOptions options = PipelineOptionsFactory.as(DataflowPipelineOptions.class);
    options.setProject(SystemProperty.applicationId.get());
    options.setStagingLocation(stagingLocation);
    options.setRunner(BlockingDataflowPipelineRunner.class);
    return options;
  }

  private static PipelineOptions getLocalExecutionOptions() {
    return PipelineOptionsFactory.create();
  }
}
