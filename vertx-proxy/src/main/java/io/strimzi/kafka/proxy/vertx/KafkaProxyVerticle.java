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

	Config config;
	EncryptionModule encMod;

	@Override
	public void init(Vertx vertx, Context context) {
		super.init(vertx, context);
		JsonObject jsonConfig = config();
		
		try {
		    config = Config.toConfig(jsonConfig);
		} catch (Exception e) {
		    LOGGER.error("Configuration error", e);
		    System.exit(1);
		}
		
		LOGGER.info("Kafka version: " + AppInfoParser.getVersion());
		
		// currently "manual" config:
		context.put(CTX_KEY_CONFIG, config);
        try {
            encMod = new EncryptionModule(config.policyRepo(), new TestKms());
            context.put(CTX_KEY_ENCMOD, encMod);
        } catch (Exception e) {
            LOGGER.error("Error creating encryption module", e);
            System.exit(1);
        }
	}
	
	@Override
	public void start(Promise<Void> promise) {
		LOGGER.debug("starting");
		
		NetServerOptions opts = new NetServerOptions();
		opts.setPort(config.getListeningPort());
		
		vertx.createNetServer(opts)
			.connectHandler(new TopicEncryptingSocketHandler(context))
			.listen(new Handler<AsyncResult<NetServer>>() {
				@Override
				public void handle(AsyncResult<NetServer> event) {
					if (event.failed()) {
						LOGGER.info("Listen failed: " + event.cause().toString());
					}
					else if (event.succeeded()) {
						LOGGER.info("Listening on port " + opts.getPort());
					}
				}
			});
	}
}
