package com.doit.slowly;

import static java.lang.String.format;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

public class CallRedis extends DoFn<Long, List<KV<String, String>>> {
  private static final Logger LOG = LoggerFactory.getLogger(CallRedis.class);
  private transient Jedis jedis;
  final HostAndPort hostAndPort;

  public CallRedis(final String host, final int port) {
    hostAndPort = new HostAndPort(host, port);
  }

  @Setup
  public void setup() {
    jedis = new Jedis(hostAndPort);
  }

  @Teardown
  public void teardown() {
    jedis.close();
  }

  @ProcessElement
  public void processElement(ProcessContext processContext) {
    final String keypattern = "redis";
    final Set<String> keys = jedis.keys(format("*%s*", keypattern));
    final List<KV<String, String>> result =
        keys.stream()
            .filter(x -> x != null && x.contains(keypattern))
            .map(
                key -> {
                  final String value = jedis.get(key);
                  LOG.info("Redis key value: {}, {}", key, value);
                  return KV.of(key, value);
                })
            .collect(Collectors.toList());
    processContext.output(result);
  }
}
