/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.policy;

/**
 * This is the interface to an implementation of a policy repository containing
 * information on topics to encrypt. Such a repository can take many forms such
 * as a JSON file, a REST service, a database, etc. The encryption module uses
 * this interface to retrieve policy information without being concerned with
 * the implementation details on repository housing policy. A trivial
 * implementation of this interface is TestPolicyRepo used for testing.
 */
public interface PolicyRepository {

    TopicPolicy getTopicPolicy(String topicName);
}
