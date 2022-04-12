/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.ser;

public class EncSerDerException extends Exception {

	private static final long serialVersionUID = 8654177305331008275L;

	public EncSerDerException(String msg) {
		super(msg);
	}
}
