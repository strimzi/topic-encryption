/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms;

/**
 * An exception thrown by a KMS implementation, either during instantiation or 
 *  operation.
 */
public class KmsException extends Exception {

    private static final long serialVersionUID = -1957947377477505516L;

    public KmsException(String msg) {
        super(msg);
    }

    public KmsException(String msg, Throwable e) {
        super(msg, e);
    }
}
