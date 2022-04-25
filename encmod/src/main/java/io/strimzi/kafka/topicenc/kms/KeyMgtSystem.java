/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms;

import javax.crypto.SecretKey;

public interface KeyMgtSystem {

    void setCredential(String cred);
 
    SecretKey getKey(String keyReference);
}
