/*
 * Copyright Strimzi authors. License: Apache License 2.0 (see the file LICENSE or
 * http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

public class Config {

    public static final class PropertyNames {
        public static final String LISTENING_PORT = "listening_port";
        public static final String KAFKA_BROKERS = "kafka_broker";
        public static final String POLICY_REPO = "topic_policies";
        public static final String KMS_CONFIG = "kms_defs";

        private PropertyNames() {
        }
    }

    private String brokers;
    private String policyFile;
    private String kmsConfigFile;
    private int listeningPort;

    public int getListeningPort() {
        return listeningPort;
    }

    public Config setListeningPort(int port) {
        listeningPort = port;
        return this;
    }

    public String kafkaHostname() {
        return brokers;
    }

    public Config setBrokers(String brokers) {
        this.brokers = brokers;
        return this;
    }

    public String getPolicyFile() {
        return policyFile;
    }

    public Config setPolicyFile(String filename) {
        this.policyFile = filename;
        return this;
    }

    public String getKmsConfigFile() {
        return kmsConfigFile;
    }

    public Config setKmsConfigFile(String filename) {
        this.kmsConfigFile = filename;
        return this;
    }
}
