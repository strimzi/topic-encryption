/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.ser;

import org.apache.kafka.common.record.MemoryRecordsBuilder;
import org.apache.kafka.common.record.Record;

import io.strimzi.kafka.topicenc.enc.EncData;

public interface EncSerDer {

	byte[] serialize(EncData md) throws EncSerDerException;
	
	void serialize(MemoryRecordsBuilder builder, Record r, EncData md) throws EncSerDerException;
	
	EncData deserialize(byte[] msg) throws EncSerDerException;
}
