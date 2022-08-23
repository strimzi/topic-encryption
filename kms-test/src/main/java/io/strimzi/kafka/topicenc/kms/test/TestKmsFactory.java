/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms.test;

import io.strimzi.kafka.topicenc.kms.KeyMgtSystem;
import io.strimzi.kafka.topicenc.kms.KmsDefinition;
import io.strimzi.kafka.topicenc.kms.KmsException;
import io.strimzi.kafka.topicenc.kms.KmsFactory;

/**
 * A KmsFactory, made available over the Java SPI, for creating instances of
 * TestKms.
 */
public class TestKmsFactory implements KmsFactory {

    private static final String NAME = "test";

    @Override
    public KeyMgtSystem createKms(KmsDefinition kmsDef) throws KmsException {
        return new TestKms(kmsDef);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
