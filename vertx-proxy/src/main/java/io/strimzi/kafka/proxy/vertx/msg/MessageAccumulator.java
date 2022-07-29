/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx.msg;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.strimzi.kafka.topicenc.common.LogUtils;
import io.vertx.core.buffer.Buffer;

/**
 * Receives and appends Kafka message fragments and answers
 * whether the message is complete based on the message length
 * in the first 4 bytes of the message.
 */
public class MessageAccumulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageAccumulator.class);

    // SIZE_LEN is the size (in bytes) of the field used to describe the length of the
    // proceeding Kafka request or response data.
    // See: https://kafka.apache.org/protocol.html#protocol_common
    private static final int SIZE_LEN = 4;

    private Buffer buffer;

    public MessageAccumulator() {
        buffer = Buffer.buffer(0);
    }

    public void append(Buffer buffer) {
        if (LOGGER.isDebugEnabled()) {
            LogUtils.hexDump("Msg append", buffer.getBytes());
        }
        this.buffer.appendBuffer(buffer);
    }

    /**
     * @return a list containing the complete Kafka protocol messages accumulated.
     *         These are removed from the accumulator.
     */
    public List<Buffer> take() {
        int pos = 0;
        int remaining = buffer.length();
        ArrayList<Buffer> result = new ArrayList<>();
        while(remaining > 0) {
            if (remaining < SIZE_LEN) {
                break;
            }

            int nextMsgLen = buffer.getInt(pos) + SIZE_LEN;
            if (nextMsgLen > remaining) {
                break;
            }

            result.add(buffer.slice(pos, pos+nextMsgLen));

            remaining -= nextMsgLen;
            pos += nextMsgLen;
        }

        if (pos == buffer.length()) {
            buffer = Buffer.buffer(0);
        } else {
            buffer = buffer.getBuffer(pos, buffer.length());
        }

        return result;
    }
}
