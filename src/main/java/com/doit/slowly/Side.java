package com.doit.slowly;

import java.util.List;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.GenerateSequence;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.*;
import org.apache.beam.sdk.values.*;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Side {
  private static final Logger LOG = LoggerFactory.getLogger(Side.class);

  public static void slowly(final Pipeline pipeline, final Duration refreshDuration) {
    // Create a single side input that updates at each interval
    final PCollectionView<List<List<KV<String, String>>>> data =
        pipeline
            .apply(GenerateSequence.from(0).withRate(1, refreshDuration))
            .apply(
                Window.<Long>into(new GlobalWindows())
                    .triggering(Repeatedly.forever(AfterProcessingTime.pastFirstElementInPane()))
                    .discardingFiredPanes())
            .apply("Read from Redis", ParDo.of(new CallRedis("10.120.16.4", 6379)))
            .apply(View.asList());

    // Consume side input. GenerateSequence generates test data.
    // Use a real source (like PubSubIO or KafkaIO) in production.
    pipeline
        .apply(GenerateSequence.from(0).withRate(1, Duration.standardSeconds(1L)))
        .apply(
            ParDo.of(
                    new DoFn<Long, KV<Long, Long>>() {

                      @ProcessElement
                      public void process(ProcessContext c) {
                        final List<KV<String, String>> sideInput = c.sideInput(data).get(0);
                        c.outputWithTimestamp(KV.of(1L, c.element()), Instant.now());

                        LOG.warn("Value is {} and side input is {}", c.element(), sideInput);
                      }
                    })
                .withSideInputs(data));
  }

  public static void main(String[] args) {
    final PipelineHelperOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(PipelineHelperOptions.class);
    options.setStreaming(true);

    final Pipeline pipeline = Pipeline.create(options);
    final Duration refreshDuration = Duration.standardSeconds(options.getRefreshDuration().get());

    slowly(pipeline, refreshDuration);

    pipeline.run().waitUntilFinish();
  }
}
