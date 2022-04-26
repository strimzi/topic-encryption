/*
 * Copyright Strimzi authors. License: Apache License 2.0 (see the file LICENSE or
 * http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class VertRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertRunner.class);

    public static void main(String[] args) {

        ConfigStoreOptions fileStore = new ConfigStoreOptions().setType("file")
                .setConfig(new JsonObject().put("path", "config.json"));
        ConfigRetrieverOptions retrOptions = new ConfigRetrieverOptions();
        retrOptions.addStore(fileStore);

        Vertx vertx = Vertx.vertx();
        ConfigRetriever retriever = ConfigRetriever.create(vertx, retrOptions);
        Future<JsonObject> configFuture = retriever.getConfig();
        configFuture.onFailure(e -> {
            LOGGER.error("Error loading configuration", e);
            vertx.close().onComplete(h -> {
                LOGGER.info("Shutdown");
            });
        }).onSuccess(jsonObj -> {
            deployVerticle(vertx, jsonObj);
        });

        LOGGER.info("Shutdown");
    }

    private static void deployVerticle(Vertx vertx, JsonObject configJson) {
        KafkaProxyVerticle proxy = new KafkaProxyVerticle();
        Future<String> deployFuture =
                vertx.deployVerticle(proxy, new DeploymentOptions().setConfig(configJson));
        deployFuture.onFailure(e -> {
            LOGGER.error("Error deploying proxy verticle", e);
            vertx.close().onComplete(h -> {
                LOGGER.info("Shutdown");
            });
        }).onSuccess(s -> {
            LOGGER.info("Proxy verticle deployed {}", s);
        });
    }
}
