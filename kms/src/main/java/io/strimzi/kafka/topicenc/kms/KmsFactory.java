/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms;

/**
 * Interface to a KMS factory.
 */
public interface KmsFactory {

    /**
     * Every factory names itself. This name must be unique and identifies the type
     * of KMS the factory produces. For example, a factory for producing instances
     * of Vault KMS may be named "vault".
     * 
     * @return
     */
    String getName();

    /**
     * Given a valid KmsDefinition, instantiate and return the KeyMgtSystem
     * implementation described by the KmsDefinition.
     * 
     * @param kmsDef
     * @return
     * @throws KmsException
     */
    KeyMgtSystem createKms(KmsDefinition kmsDef) throws KmsException;
}
