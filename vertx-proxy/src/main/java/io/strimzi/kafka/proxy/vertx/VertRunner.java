/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class VertRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertRunner.class);    

	public static void main(String[] args) {
	    
	    ConfigStoreOptions fileStore = new ConfigStoreOptions()
	            .setType("file")
	            .setConfig(new JsonObject().put("path", "config.json"));	 
	    ConfigRetrieverOptions retrOptions = new ConfigRetrieverOptions();
	    retrOptions.addStore(fileStore);
	    
	    Vertx vertx = Vertx.vertx();	    
	    ConfigRetriever retriever = ConfigRetriever.create(vertx, retrOptions);
	    retriever.getConfig(ar -> {
	        if (ar.failed()) {
	            LOGGER.error("Unable to load configuration", ar.cause());
	            System.exit(1);
	        }
            JsonObject config = ar.result();
            KafkaProxyVerticle proxy = new KafkaProxyVerticle();
            vertx.deployVerticle(proxy, new DeploymentOptions().setConfig(config));
	    });
	}
}
