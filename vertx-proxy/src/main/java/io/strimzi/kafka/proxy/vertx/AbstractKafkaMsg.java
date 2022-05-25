/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

import java.nio.ByteBuffer;
import java.util.Arrays;

import io.vertx.core.buffer.Buffer;

public class AbstractKafkaMsg {
	
	protected final static int MSG_SIZE_LEN = 4;

	Buffer rawMsg;
	ByteBuffer payload;
	
	public AbstractKafkaMsg(Buffer rawMsg) {
		this.rawMsg = rawMsg;
	}
	
	public Buffer getRawMsg() {
		return rawMsg;
	}
	
	public ByteBuffer getPayload() {
	    if (payload == null) {
	        payload = extractKafkaPayload(rawMsg);
	    }
		return payload;
	}

	protected short getApiKey() {
		return rawMsg.getShort(4);
	}

	protected ByteBuffer extractKafkaPayload(Buffer kafkaMsg) {
		// copy bytes after leading 4 bytes containing message len:
		byte[] dataBytes = Arrays.copyOfRange(kafkaMsg.getBytes(), 4, kafkaMsg.getBytes().length);
        return ByteBuffer.wrap(dataBytes);
	}
}
