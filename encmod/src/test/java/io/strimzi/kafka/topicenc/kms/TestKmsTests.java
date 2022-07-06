/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;

import org.junit.Test;

/**
 * Testing of the Test KMS impl.
 */
public class TestKmsTests {

    @Test
    public void basicTests() {

        KeyMgtSystem kms = new TestKms();

        SecretKey key;
        try {
            key = kms.getKey("anything");

        } catch (URISyntaxException | IOException | InterruptedException | InvalidKeySpecException
                | NoSuchAlgorithmException e) {
            fail("Error retrieving key from test kms: " + e.toString());
            return;
        }
        if (key == null) {
            fail("getKey() did not return a key");
        }
        // TODO compare key with expected value.
    }
}
