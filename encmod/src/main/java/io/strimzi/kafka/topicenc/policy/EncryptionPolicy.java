/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.policy;

import java.util.ArrayList;
import java.util.List;

public class EncryptionPolicy {
    
    public static final String ALL_TOPICS = "*";
	
	List<KmsDefinition> kmsDefinitions = new ArrayList<>(); 
	List<TopicPolicy> topicPolicies = new ArrayList<>();
	
	
	public List<KmsDefinition> getKmsDefinitions() {
		return kmsDefinitions;
	}
	public List<TopicPolicy> getTopicPolicies() {
		return topicPolicies;
	}
}
