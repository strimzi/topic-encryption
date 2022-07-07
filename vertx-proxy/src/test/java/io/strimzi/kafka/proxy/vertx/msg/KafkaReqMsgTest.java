package io.strimzi.kafka.proxy.vertx.msg;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.Charset;

import org.apache.kafka.common.message.ProduceRequestData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.DataOutputStreamWritable;
import org.apache.kafka.common.protocol.ObjectSerializationCache;
import org.apache.kafka.common.requests.ProduceRequest;
import org.apache.kafka.common.requests.RequestHeader;
import org.junit.Test;

public class KafkaReqMsgTest {

    @Test
    public void getHeader() {
        RequestHeader expectedRH = new RequestHeader(ApiKeys.PRODUCE, (short) 8, "clientId", 7);
        ProduceRequest pr = new ProduceRequest.Builder(expectedRH.apiVersion(),
                expectedRH.apiVersion(), new ProduceRequestData()).build();
        KafkaReqMsg reqMsg = new KafkaReqMsg(
                AbstractKafkaMsgTest.serializeToBuffer(expectedRH, pr));

        RequestHeader actualRH = reqMsg.getHeader();

        assertEquals(expectedRH, actualRH);
    }

    @Test
    public void getHeaderBytes() {
        short[] requestVersions = new short[] {
                8, // -> uses request header v1
                9, // -> uses request header v2
        };
        String[] clientIds = new String[] { "clientId", null };

        for (int i = 0; i < requestVersions.length; i++) {
            RequestHeader rh = new RequestHeader(ApiKeys.PRODUCE, requestVersions[i], clientIds[i],
                    7);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectSerializationCache cache = new ObjectSerializationCache();
            if (clientIds[i] != null) {
                cache.cacheSerializedValue(clientIds[i],
                        clientIds[i].getBytes(Charset.forName("UTF-8")));
            }
            try (DataOutputStreamWritable dosw = new DataOutputStreamWritable(
                    new DataOutputStream(baos))) {
                rh.data().write(dosw, cache, rh.headerVersion());
            }
            byte[] expectedHeaderBytes = baos.toByteArray();
            ProduceRequest pr = new ProduceRequest.Builder(
                    rh.apiVersion(), rh.apiVersion(), new ProduceRequestData()).build();
            KafkaReqMsg reqMsg = new KafkaReqMsg(AbstractKafkaMsgTest.serializeToBuffer(rh, pr));

            byte[] actualHeaderBytes = reqMsg.getHeaderBytes();

            assertArrayEquals(expectedHeaderBytes, actualHeaderBytes);
        }
    }
}
