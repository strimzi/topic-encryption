/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/** For test only. This class to be moved into test source path */
public class TestKms implements KeyMgtSystem {
    
    private static String TEST_KEY = "bfUup8fs92bnOHlghWXegCJleHhbnNaf31RZL0d6r/I=";

    SecretKey key;
    
    public TestKms() throws NoSuchAlgorithmException {
        byte[] decodedKey = Base64.getDecoder().decode(TEST_KEY);
        key = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");         
    }
    
    @Override
    public void setCredential(String cred) {
    }

    @Override
    public SecretKey getKey(String keyReference) {
        return key;
    }
}
