package io.strimzi.kafka.proxy.vertx.msg;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.HashMap;

import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.requests.AbstractRequest;
import org.apache.kafka.common.requests.FetchRequest;
import org.apache.kafka.common.requests.RequestHeader;
import org.apache.kafka.common.requests.RequestUtils;
import org.junit.Test;

import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;

public class AbstractKafkaMsgTest {

    @Test
    public void getRawMsg() {
        // getRawMsg doesn't validate the data is a valid Kafka message, so just
        // use any sequence of bytes in this test.
        byte[] expectedRawMsg = new byte[] { 0x01, 0x02, 0x03 };
        AbstractKafkaMsg akm = new AbstractKafkaMsg(Buffer.buffer(expectedRawMsg));

        byte[] actualRawMsg = akm.getRawMsg().getBytes();

        assertArrayEquals(expectedRawMsg, actualRawMsg);
    }

    @Test
    public void getPayload() {
        // getPayload considers anything past the first 4 bytes as the payload
        // so this isn't an accurate rendering of a Kafka protocol message
        byte[] expectedPayload = new byte[] {
                0x01, 0x02, 0x03, 0x04
        };
        Buffer msg = Buffer.buffer().appendInt(expectedPayload.length).appendBytes(expectedPayload);
        AbstractKafkaMsg akm = new AbstractKafkaMsg(msg);

        ByteBuffer actualPayloadBB = akm.getPayload();
        byte[] actualPayload = new byte[actualPayloadBB.remaining()];
        actualPayloadBB.get(actualPayload);

        assertArrayEquals(expectedPayload, actualPayload);
    }

    // Helper for serializing a RequestHeader / Request to a Buffer.
    static Buffer serializeToBuffer(RequestHeader rh, AbstractRequest req) {
        ByteBuffer reqBB = RequestUtils.serialize(rh.data(), rh.apiVersion(), req.data(),
                rh.apiVersion());
        return Buffer.buffer()
                .appendInt(reqBB.remaining())
                .appendBuffer(Buffer.buffer(Unpooled.copiedBuffer(reqBB)));
    }

    @Test
    public void getApiKey() {
        // Generate more realistic test data to make sure that the API key
        // value is pulled from the correct offset.
        RequestHeader rh = new RequestHeader(ApiKeys.FETCH, (short) 12, "clientId", 1);
        FetchRequest fr = new FetchRequest.Builder(
                rh.apiVersion(), rh.apiVersion(), 0, 500, 1, new HashMap<>()).build();

        AbstractKafkaMsg akm = new AbstractKafkaMsg(serializeToBuffer(rh, fr));

        short actualApiKey = akm.getApiKey();

        assertEquals(fr.apiKey().id, actualApiKey);
    }
}
