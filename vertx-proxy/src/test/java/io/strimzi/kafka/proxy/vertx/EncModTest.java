/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
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
import org.junit.Test;

import io.strimzi.kafka.proxy.vertx.msg.KafkaRspMsg;
import io.strimzi.kafka.proxy.vertx.msg.MsgUtil;
import io.strimzi.kafka.proxy.vertx.util.LogUtils;
import io.strimzi.kafka.topicenc.EncryptionModule;
import io.strimzi.kafka.topicenc.kms.KmsException;
import io.strimzi.kafka.topicenc.policy.PolicyRepository;
import io.strimzi.kafka.topicenc.policy.TestPolicyRepository;
import io.strimzi.kafka.topicenc.ser.EncSerDerException;
import io.vertx.core.buffer.Buffer;

/**
 * Unit tests for the EncryptionModule.
 */
public class EncModTest {

    EncryptionModule encMod;
    Config dummyConfig;

    @Test
    public void testEncryption()
            throws IOException, EncSerDerException, GeneralSecurityException, KmsException,
            URISyntaxException {
        PolicyRepository policyRepo = new TestPolicyRepository();
        encMod = new EncryptionModule(policyRepo);
        dummyConfig = createDummyConfig();
        var handler = new MessageHandler(encMod, dummyConfig);

        File reqDataFile = new File("src/test/resources/produce_request.hex");
        byte[] prodReq = TestDataFileUtil.hexToBin(reqDataFile);

        var reqBuf = Buffer.buffer(prodReq);

        Buffer sendBuf = handler.processProduceRequest(reqBuf);
        boolean equal = Arrays.equals(prodReq, sendBuf.getBytes());
        Assert.assertFalse("Message was not encrypted", equal);
    }

    @Test
    public void testDecryption()
            throws IOException, EncSerDerException, GeneralSecurityException, KmsException,
            URISyntaxException {

        File fetchRsp = new File("src/test/resources/fetch_response.hex");
        testDecryption(fetchRsp);

        File multiFetchRsp = new File("src/test/resources/fetch_multi_response.hex");
        testDecryption(multiFetchRsp);
    }

    private void testDecryption(File rspMsgFile)
            throws IOException, EncSerDerException, GeneralSecurityException, KmsException {
        byte[] fetchRsp = TestDataFileUtil.hexToBin(rspMsgFile);
        LogUtils.hexDump("FETCH response encrypted", fetchRsp);

        // set up so we can call the handler
        encMod = new EncryptionModule(new TestPolicyRepository());
        dummyConfig = this.createDummyConfig();
        var handler = new MessageHandler(encMod, dummyConfig);
        var rspBuf = Buffer.buffer(fetchRsp);
        int corrId = MsgUtil.getRspCorrId(rspBuf);
        var reqHeader = new RequestHeader(ApiKeys.FETCH, (short) 12, "console-producer", corrId);

        // decrypt:
        Buffer fetchRspBuf = handler.processFetchResponse(rspBuf, reqHeader);

        LogUtils.hexDump("FETCH response decrypted", fetchRspBuf);

        // instantiate the decrypted fetch response
        KafkaRspMsg rsp = new KafkaRspMsg(fetchRspBuf, reqHeader.apiVersion());
        FetchResponse<?> fetch = (FetchResponse<?>) AbstractResponse.parseResponse(rsp.getPayload(),
                reqHeader);

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
        return new Config();
    }
}

