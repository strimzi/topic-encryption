/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc;

/**
 * This defines the interface to the Encryption Module to functions
 * controlling its internal state. So, for example, can an implementation
 * receiving events from a key management system (KMS), notify the module
 * to purge a key because it has expired. If we consider the
 * Encryption Module's encrypt() and decrypt() functions to comprise
 * the data path, this interface describes its control path. 
 * 
 * Currently this interface is a placeholder but will be continually 
 * extended as the implementation matures.
 */
public interface EncModControl {

    /**
     * Purge the key, indicated by the keyRef argument, from any 
     * internal state such that the key in question is now longer used.
     * This supports key revokation.
     * 
     * @param keyref A key reference, understood by the Encryption Module and its KMS, identifying the key to purge.
     */
	void purgeKey(String keyref);
}
