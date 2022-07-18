/*
 * Copyright Strimzi authors. License: Apache License 2.0 (see the file LICENSE or
 * http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.strimzi.kafka.proxy.vertx.util.ConfigUtil;
import io.strimzi.kafka.topicenc.EncryptionModule;
import io.strimzi.kafka.topicenc.policy.InMemoryPolicyRepository;
import io.strimzi.kafka.topicenc.policy.JsonPolicyLoader;
import io.strimzi.kafka.topicenc.policy.TopicPolicy;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;

public class KafkaProxyVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProxyVerticle.class);

    public static final String CTX_KEY_CONFIG = "topicenc.config";
    public static final String CTX_KEY_ENCMOD = "topicenc.encmod";

    private Config config;
    private EncryptionModule encMod;

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        JsonObject jsonConfig = config();

        this.config = ConfigUtil.toProxyConfig(jsonConfig);
        context.put(CTX_KEY_CONFIG, config);

        try {
            List<TopicPolicy> topicPolicy = JsonPolicyLoader.loadTopicPolicies(
                    new File(config.getKmsConfigFile()),
                    new File(config.getPolicyFile()));

            InMemoryPolicyRepository policy = new InMemoryPolicyRepository(topicPolicy);

            encMod = new EncryptionModule(policy);

        } catch (Exception e) {
            throw new RuntimeException("Error initializing Encryption Module", e);
        }
        context.put(CTX_KEY_ENCMOD, encMod);
    }

    @Override
    public void start(Promise<Void> promise) {
        LOGGER.debug("starting");

        NetServerOptions opts = new NetServerOptions();
        opts.setPort(config.getListeningPort());

        vertx.createNetServer(opts).connectHandler(new TopicEncryptingSocketHandler(context))
                .listen(new Handler<AsyncResult<NetServer>>() {
                    @Override
                    public void handle(AsyncResult<NetServer> event) {
                        if (event.failed()) {
                            LOGGER.info("Listen failed: " + event.cause().toString());
                        } else if (event.succeeded()) {
                            LOGGER.info("Listening on port " + opts.getPort());
                        }
                    }
                });
    }
}
