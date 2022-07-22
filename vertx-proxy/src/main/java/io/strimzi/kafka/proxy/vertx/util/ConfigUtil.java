/*
 * Copyright Strimzi authors. License: Apache License 2.0 (see the file LICENSE or
 * http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx.util;

import io.strimzi.kafka.proxy.vertx.Config;
import io.strimzi.kafka.topicenc.common.Strings;
import io.vertx.core.json.JsonObject;

/**
 * Utility methods for loading the proxy configuration from a JSON file.
 */
public class ConfigUtil {

    /**
     * Given a proxy configuration in JSON format, convert it to a Config instance.
     * 
     * @param jsonConfig the JSON configuration object
     * @return an instance of Config
     */
    public static Config toProxyConfig(JsonObject jsonConfig) {
        String brokers = getParam(jsonConfig, Config.PropertyNames.KAFKA_BROKERS);
        if (brokers.indexOf(':') == -1) {
            throw new IllegalArgumentException("Broker must be specified as 'hostname:port'");
        }
        int listeningPort = getIntParam(jsonConfig, Config.PropertyNames.LISTENING_PORT);
        String policyRepo = getParam(jsonConfig, Config.PropertyNames.POLICY_REPO);
        String kmsConfigFile = getParam(jsonConfig, Config.PropertyNames.KMS_CONFIG);

        Config config = new Config()
                .setBrokers(brokers)
                .setListeningPort(listeningPort)
                .setPolicyFile(policyRepo)
                .setKmsConfigFile(kmsConfigFile);
        return config;
    }

    /**
     * Utility method for extracting a string field from a JSON object. Assumes the
     * field is required and throws an IllegalArgumentException if the field is not
     * present.
     *
     * @param jsonConfig the input JSON object
     * @param paramName  the name of the field to extract
     * @return the value of the request field.
     */
    private static String getParam(JsonObject jsonConfig, String paramName) {
        String param = jsonConfig.getString(paramName);
        if (Strings.isNullOrEmpty(param)) {
            throw new IllegalArgumentException("Configuration missing field, " + paramName);
        }
        return param;
    }

    /**
     * Utility method for extracting an integer field from a JSON object. Assumes
     * the field is required and throws an IllegalArgumentException if the field is
     * not present.
     * 
     * @param jsonConfig
     * @param paramName
     * @return
     */
    private static int getIntParam(JsonObject jsonConfig, String paramName) {
        if (!jsonConfig.containsKey(paramName)) {
            throw new IllegalArgumentException("Configuration missing field, " + paramName);
        }
        return jsonConfig.getInteger(paramName);
    }
}
