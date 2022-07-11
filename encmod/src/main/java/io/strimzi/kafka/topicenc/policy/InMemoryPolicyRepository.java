/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.policy;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class is an in-memory repository of topic policies. Individual policies
 * are retrieved by name.
 */
public class InMemoryPolicyRepository extends AbstractPolicyRepository {

    /**
     * Creates an instance of a policy repository where policies are passed in
     * memory.
     * 
     * @param policies A list of topic policies
     */
    public InMemoryPolicyRepository(List<TopicPolicy> policies) {
        Objects.requireNonNull(policies, "Topic policy list must be non-null.");
        isEncryptAllTopics = isWildcardPolicy(policies);
        policyMap = policies.stream()
                .collect(Collectors.toMap(InMemoryPolicyRepository::key, Function.identity()));
    }
}
