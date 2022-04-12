/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

import java.util.Arrays;

import org.apache.kafka.common.requests.RequestHeader;

import io.vertx.core.buffer.Buffer;

public class KafkaReqMsg extends AbstractKafkaMsg {
	
	private final static int FIXED_HEADER_LEN = 10;

	RequestHeader header;
	byte[] headerBytes;
	
	public KafkaReqMsg(Buffer rawMsg) {
	    super(rawMsg);
	}
	
	public RequestHeader getHeader() {
	    if (header == null) {
	        
	        header = RequestHeader.parse(getPayload());
	    }
		return header;
	}
	
	public byte[] getHeaderBytes() {
		if (headerBytes == null) {
			// to do: clarify +1
	    	int headerSize = FIXED_HEADER_LEN + getClientIdLen(); 
	    	int destIndex = MSG_SIZE_LEN + headerSize + 1;
			headerBytes = Arrays.copyOfRange(rawMsg.getBytes(),
					                         MSG_SIZE_LEN,
					                         destIndex);
		}
		return headerBytes;
	}
	
	private int getClientIdLen() {
		String clientId = header.clientId();
		int clientIdLen = clientId != null ? clientId.length() : 0;
		return clientIdLen;
	}
}
