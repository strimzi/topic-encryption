/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import org.apache.kafka.common.message.FetchResponseData;
import org.apache.kafka.common.message.FetchResponseData.FetchableTopicResponse;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.requests.AbstractResponse;
import org.apache.kafka.common.requests.FetchResponse;
import org.apache.kafka.common.requests.RequestHeader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.strimzi.kafka.topicenc.EncryptionModule;
import io.strimzi.kafka.topicenc.kms.TestKms;
import io.strimzi.kafka.topicenc.policy.TestPolicyRepo;
import io.strimzi.kafka.topicenc.ser.EncSerDerException;
import io.vertx.core.buffer.Buffer;

public class EncModTests {

    EncryptionModule encMod;
    Config dummyConfig;
    
    @Before
    public void testsInit() throws NoSuchAlgorithmException {
    }

    @Test
    public void testEncryption() throws IOException, NoSuchAlgorithmException {
        encMod = new EncryptionModule(new TestPolicyRepo(), new TestKms());
        dummyConfig = new Config(0, "localhost",null);
        var handler = new MessageHandler(encMod, dummyConfig);

        File reqDataFile = new File("src/test/resources/produce_request.hex");
        byte[] prodReq = TestDataFileUtil.hexToBin(reqDataFile);
        
        var reqBuf = Buffer.buffer(prodReq);

        try {
            Buffer sendBuf = handler.processProduceRequest(reqBuf);
            boolean equal = Arrays.equals(prodReq, sendBuf.getBytes());
            Assert.assertFalse("Message was not encrypted", equal);
            
        } catch (EncSerDerException | GeneralSecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Test
    public void testDecryption() throws IOException, NoSuchAlgorithmException {
        testDecryption(new File("src/test/resources/fetch_response.hex"));
        testDecryption(new File("src/test/resources/fetch_multi_response.hex"));
    }
    
    private void testDecryption(File rspMsgFile) throws IOException, NoSuchAlgorithmException {
        byte[] fetchRsp = TestDataFileUtil.hexToBin(rspMsgFile);
        LogUtils.hexDump("FETCH response encrypted", fetchRsp);

        // set up so we can call the handler
        encMod = new EncryptionModule(new TestPolicyRepo(), new TestKms());
        dummyConfig = this.createDummyConfig();
        var handler = new MessageHandler(encMod, dummyConfig);
        var rspBuf = Buffer.buffer(fetchRsp);
        int corrId = MsgUtil.getRspCorrId(rspBuf);
        var reqHeader = new RequestHeader(ApiKeys.FETCH, (short) 12, "console-producer", corrId);
        
        // decrypt:
        Buffer fetchRspBuf;
        try {
            fetchRspBuf = handler.processFetchResponse(rspBuf, reqHeader);
        } catch (EncSerDerException | GeneralSecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        LogUtils.hexDump("FETCH response decrypted", fetchRspBuf);
        
        // instantiate the decrypted fetch response
        KafkaRspMsg rsp = new KafkaRspMsg(fetchRspBuf, reqHeader.apiVersion());
        FetchResponse<?> fetch = (FetchResponse<?>) AbstractResponse.parseResponse(rsp.getPayload(), reqHeader);
        
        FetchResponseData data = fetch.data();
        navigate(data);
    }
    
    private void navigate(FetchResponseData data) {
        // navigate into the decrypted response. 
        // This tests the integrity of the decrypted response.
        List<FetchableTopicResponse> responses = data.responses();
        for (FetchableTopicResponse topicRsp : responses) {
            topicRsp.partitionResponses().forEach(pd -> {
                MemoryRecords recs = (MemoryRecords) pd.recordSet();
                recs.records().forEach(r -> {
                    if (r.hasValue()) {
                        byte[] recordData = new byte[r.valueSize()];
                        r.value().get(recordData);
                        LogUtils.hexDump("Record data", recordData);
                    }
                });
            });
        }
    }
    
    private Config createDummyConfig() {
        return new Config(0, "localhost",null);
    }
}
