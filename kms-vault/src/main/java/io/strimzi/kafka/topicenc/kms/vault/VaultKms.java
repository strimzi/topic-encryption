/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms.vault;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.strimzi.kafka.topicenc.common.EncUtils;
import io.strimzi.kafka.topicenc.kms.KeyMgtSystem;
import io.strimzi.kafka.topicenc.kms.KmsDefinition;
import io.strimzi.kafka.topicenc.kms.KmsException;

/**
 * Key Management System interface implemented with Vault.
 */
public class VaultKms implements KeyMgtSystem {

    public static final String VAULT_TOKEN_HEADER = "X-Vault-Token";

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultKms.class);
    private static final ObjectMapper OBJ_MAPPER = new ObjectMapper();
    private static final String KEY_PATH = "/data/data/%s";

    private HttpClient client;
    private KmsDefinition config;

    public VaultKms() {
    }

    /**
     * Constructor
     * 
     * @param config a valid KmsDefiniton instance containing the info to interact
     *               with Vault.
     */
    public VaultKms(KmsDefinition config) {
        if (isNull(config)) {
            throw new IllegalArgumentException("Null KmsConfig argument.");
        }
        if (isNull(config.getUri())) {
            throw new IllegalArgumentException("Required argument 'baseUri' is missing.");
        }
        if (isNull(config.getCredential())) {
            throw new IllegalArgumentException("Required argument 'token' is missing.");
        }
        this.config = config;
        this.client = HttpClient.newBuilder().build();
        LOGGER.debug("Vault KMS created");
    }

    /**
     * Return the key corresponding to the requested key reference.
     */
    @Override
    public SecretKey getKey(String keyReference) throws KmsException {

        URI uri;
        try {
            uri = createKeyUri(config.getUri(), keyReference);
        } catch (URISyntaxException e) {
            throw new KmsException("Error creating Vault URI", e);
        }

        // create request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header(VAULT_TOKEN_HEADER, config.getCredential())
                .GET()
                .build();

        // send request
        HttpResponse<String> rsp;
        try {
            rsp = client.send(request, BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new KmsException("Error requesting key.", e);
        }

        // check HTTP status code
        if (rsp.statusCode() != 200) {
            LOGGER.error("Error requesting key from Vault: HTTP {}", rsp.statusCode());
            throw new KmsException("Error accessing Vault instance: HTTP " + rsp.statusCode());
        }

        // parse out key, decode and return:
        String key = getKey(rsp.body(), keyReference);
        LOGGER.debug("Vault KMS returned key");
        return EncUtils.base64Decode(key);
    }

    public static URI createKeyUri(URI baseUri, String keyRef)
            throws URISyntaxException, UnsupportedEncodingException {
        String uriStr = String.format("%s/%s",
                baseUri.toString(),
                URLEncoder.encode(keyRef, StandardCharsets.UTF_8.toString()));
        return new URI(uriStr);
    }

    /**
     * Utility to process the JSON response to a key request and return the key
     * payload.
     * 
     * @param jsonStr      a JSON response document
     * @param keyReference the reference of the key to return.
     * @return a String in base64 encoding
     * @throws KmsException if any error occurs or the requested key is not
     *                      returned.
     */
    private String getKey(String jsonStr, String keyReference) throws KmsException {
        JsonNode jsonObj;
        try {
            jsonObj = OBJ_MAPPER.readTree(jsonStr);
        } catch (JsonProcessingException e) {
            throw new KmsException("Error processing KMS response", e);
        }
        String path = String.format(KEY_PATH, keyReference);
        JsonNode keyNode = jsonObj.at(path);
        if (keyNode != null) {
            String key = keyNode.asText();
            if (key != null) {
                return key;
            }
        }
        // key not found:
        throw new KmsException("Key " + keyReference + " not found");
    }
}
