/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx.msg;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

import org.apache.kafka.common.requests.AbstractRequest;
import org.apache.kafka.common.requests.FetchResponse;
import org.apache.kafka.common.requests.RequestHeader;
import org.apache.kafka.common.requests.RequestUtils;
import org.apache.kafka.common.requests.ResponseHeader;

import io.vertx.core.buffer.Buffer;

/**
 * Utility methods for processing Kafka messages.
 */
public class MsgUtil {

    /**
     * Given a Kafka request in form of a vert.x Buffer, return the Apikey.
     * This intended as a lightweight way of identifying request types without
     * instantiating/deserializing the bytes into a full request instance.
     * 
     * @param buffer
     * @return -1 if there is something wrong with the input.
     */
    public static short getApiKey(Buffer buffer) {
    	if (Objects.isNull(buffer) || buffer.length() < 6) {
    		return -1;
    	}
    	ByteBuffer bb = ByteBuffer.allocate(2);
    	bb.order(ByteOrder.BIG_ENDIAN);
    	bb.put(buffer.getByte(4));
    	bb.put(buffer.getByte(5));
    	return bb.getShort(0);
    }

    /**
     * Given a Kafka response in form of a vert.x Buffer, return the correlation ID.
     * This intended as a lightweight way of identifying a response without
     * instantiating/deserializing the bytes into a full response instance.
     *
     * @param buffer
     * @return -1 if there is something wrong with the input.
     */
    public static int getRspCorrId(Buffer buffer) {
        if (Objects.isNull(buffer) || buffer.length() < 8) {
            return -1;
        }
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(buffer.getByte(4));
        bb.put(buffer.getByte(5));
        bb.put(buffer.getByte(6));
        bb.put(buffer.getByte(7));
        return bb.getInt(0);
    }

    public static int getReqCorrId(Buffer buffer) {
        if (Objects.isNull(buffer) || buffer.length() < 12) {
            return -1;
        }
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(buffer.getByte(8));
        bb.put(buffer.getByte(9));
        bb.put(buffer.getByte(10));
        bb.put(buffer.getByte(11));
        return bb.getInt(0);
    }

    public static int getMsgLen(Buffer buffer) {
        if (Objects.isNull(buffer) || buffer.length() < 4) {
            return -1;
        }
        return buffer.getInt(0);
    }

    /**
     * Serialize a request into a Kafka send buffer
     *
     * @param header
     * @param req
     * @return
     */
    public static Buffer toSendBuffer(byte[] header, AbstractRequest req) {
    	ByteBuffer serializedReq = req.serialize();
    	ByteBuffer msgLenBuf = ByteBuffer.allocate(Integer.BYTES);
    	msgLenBuf.putInt(header.length + serializedReq.array().length);
    
    	Buffer sendBuffer = Buffer.buffer(msgLenBuf.array());
    	sendBuffer.appendBytes(header);
    	sendBuffer.appendBytes(serializedReq.array());
    	return sendBuffer;
    }

    /**
     * Serialize a request into a Kafka send buffer
     *
     * @param header
     * @param req
     * @return
     */
    public static Buffer toSendBuffer(FetchResponse<?> fetchRsp, RequestHeader reqHeader) {
        ByteBuffer serializedRsp = serialize(fetchRsp, reqHeader);
        byte[] rspBytes = serializedRsp.array();
        int bufLen = Integer.BYTES + rspBytes.length;
        ByteBuffer bufTmp = ByteBuffer.allocate(bufLen);
        bufTmp.putInt(rspBytes.length);
        bufTmp.put(rspBytes);
        return Buffer.buffer(bufTmp.array());
    }
    
    /**
     * Unlike requests, the serialize() method in the base AbstractResponse
     * class is not public so we cannot call it. Therefore we have to do
     * the work here ourselves.
     * 
     * @param fetchRsp
     * @return
     */
    private static ByteBuffer serialize(FetchResponse<?> fetchRsp, RequestHeader reqHeader) {
        ResponseHeader rspHeader = reqHeader.toResponseHeader();
        //System.out.println("****** req: " + reqHeader.apiVersion() + " rsp: " + rspHeader.headerVersion());
        return RequestUtils.serialize(rspHeader.data(), rspHeader.headerVersion(), fetchRsp.data(), reqHeader.apiVersion());
    }

    public static boolean isBufferComplete(Buffer buffer) {
        if (buffer.length() < 4) {
            return false;
        }
        int msgLen = buffer.getInt(0);
        return isBufferComplete(buffer, msgLen);
    }

    public static boolean isBufferComplete(Buffer buffer, int msgLen) {
        return msgLen == buffer.length() - 4;
    }
}
