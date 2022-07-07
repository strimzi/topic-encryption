/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx.msg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.kafka.common.protocol.ApiKeys;
import org.junit.Assert;
import org.junit.Test;

import io.strimzi.kafka.proxy.vertx.TestDataFileUtil;
import io.strimzi.kafka.proxy.vertx.util.LogUtils;
import io.vertx.core.buffer.Buffer;

public class MsgUtilTests {

    @Test
    public void testMsgUtil() throws FileNotFoundException, IOException, URISyntaxException {

        // produce request
        File testDataFile = new File("src/test/resources/produce_request.hex");
        // File testDataFile = FileUtils.getFileFromClasspath(this,
        // "produce_request.hex");
        byte[] prodReq = TestDataFileUtil.hexToBin(testDataFile);
        LogUtils.hexDump("PRODUCE request", prodReq);

        Buffer buf = Buffer.buffer(prodReq);
        short apikey = MsgUtil.getApiKey(buf);
        Assert.assertEquals(ApiKeys.PRODUCE.id, apikey);

        int expectedMsgLen = buf.length() - 4;
        int msgLen = MsgUtil.getMsgLen(buf);
        Assert.assertEquals(msgLen, expectedMsgLen);

        boolean isCompleted = MsgUtil.isBufferComplete(buf);
        Assert.assertEquals(true, isCompleted);

        isCompleted = MsgUtil.isBufferComplete(buf, expectedMsgLen);
        Assert.assertEquals(true, isCompleted);

        // fetch response
        File rspDataFile = new File("src/test/resources/fetch_response.hex");
        // File rspDataFile = FileUtils.getFileFromClasspath(this,
        // "fetch_response.hex");
        byte[] fetchRsp = TestDataFileUtil.hexToBin(rspDataFile);

        var rspBuf = Buffer.buffer(fetchRsp);
        int corrId = MsgUtil.getRspCorrId(rspBuf);
        Assert.assertEquals("Correlation ID", (int) 0x4B, corrId);
    }
}
