package com.doit.slowly;

import org.apache.beam.sdk.extensions.gcp.options.GcpOptions;
import org.apache.beam.sdk.options.*;
import org.apache.beam.sdk.options.Validation.Required;

public interface PipelineHelperOptions extends PipelineOptions, StreamingOptions, GcpOptions {
  @Description(
      "How often we should update the database on the side in seconds, should be an integer")
  @Required
  ValueProvider<Long> getRefreshDuration();

  void setRefreshDuration(ValueProvider<Long> value);
}
