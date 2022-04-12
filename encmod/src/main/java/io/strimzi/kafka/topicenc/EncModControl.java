/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc;

public interface EncModControl {

	//void disableTopic(String topic);
	
	//void enableTopic(String topic);
	
	//void clearTopic(String topic);
	
	void purgeKey(String keyref);
}
