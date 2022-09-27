/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

/**
 * Testing of the KMS core components.
 */
public class KmsTests {

    @Test
    public void basicTests() throws KmsException {

        KmsFactoryManager mgr = KmsFactoryManager.getInstance();
        assertNotNull("Null KmsManager", mgr);

        URI uri;
        try {
            uri = new URI("https://test/test");
        } catch (URISyntaxException e) {
            fail("Error initializing test data: " + e.toString());
            return;
        }

        KmsDefinition kmsDef = new KmsDefinition()
                .setName("test")
                .setUri(uri)
                .setCredential("token")
                .setInstanceId("1234567890")
                .setType("key-protect");

        kmsDef.validate();
    }
}
