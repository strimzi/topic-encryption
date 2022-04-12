/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.kafka.common.requests.ResponseHeader;

import io.vertx.core.buffer.Buffer;

public class KafkaRspMsg extends AbstractKafkaMsg {
	
	private final static int FIXED_HEADER_LEN = 4;

	ResponseHeader header;
	byte[] headerBytes;
	short version;
	
	public KafkaRspMsg(Buffer rawMsg, short version) {
	    super(rawMsg);
	    this.version = version;
	}
	
	public short getVersion() {
	    return version;
	}
	
	public ResponseHeader getHeader() {
	    if (header == null) {
	        header = ResponseHeader.parse(getPayload(), version);
	    }
		return header;
	}
	
	public byte[] getHeaderBytes() {
	    
		if (headerBytes == null) {
			// to do: test, not correct
	    	int headerSize = FIXED_HEADER_LEN; 
	    	int destIndex = MSG_SIZE_LEN + headerSize + 1;
			headerBytes = Arrays.copyOfRange(rawMsg.getBytes(),
					                         MSG_SIZE_LEN,
					                         destIndex);
		}
		return headerBytes;
	}
	
	@Override
    protected ByteBuffer extractKafkaPayload(Buffer kafkaMsg) {
        // copy bytes after leading 4 bytes containing message len:
	    int msgLen = kafkaMsg.length();
	    int headerLen = MSG_SIZE_LEN; // + FIXED_HEADER_LEN;
	    return ByteBuffer.wrap(kafkaMsg.getBytes(headerLen, msgLen));
        //byte[] dataBytes = Arrays.copyOfRange(kafkaMsg.getBytes(), 0, kafkaMsg.getBytes().length);
        //return ByteBuffer.wrap(dataBytes);
    }
	
}
