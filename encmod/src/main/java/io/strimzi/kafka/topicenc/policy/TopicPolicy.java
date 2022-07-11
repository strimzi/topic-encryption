/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.policy;

import io.strimzi.kafka.topicenc.common.Strings;
import io.strimzi.kafka.topicenc.kms.KeyMgtSystem;

/**
 * TopicPolicy contains the information for the Encryption Module to: 1)
 * determine whether a topic is to be encrypted 2) details about the encryption
 * method 3) key management system instance for retrieving keys
 */
public class TopicPolicy {

    /**
     * A reserved topic name indicating that all topics are to be encrypted using
     * one policy. This is not a regular expressions. Regex is not supported for
     * specifying topic names.
     */
    public static final String ALL_TOPICS = "*";

    /**
     * The name of the topic to encrypt. Required.
     */
    private String topic;

    /**
     * A string indicating the encryption method. Optional. Currently only one
     * method is supported: AES-GCM.
     */
    private String encMethod;

    /**
     * The name or ID of the key to use when encrypting this topic. Typically this
     * is a key identifier in a key management system.
     */
    private String keyReference;

    /**
     * Key management system instance used in retrieving keys.
     */
    private KeyMgtSystem kms;

    /**
     * The name of the key management system to use. This is a configuration
     * property which is ultimately resolved to the KeyMgtSystem instance,
     * TopicPolicy.kms, used to retrieved keys.
     */
    private String kmsName;

    /**
     * A credential, such as a token, used to access the key management system.
     */
    private String credential;

    /**
     * Returns the topic name to which this policy applies.
     * 
     * @return the topic name
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Set the topic name to which this policy applies.
     * 
     * @param topic the topic name
     * @return this instance
     */
    public TopicPolicy setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    /**
     * Returns the optional encryption method in use. Can be null is was not
     * specified.
     * 
     * @return a string indicating the encryption method
     */
    public String getEncMethod() {
        return encMethod;
    }

    /**
     * Set the encryption method to be applied for this topic.
     * 
     * @param topic the encryption method
     * @return this instance
     */
    public TopicPolicy setEncMethod(String encMethod) {
        this.encMethod = encMethod;
        return this;
    }

    /**
     * Return the key reference used to identify the key within the key management
     * system to be used for this topic.
     * 
     * @return the configured key reference.
     */
    public String getKeyReference() {
        return keyReference;
    }

    /**
     * Set the reference of the key to be used for this topic.
     * 
     * @param topic the key reference
     * @return this instance
     */
    public TopicPolicy setKeyReference(String keyReference) {
        this.keyReference = keyReference;
        return this;
    }

    /**
     * Returns the key management system instance used to retrieve the key for this
     * topic.
     * 
     * @return a KeyMgtSystem instance
     */
    public KeyMgtSystem getKms() {
        return kms;
    }

    /**
     * Set the key management instance to be used for this topic.
     * 
     * @param topic the kms instance
     * @return this instance
     */
    public TopicPolicy setKms(KeyMgtSystem kms) {
        this.kms = kms;
        return this;
    }

    /**
     * Return the name of the key management system definition used for this topic.
     * 
     * @return the kms configuration name
     */
    public String getKmsName() {
        return kmsName;
    }

    /**
     * Set the name identifying the key management system definition in the
     * configuration to be used for this topic.
     * 
     * @param topic the name of the kms definition in the configuration.
     * @return this instance
     */
    public TopicPolicy setKmsName(String kmsName) {
        this.kmsName = kmsName;
        return this;
    }

    /**
     * Return the credential used in accessing the key management system
     * 
     * @return the credential
     */
    public String getCredential() {
        return credential;
    }

    /**
     * Set the credential, such as a token, to be used when accessing the key
     * management system.
     * 
     * @param topic the credential
     * @return this instance
     */
    public TopicPolicy setCredential(String credential) {
        this.credential = credential;
        return this;
    }

    /**
     * Validate this policy. Asserts that all required properties are present. If
     * the policy is not valid, an IllegalArgumentException exception is thrown
     * 
     * @return this instance when the policy is valid.
     */
    public TopicPolicy validate() {
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
        return this;
    }

    /**
     * Indicates whether this topic policy is a "wildcard" policy, that is, it
     * applies to all topics.
     * 
     * @return true if the topic's policy name indicates all topics (i.e., is the
     *         wildcard), otherwise returns false.
     */
    public boolean isWildcardPolicy() {
        return isWildcard(this);
    }

    /**
     * A static method indicating whether the provided policy is a wildcard policy.
     * 
     * @param policy the policy to assess
     * @return true if the topic's policy name indicates all topics (i.e., is the
     *         wildcard), otherwise returns false.
     */
    public static boolean isWildcard(TopicPolicy policy) {
        return TopicPolicy.ALL_TOPICS.equals(policy.getTopic());
    }
}
