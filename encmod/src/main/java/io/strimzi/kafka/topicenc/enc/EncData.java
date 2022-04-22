/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.enc;

/**
 * Encrypted data. Contains the ciphertext and IV.
 * This class covers symmetric block ciphers such as AES.
 * Other types of encrypted data classes are possible in the future.
 */
public class EncData {
	private final byte[] iv;         
	private final byte[] ciphertext;
	
	public EncData(byte[] iv, byte[] ciphertext) {
		this.iv = iv;
		this.ciphertext = ciphertext; 
	}

	public byte[] getIv() {
		return iv;
	}

	public byte[] getCiphertext() {
		return ciphertext;
	}
}
