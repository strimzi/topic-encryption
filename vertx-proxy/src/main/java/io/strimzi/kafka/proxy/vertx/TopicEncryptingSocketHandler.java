/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.strimzi.kafka.topicenc.EncryptionModule;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.net.NetSocket;

/**
 * This handler integrates the Encryption Module
 * into the vert.x proxy framework. Here we are receiving client requests,
 * processing them if needed, forwarding to the broker, processing 
 * the response, and forwarding to the client.
 * The philosophy is to instantiate only requests we are interested in.
 * All messages which we either are not interested in or which we do
 * not change are passed on to the broker as is, in original form.
 * The encryption module is configured and instantiated outside of this
 * handler and passed here by means of the vert.x Context.
 */
public class TopicEncryptingSocketHandler implements Handler<NetSocket> {

	private static final Logger LOGGER = LoggerFactory.getLogger(TopicEncryptingSocketHandler.class);
	
	Context context;
	EncryptionModule encMod;
	Config config;
	Map<NetSocket, MessageHandler> activeHandlers = new HashMap<>();
	
	/**
	 * Constructor. The handler retrieves config and enccryption module
	 * instances from the provided context.
	 * @param context
	 */
	public TopicEncryptingSocketHandler(Context context) {
		//super();
		this.context = context;
    	this.encMod = context.get(KafkaProxyVerticle.CTX_KEY_ENCMOD);
    	if (Objects.isNull(encMod)) {
    		throw new NullPointerException("No encryption module");
    	}
    	this.config = context.get(KafkaProxyVerticle.CTX_KEY_CONFIG);    	
        if (Objects.isNull(config)) {
            throw new NullPointerException("No config object"); 
        }
	}
	
    /**
	 * Here client sockets are received.
	 */
	@Override
	public void handle(NetSocket clientSocket) {
		LOGGER.info("New client socket " + clientSocket.remoteAddress().toString());

		// create a message handler and store in the activeHandlers map:
	    MessageHandler msgHandler = new MessageHandler(context, clientSocket);
	    activeHandlers.put(clientSocket, msgHandler);

	    // assign the socket's handlers, most notably the msgHandler
		clientSocket
		  .handler(msgHandler)
		  .exceptionHandler(e -> {
		      LOGGER.info("Client socket exception: {}",e);}
		   )
		  .closeHandler(x -> { 
		    LOGGER.info("Client socket closed: {} ", clientSocket.remoteAddress().toString());
		    // activeHandlers should be concurrent map
		    MessageHandler h = activeHandlers.remove(clientSocket);
		    if (h != null) {
		        h.close();
		    }
		   });
	}
}
	
