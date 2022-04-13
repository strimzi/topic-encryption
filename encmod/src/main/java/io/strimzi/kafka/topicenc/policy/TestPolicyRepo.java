/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of a policy repository used for testing.
 */
public class TestPolicyRepo implements PolicyRepository {
    

	List<KmsDefinition> kmsDefinitions = new ArrayList<>(); 
	List<TopicPolicy> topicPolicies = new ArrayList<>();
	
	Map<String,TopicPolicy> policyMap = new HashMap<>();
	Map<String,KmsDefinition> kmsMap = new HashMap<>();
	
	public TestPolicyRepo() {
	    TopicPolicy policy = new TopicPolicy();
	    policy.setEncMethod("AesGcmV1");
	    policy.setKeyReference("test");
	    policy.setTopic(EncryptionPolicy.ALL_TOPICS);
	    
	    policyMap.put(EncryptionPolicy.ALL_TOPICS, policy);
	}
	
	@Override
	public TopicPolicy getTopicPolicy(String topicName) {
	    
	    // wildcard has priority:
	    TopicPolicy policy = policyMap.get(EncryptionPolicy.ALL_TOPICS);
	    if (policy != null) {
	        return policy;
	    }
	    return policyMap.get(topicName.toLowerCase());
	}

	@Override
	public KmsDefinition getKmsDefinition(String kmsName) {
		return kmsMap.get(kmsName.toLowerCase());
	}
}
