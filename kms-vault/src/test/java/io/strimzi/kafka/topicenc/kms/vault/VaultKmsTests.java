/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms.vault;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

import org.junit.Before;
import org.junit.Test;

import io.strimzi.kafka.topicenc.common.EncUtils;
import io.strimzi.kafka.topicenc.kms.KeyMgtSystem;
import io.strimzi.kafka.topicenc.kms.KmsDefinition;
import io.strimzi.kafka.topicenc.kms.KmsException;
import io.strimzi.kafka.topicenc.kms.KmsFactoryManager;

/**
 * Testing of the Vault KMS. This is not complete because: 1) it assumes a vault
 * instance is running on localhost 2) vault token currently must be entered as
 * a string below.
 * 
 * This will be addressed in a separate PR involving the use of
 * testcontainers.org
 */
public class VaultKmsTests {

    private static final String BASE_URI = "https://127.0.0.1:8200/v1/secret/data";
    private static final String STORE_KEY_DATA = "{\"data\":{\"%s\":\"%s\"}}";

    KeyMgtSystem vaultKms;
    KmsDefinition config;

    @Before
    public void setUp() {
        // create the KMS:
        try {
            config = new KmsDefinition()
                    .setUri(new URI(BASE_URI))
                    .setType(VaultKmsFactory.class.getName())
                    .setCredential("<vault token>");

            vaultKms = KmsFactoryManager.getInstance().createKms(config);

        } catch (Exception e) {
            fail("Error creating vault kms: " + e.toString());
            return;
        }
    }

    @Test
    public void basicVaultTests() {

        String testKey;
        try {
            testKey = createTestKey();
        } catch (NoSuchAlgorithmException e) {
            fail("Creating test key: " + e.toString());
            return;
        }

        // store test key
        try {
            storeKey("test", testKey);
        } catch (KmsException | IOException | InterruptedException e) {
            fail("Error storing test key " + e.toString());
            return;
        }

        // retrieve test key
        SecretKey key;
        try {
            key = vaultKms.getKey("test");

        } catch (KmsException e) {
            fail("Error retrieving key from kms: " + e.toString());
            return;
        }
        if (key == null) {
            fail("getKey() did not return a key");
        }
        String retrievedKey = EncUtils.base64Encode(key);
        assertEquals("Retrieved key does not equal stored key.", testKey, retrievedKey);
        // compare
    }

    private void storeKey(String keyRef, String key)
            throws KmsException, IOException, InterruptedException {

        URI uri = VaultKms.createKeyUri(config.getUri(), keyRef);
        String data = String.format(STORE_KEY_DATA, keyRef, key);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header(VaultKms.VAULT_TOKEN_HEADER, config.getCredential())
                .POST(HttpRequest.BodyPublishers.ofString(data))
                .build();

        HttpClient client = HttpClient.newBuilder().build();
        HttpResponse<String> rsp = client.send(request, BodyHandlers.ofString());
        if (rsp.statusCode() != 200) {
            throw new IOException("Error accessing Vault instance: HTTP " + rsp.statusCode());
        }
    }

    /**
     * Obtains the test key from the test KMS.
     * 
     * @return base 64 encoding of the key.
     * @throws NoSuchAlgorithmException
     */
    private String createTestKey() throws NoSuchAlgorithmException {
        SecretKey key = EncUtils.generateAesKey(128);
        return EncUtils.base64Encode(key);
    }
}
