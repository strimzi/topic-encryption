package io.strimzi.kafka.topicenc.kms.keyprotect;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URI;

import javax.crypto.SecretKey;

import org.junit.Before;
import org.junit.Test;

import io.strimzi.kafka.topicenc.kms.KeyMgtSystem;
import io.strimzi.kafka.topicenc.kms.KmsDefinition;
import io.strimzi.kafka.topicenc.kms.KmsException;

public class KeyProtectTests {

    private static final String KP_URI = "https://us-south.kms.cloud.ibm.com";
    private static final String KEY_REF = "";
    private static final String KP_INSTANCEID = "";

    KeyMgtSystem keyProtect;
    KmsDefinition kmsDef;

    @Before
    public void setUp() {
        // create the KMS:
        try {
            kmsDef = new KmsDefinition()
                    .setUri(new URI(KP_URI))
                    .setCredential("")
                    .setInstanceId(KP_INSTANCEID);

            keyProtect = new KeyProtectKms(kmsDef);

        } catch (Exception e) {
            fail("Error creating kms: " + e.toString());
            return;
        }
    }

    @Test
    public void basicTests() {

        // retrieve test key
        try {
            SecretKey key = keyProtect.getKey(KEY_REF);
            assertNotNull("Retrieved key is null.", key);
        } catch (KmsException e) {
            fail("Error retrieving key from kms: " + e.toString());
            return;
        }
    }

}
