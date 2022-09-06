/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms.keyprotect;

import io.strimzi.kafka.topicenc.kms.KeyMgtSystem;
import io.strimzi.kafka.topicenc.kms.KmsDefinition;
import io.strimzi.kafka.topicenc.kms.KmsException;
import io.strimzi.kafka.topicenc.kms.KmsFactory;

/**
 * A KmsFactory, made available over the Java SPI, for creating instance of
 * KeyProtectKms.
 */
public class KeyProtectKmsFactory implements KmsFactory {

    @Override
    public KeyMgtSystem createKms(KmsDefinition kmsDef) throws KmsException {
        return new KeyProtectKms(kmsDef);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }
}
