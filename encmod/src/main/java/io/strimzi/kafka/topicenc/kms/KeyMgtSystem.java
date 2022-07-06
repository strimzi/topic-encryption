/*
 * Copyright Strimzi authors. License: Apache License 2.0 (see the file LICENSE or
 * http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;

public interface KeyMgtSystem {

    SecretKey getKey(String keyReference) throws URISyntaxException, IOException,
            InterruptedException, InvalidKeySpecException, NoSuchAlgorithmException;
}
