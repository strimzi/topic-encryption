/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;


import org.apache.kafka.common.utils.AppInfoParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.strimzi.kafka.topicenc.EncryptionModule;
import io.strimzi.kafka.topicenc.kms.TestKms;
import io.strimzi.kafka.topicenc.policy.TestPolicyRepo;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;

public class KafkaProxyVerticle extends AbstractVerticle {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProxyVerticle.class);
	
	public static final String CTX_KEY_CONFIG = "topicenc.config";
	public static final String CTX_KEY_ENCMOD = "topicenc.encmod";

	
	private static final int LISTENING_PORT = 1234;
	private static final int KAFKA_PORT = 9092;
	private static final String KAFKA_HOSTNAME = "localhost";
	
	NetServerOptions opts = new NetServerOptions();
	String kafkaHostname = KAFKA_HOSTNAME;
	int kafkaPort = KAFKA_PORT;
	Config config;
	EncryptionModule encMod;

	public void start() {
		LOGGER.info("in start");
	}
	
	@Override
	public void init(Vertx vertx, Context context) {
		super.init(vertx, context);
		LOGGER.info("Kafka version: " + AppInfoParser.getVersion());
		//LOGGER.info(System.getProperties().toString());
		
		// currently "manual" config:
		config = new Config(KAFKA_HOSTNAME, KAFKA_PORT, new TestPolicyRepo());
		context.put(CTX_KEY_CONFIG, config);
        try {
            encMod = new EncryptionModule(config.policyRepo(), new TestKms());
            context.put(CTX_KEY_ENCMOD, encMod);
        } catch (Exception e) {
            LOGGER.error("Error creating encryption module", e);
            stop();
            System.exit(1);
        }
	}
	
	@Override
	public void start(Promise<Void> promise) {
		LOGGER.debug("in start(Promise<Void>)");
		
		NetServerOptions opts = new NetServerOptions();
		opts.setPort(LISTENING_PORT);
		
		vertx.createNetServer(opts)
			.connectHandler(new TopicEncryptingSocketHandler(context))
			.listen(new Handler<AsyncResult<NetServer>>() {
				@Override
				public void handle(AsyncResult<NetServer> event) {
					if (event.failed()) {
						LOGGER.info("listen failed: " + event.cause().toString());
					}
					else if (event.succeeded()) {
						LOGGER.info("Listening on port " + opts.getPort());
					}
				}
			});
	}
	
	@Override
	public void stop() {
		LOGGER.debug("in stop()");
	}

	@Override
	public void stop(Promise<Void> promise) {
		LOGGER.debug("in stop(Promise<Void>)");
	}
}
