/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.enc;

import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.SecretKey;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.strimzi.kafka.topicenc.kms.TestKms;
import io.strimzi.kafka.topicenc.ser.AesGcmV1SerDer;
import io.strimzi.kafka.topicenc.ser.EncSerDerException;

public class AesGcmEncTests {
    
    private static final String TEST_MSG = "abcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_-=+[]{}";

    TestKms kms;
    AesGcmEncrypter enc;
    
    @Before
    public void testsInit() throws NoSuchAlgorithmException {
        kms = new TestKms();
        SecretKey key = kms.getKey("test");
        enc = new AesGcmEncrypter(key);
    }

    /**
     * Basic test of encryption, decryption of AES GCM encrypter.
     */
    @Test
    public void basicTestAesGcm() {

        byte[] testMsg = TEST_MSG.getBytes(StandardCharsets.UTF_8);
        
        EncData encData;
        try {
            encData = enc.encrypt(testMsg);
        } catch (Exception e) {
            fail("Error encrypting test message: " + e.toString());
            return;
        }
        try {
            byte[] plaintext = enc.decrypt(encData);
            Assert.assertArrayEquals(testMsg, plaintext);
            
            String plaintextStr = new String(plaintext, "UTF-8");
            Assert.assertEquals(TEST_MSG, plaintextStr);
            
        } catch (Exception e) {
            fail("Error deencrypting test message: " + e.toString());
        }
    }
    
    /**
     * Basic test of serialization, deserialization of encrypted data.
     */
    @Test
    public void basicTestSerDer() {
        byte[] testMsg = TEST_MSG.getBytes(StandardCharsets.UTF_8);
        testSerDer(testMsg);        
    }
    
    /**
     * Tests serialization, deserialization of encrypted data
     * using buffers of random size and data.
     */
    @Test
    public void randomTestSerDer() {
        int numTests = 200;
        
        // tiny
        testRandomSerDer(1, 2, numTests);
        // small
        testRandomSerDer(10, 100, numTests);
        // medium
        testRandomSerDer(500, 2500, numTests);
        // large
        testRandomSerDer(100000, 500000, numTests);
        // extra large
        testRandomSerDer(1000000, 5000000, numTests);
    }
    
    private void testRandomSerDer(int minBufSize, int maxBufSize, int iterations) {
        Random rand = new Random();
        
        for (int i=0; i< iterations; i++) {
            int bufSize = rand.nextInt(maxBufSize - minBufSize) + minBufSize;
            byte[] buf = CryptoUtils.createRandom(bufSize);

            testSerDer(buf);
        }
    }

    private void testSerDer(byte[] testMsg) {
        
        // encrypt test message
        EncData encData;
        try {
            encData = enc.encrypt(testMsg);
        } catch (Exception e) {
            fail("Error encrypting test message: " + e.toString());
            return;
        }
        
        // serialize encrypted message
        AesGcmV1SerDer serder = new AesGcmV1SerDer();
        byte[] serialized;
        try {
            serialized = serder.serialize(encData);
        } catch (EncSerDerException e) {
            fail("Error serializing encrypted test message: " + e.toString());
            return;
        }
        
        // deserialize serialized buf
        EncData deserialized;
        try {
            deserialized = serder.deserialize(serialized);
        } catch (EncSerDerException e) {
            fail("Error deserializing serialized test message: " + e.toString());
            return;
        }
        
        // decrypt deserialized data, assert plaintext equals original message.
        try {
            byte[] decrypted = enc.decrypt(deserialized);
            Assert.assertArrayEquals(testMsg, decrypted);

        } catch (Exception e) {
            fail("Error decrypting test message: " + e.toString());
        }
    }
}
