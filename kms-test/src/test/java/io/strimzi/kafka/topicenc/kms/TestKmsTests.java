/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms;

import static org.junit.Assert.fail;

import javax.crypto.SecretKey;

import org.junit.Test;

import io.strimzi.kafka.topicenc.kms.test.TestKms;

/**
 * Testing of the Test KMS impl.
 */
public class TestKmsTests {

    @Test
    public void basicTests() {

        KeyMgtSystem kms = new TestKms(new KmsDefinition());

        SecretKey key;
        try {
            key = kms.getKey("test_key_id");

        } catch (KmsException e) {
            fail("Error retrieving key from test kms: " + e.toString());
            return;
        }
        if (key == null) {
            fail("getKey() did not return a key");
        }
        // TODO compare key with expected value.
    }
}
