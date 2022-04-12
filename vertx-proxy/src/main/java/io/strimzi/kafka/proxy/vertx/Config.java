/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

import io.strimzi.kafka.topicenc.policy.PolicyRepository;

public class Config {
	
	String kafkaHostname;
	int kafkaPort;
	PolicyRepository policyRepo;
	
	public Config (String kafkaHostname, int kafkaPort, PolicyRepository policyRepo) {
		this.kafkaHostname = kafkaHostname;
		this.kafkaPort = kafkaPort;
		this.policyRepo = policyRepo;
	}

	public String kafkaHostname() {
		return kafkaHostname;
	}

	public int kafkaPort() {
		return kafkaPort;
	}

	public PolicyRepository policyRepo() {
		return policyRepo;
	}
}
