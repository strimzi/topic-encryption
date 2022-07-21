/*
 * Copyright Strimzi authors. License: Apache License 2.0 (see the file LICENSE or
 * http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.policy;

import io.strimzi.kafka.topicenc.kms.KeyMgtSystem;
import io.strimzi.kafka.topicenc.kms.KmsDefinition;
import io.strimzi.kafka.topicenc.kms.KmsFactory;

/**
 * An trivial implementation of a policy repository used only for testing. All
 * topics will be encrypted with the key from TestKms.
 */
public class TestPolicyRepo implements PolicyRepository {

    TopicPolicy policy;
    KmsDefinition kmsDef;

    public TestPolicyRepo() {
        KmsDefinition kmsDef = new KmsDefinition()
                .setType("test")
                .setName("testrepo.test");

        KeyMgtSystem kms = KmsFactory.createKms(kmsDef);
        policy = new TopicPolicy()
                .setEncMethod("AesGcmV1")
                .setKeyReference("test")
                .setTopic(TopicPolicy.ALL_TOPICS)
                .setKms(kms);
    }

    @Override
    public TopicPolicy getTopicPolicy(String topicName) {
        return policy;
    }
}
