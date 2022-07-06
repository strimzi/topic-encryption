/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.policy;

import io.strimzi.kafka.topicenc.common.Strings;
import io.strimzi.kafka.topicenc.kms.KeyMgtSystem;

public class TopicPolicy {

    private String topic;
    private String encMethod;
    private String keyReference;
    private KeyMgtSystem kms;
    private String kmsName;
    private String credential;

    public String getTopic() {
        return topic;
    }

    public TopicPolicy setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public String getEncMethod() {
        return encMethod;
    }

    public TopicPolicy setEncMethod(String encMethod) {
        this.encMethod = encMethod;
        return this;
    }

    public String getKeyReference() {
        return keyReference;
    }

    public TopicPolicy setKeyReference(String keyReference) {
        this.keyReference = keyReference;
        return this;
    }

    public KeyMgtSystem getKms() {
        return kms;
    }

    public TopicPolicy setKms(KeyMgtSystem kms) {
        this.kms = kms;
        return this;
    }

    public String getKmsName() {
        return kmsName;
    }

    public TopicPolicy setKmsName(String kmsName) {
        this.kmsName = kmsName;
        return this;
    }

    public String getCredential() {
        return credential;
    }

    public TopicPolicy setCredential(String credential) {
        this.credential = credential;
        return this;
    }

    public void validate() {
        if (Strings.isNullOrEmpty(getTopic())) {
            throw new IllegalArgumentException("Topic name missing from policy definition");
        }
        if (Strings.isNullOrEmpty(getKmsName())) {
            String msg = String.format(
                    "Policy for topic %s does not contain required KMS reference.",
                    getTopic());
            throw new IllegalArgumentException(msg);
        }
        if (Strings.isNullOrEmpty(getKeyReference())) {
            String msg = String.format(
                    "Policy for topic %s does not contain a key reference.",
                    getTopic());
            throw new IllegalArgumentException(msg);
        }
    }
}
