/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms;

import javax.crypto.SecretKey;

/**
 * KeyMgtSystem is an interface to an class providing access to a key management
 * system such as Vault, IBM Key Protect, etc.
 */
public interface KeyMgtSystem {

    /**
     * Retrieve the key identified by the provided key reference.
     * 
     * @param keyReference an identifier in the respective KMS which identifies a key.
     * @return
     * @throws KmsException
     */
    SecretKey getKey(String keyReference) throws KmsException;
}
