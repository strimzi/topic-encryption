/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.policy;

import static io.strimzi.kafka.topicenc.common.Strings.createKey;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.strimzi.kafka.topicenc.common.Strings;

/**
 * Base functionality for Topic Policy repositories.
 */
public abstract class AbstractPolicyRepository implements PolicyRepository {

    protected Map<String, TopicPolicy> policyMap = new HashMap<>();
    protected boolean isEncryptAllTopics;

    /**
     * Retrieve a topic policy by topic name.
     */
    @Override
    public TopicPolicy getTopicPolicy(String topicName) {
        String key;
        if (isEncryptAllTopics) {
            key = TopicPolicy.ALL_TOPICS;
        } else {
            key = Strings.createKey(topicName, Locale.getDefault());
        }
        return policyMap.get(key);
    }

    protected static String key(TopicPolicy policy) {
        return createKey(policy.getTopic(), Locale.getDefault());
    }

    /**
     * Determine whether a list of policies is comprises a wild card policy i.e., a
     * single policy to encrypt all topics.
     *
     * @param policies a list of topic policies
     * @return true if the policy list represents a single wildcard policy.
     */
    protected static boolean isWildcardPolicy(List<TopicPolicy> policies) {
        // it is a wildcard policy if the policy list consists of a single policy
        // whose topic is the wildcard indicator ('*').
        if (policies.size() == 1 && policies.get(0).isWildcardPolicy()) {
            return true;
        }
        // There is more than on topic policy. Check if one is wildcard, if so throw
        // exception. Can't mix wildcard with specific topic policies.
        policies.stream()
                .forEach(policy -> {
                    if (policy.isWildcardPolicy()) {
                        throw new IllegalArgumentException(
                                "When a wildcard policy is defined, it must be the only policy.");
                    }
                });
        return false;
    }
}
