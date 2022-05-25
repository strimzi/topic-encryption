/*
 * Copyright Strimzi authors. License: Apache License 2.0 (see the file LICENSE or
 * http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

import java.util.Arrays;

import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.requests.RequestHeader;
import io.vertx.core.buffer.Buffer;

public class KafkaReqMsg extends AbstractKafkaMsg {

    private final static int FIXED_HEADER_LEN = 10;

    private RequestHeader header;
    private byte[] headerBytes;

    public KafkaReqMsg(Buffer rawMsg) {
        super(rawMsg);
    }

    public RequestHeader getHeader() {
        if (header == null) {
            header = RequestHeader.parse(getPayload());
        }
        return header;
    }

    private short getApiVersion() {
		return rawMsg.getShort(6);
	}

    public byte[] getHeaderBytes() {
        if (headerBytes == null) {
            int tagBufferSize = 0;
            short headerVersion = ApiKeys.forId((int)getApiKey()).requestHeaderVersion(getApiVersion());
            if (headerVersion >= 2) {
                tagBufferSize = 1;
            }
            int headerSize = FIXED_HEADER_LEN + getClientIdLen() + tagBufferSize;
            headerBytes = Arrays.copyOfRange(rawMsg.getBytes(), MSG_SIZE_LEN, MSG_SIZE_LEN + headerSize);
        }
        return headerBytes;
    }

    private int getClientIdLen() {
        RequestHeader hdr = getHeader();
        if (hdr == null) {
            return 0;
        }
        String clientId = hdr.clientId();
        int clientIdLen = clientId != null ? clientId.length() : 0;
        return clientIdLen;
    }
}
