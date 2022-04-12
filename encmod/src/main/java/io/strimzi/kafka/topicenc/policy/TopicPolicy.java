/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.policy;

public class TopicPolicy {

	String topic;
	String encMethod;
	String keyReference;
	KmsDefinition kms;
	String credential;
	
	public String getTopic() {
		return topic;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}
	public String getEncMethod() {
		return encMethod;
	}
	public void setEncMethod(String encMethod) {
		this.encMethod = encMethod;
	}
	public String getKeyReference() {
		return keyReference;
	}
	public void setKeyReference(String keyReference) {
		this.keyReference = keyReference;
	}
	public KmsDefinition getKms() {
		return kms;
	}
	public void setKms(KmsDefinition kms) {
		this.kms = kms;
	}
	public String getCredential() {
		return credential;
	}
	public void setCredential(String credential) {
		this.credential = credential;
	}
}
