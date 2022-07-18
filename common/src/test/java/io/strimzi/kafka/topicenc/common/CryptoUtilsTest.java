package io.strimzi.kafka.topicenc.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

import org.junit.Test;

public class CryptoUtilsTest {

    /**
     * Simply exercise the random number generation with different sizes.
     */
    @Test
    public void testRandomNumGen() {
        testRng(1);
        testRng(10);
        testRng(100);
        testRng(1000);
    }

    /**
     * Exercise base 64 encoding, decoding by round-tripping: AES key -> base 64 ->
     * AES key.
     */
    @Test
    public void testEncoding() {
        SecretKey key;
        try {
            key = getTestKey();
        } catch (Exception e) {
            fail("Error retrieving test key: " + e.toString());
            return;
        }
        String key64 = CryptoUtils.base64Encode(key);
        SecretKey keyCopy = CryptoUtils.base64Decode(key64);

        assertEquals("keys are not equal.", key, keyCopy);
    }

    private SecretKey getTestKey() throws NoSuchAlgorithmException {
        return CryptoUtils.generateAesKey(256);
    }

    private void testRng(int bufLen) {
        byte[] random = CryptoUtils.createRandom(bufLen);
        assertEquals("createRandom returned buffer of unexpected length", random.length, bufLen);
    }
}
