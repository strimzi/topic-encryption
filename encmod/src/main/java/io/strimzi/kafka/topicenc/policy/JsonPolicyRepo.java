/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonPolicyRepo implements PolicyRepository {

	List<KmsDefinition> kmsDefinitions = new ArrayList<>(); 
	List<TopicPolicy> topicPolicies = new ArrayList<>();
	
	Map<String,TopicPolicy> policyMap = new HashMap<>();
	Map<String,KmsDefinition> kmsMap = new HashMap<>();
	
	public JsonPolicyRepo(String jsonStr) {
		
	}
	
	@Override
	public TopicPolicy getTopicPolicy(String topicName) {
		return policyMap.get(topicName.toLowerCase());
	}

	@Override
	public KmsDefinition getKmsDefinition(String kmsName) {
		return kmsMap.get(kmsName.toLowerCase());
	}
}
