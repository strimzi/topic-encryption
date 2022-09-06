/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms.keyprotect;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.crypto.SecretKey;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.strimzi.kafka.topicenc.kms.KeyMgtSystem;
import io.strimzi.kafka.topicenc.kms.KmsDefinition;
import io.strimzi.kafka.topicenc.kms.KmsException;
import io.strimzi.kafka.topicenc.kms.KmsFactoryManager;

/**
 * Units tests for the Key Protection KMS implementation.
 */
public class KeyProtectTest {

    private static final String NAME = "name";
    private static final String KP_URI = "uri";
    private static final String KEY_REF = "key_ref";
    private static final String KP_INSTANCEID = "instance_id";
    private static final String KP_CREDENTIAL = "credential";
    private static final String KP_PROPS_FILE = "src/test/resources/keyprotect.properties";
    private static final String KMS_DEF_FILE = "src/test/resources/kmsdef.json";

    KeyMgtSystem keyProtect;
    KmsDefinition kmsDef;
    String testKeyRef;

    @Before
    public void setUp() {
        try {
            testKeyRef = loadTestKeyRef(new File(KP_PROPS_FILE));
            kmsDef = loadKmsDef(new File(KMS_DEF_FILE));

            keyProtect = KmsFactoryManager.getInstance().createKms(kmsDef);

        } catch (Exception e) {
            fail("Error setting up key-protect test: " + e.toString());
            return;
        }
    }

    @Test
    public void basicTests() {
        // retrieve test key
        try {
            SecretKey key = keyProtect.getKey(testKeyRef);
            assertNotNull("Retrieved key is null.", key);
        } catch (KmsException e) {
            fail("Error retrieving key from kms: " + e.toString());
            return;
        }
    }

    /**
     * Load KMS def from a JSON file.
     * 
     * @param file
     * @return
     * @throws StreamReadException
     * @throws DatabindException
     * @throws IOException
     */
    private KmsDefinition loadKmsDef(File file)
            throws StreamReadException, DatabindException, IOException {
        ObjectMapper objMapper = new ObjectMapper();
        KmsDefinition kmsDef = objMapper.readValue(file,
                new TypeReference<KmsDefinition>() {
                });
        kmsDef.validate();
        return kmsDef;
    }

    /**
     * Load the key ref to use during testing.
     * 
     * @param file
     * @return
     * @throws IOException
     */
    private String loadTestKeyRef(File file) throws IOException {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
        }
        return props.getProperty(KEY_REF);
    }
}
