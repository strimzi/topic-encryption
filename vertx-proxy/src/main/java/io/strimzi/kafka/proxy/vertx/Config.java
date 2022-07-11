/*
 * Copyright Strimzi authors. License: Apache License 2.0 (see the file LICENSE or
 * http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

import io.strimzi.kafka.topicenc.policy.PolicyRepository;
import io.strimzi.kafka.topicenc.policy.TestPolicyRepo;
import io.vertx.core.json.JsonObject;

public class Config {

  public static final class PropertyNames {
    public static final String LISTENING_PORT = "listening_port";
    public static final String KAFKA_BROKERS = "kafka_broker";
    public static final String POLICY_REPO = "policy_repo";

    private PropertyNames() {}
  }

  private final String brokers;
  private final PolicyRepository policyRepo;
  private final int listeningPort;

  public Config(int listeningPort, String kafkaHostname, PolicyRepository policyRepo) {
    this.listeningPort = listeningPort;
    this.brokers = kafkaHostname;
    this.policyRepo = policyRepo;
  }

  public int getListeningPort() {
    return listeningPort;
  }

  public String kafkaHostname() {
    return brokers;
  }

  public PolicyRepository policyRepo() {
    return policyRepo;
  }

  public static Config toConfig(JsonObject jsonConfig) {
    String brokers = getParam(jsonConfig, PropertyNames.KAFKA_BROKERS);
    if (brokers.indexOf(':') == -1) {
      throw new IllegalArgumentException("Broker must be specified as 'hostname:port'");
    }
    int listeningPort = getIntParam(jsonConfig, PropertyNames.LISTENING_PORT);

    String policyRepo = getParam(jsonConfig, PropertyNames.POLICY_REPO);
    if (!policyRepo.equalsIgnoreCase("test")) {
      // only test repo supported currently.
      throw new IllegalArgumentException("Unsupported policy repo");
    }
    return new Config(listeningPort, brokers, new TestPolicyRepo());
  }

  private static String getParam(JsonObject jsonConfig, String paramName) {
    String param = jsonConfig.getString(paramName);
    if (isEmpty(param)) {
      throw new IllegalArgumentException("Configuration missing field, " + paramName);
    }
    return param;
  }

  private static int getIntParam(JsonObject jsonConfig, String paramName) {
    if (!jsonConfig.containsKey(paramName)) {
      throw new IllegalArgumentException("Configuration missing field, " + paramName);
    }
    return jsonConfig.getInteger(paramName);
  }

  private static boolean isEmpty(String s) {
    return s == null || s.trim().length() == 0;
  }
}
