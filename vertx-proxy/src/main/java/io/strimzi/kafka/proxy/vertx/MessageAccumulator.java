/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

import io.vertx.core.buffer.Buffer;

/**
 * Receives and appends Kafka message fragments and answers
 * whether the message is complete based on the message length
 * in the first 4 bytes of the message.
 */
public class MessageAccumulator {

    Buffer buffer;
    
    public MessageAccumulator() {
        buffer = Buffer.buffer(0);
    }
    
    public void append(Buffer buffer) {
        LogUtils.hexDump("Msg append", buffer);
        this.buffer.appendBuffer(buffer);
    }
 
    public boolean isComplete() {
        return MsgUtil.isBufferComplete(buffer);
    }
    
    public Buffer getBuffer() {
        return buffer;
    }
    
    public void reset() {
        buffer = Buffer.buffer(0);
    }
    
    public boolean isEmpty() {
        return buffer.length() == 0;
    }
}
