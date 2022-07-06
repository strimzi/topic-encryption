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
 * This class is an in-memory data structure containing all KMS and topic policy
 * definitions. Individual policies and KMS definitions are retrieved by name.
 */
public class BasicPolicyRepo implements PolicyRepository {

    public static final String ALL_TOPICS = "*";

    private List<TopicPolicy> topicPolicies = new ArrayList<>();
    private Map<String, TopicPolicy> policyMap = new HashMap<>();
    private boolean isEncryptAllTopics;

    public BasicPolicyRepo(List<TopicPolicy> policies) {
        if (policies == null) {
            throw new NullPointerException("Null topic policy list.");
        }
        topicPolicies.addAll(policies);
        topicPolicies.stream().forEach(x -> addToMap(x));
        isEncryptAllTopics = isWildcardPolicy(policies);
    }

    public TopicPolicy getTopicPolicy(String topicName) {
        String key;
        if (isEncryptAllTopics) {
            key = ALL_TOPICS;
        } else {
            key = normalize(topicName);
        }
        return policyMap.get(key);
    }

    private void addToMap(TopicPolicy policy) {
        if (policyMap.containsKey(ALL_TOPICS)) {
            throw new IllegalArgumentException(
                    "When a wildcard policy is defined, no other topic policies are permitted.");
        }
        String key = normalize(policy.getTopic());
        if (key == null) {
            throw new NullPointerException("Invalid policy: a topic must be provided.");
        }
        TopicPolicy p = policyMap.get(key);
        if (p != null) {
            throw new IllegalArgumentException(
                    "A policy is already associated with the topic, " + key);
        }
        policyMap.put(key, policy);
    }

    private static String normalize(String key) {
        if (key == null) {
            return null;
        }
        return key.toLowerCase();
    }

    /**
     * Determine whether a list of policies is equivalent to a wild card policy
     * i.e., a single policy for encrypting all topics.
     *
     * @param policies a list of topic policies
     * @return true if the policy list represents a single wildcard policy.
     */
    private static boolean isWildcardPolicy(List<TopicPolicy> policies) {
        // it is a wildcard policy if the policy list consists of a single policy
        // whose topic is the wildcard indicator ('*').
        return policies.size() == 1 && ALL_TOPICS.equals(policies.get(0).getTopic());
    }

}
