/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.enc;

/**
 * As the name suggests, an Encrypter/Decrypter is a component which encrypts and decrypts messages.
 * With this interface, implementers can develop a variety of encryption functions.
 */
public interface EncrypterDecrypter {
	
	/**
	 * Encrypt, internally generating a nonce/IV.
	 * @param plaintext
	 * @return
	 * @throws Exception
	 */
	EncData encrypt(byte[] plaintext) throws Exception;
	
	EncData encrypt(byte[] plaintext, byte[] iv) throws Exception; 
	
	byte[] decrypt(EncData encMetadata) throws Exception; 
}
