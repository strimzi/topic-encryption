/*
 * Copyright Strimzi authors. License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import javax.crypto.SecretKey;

import com.jayway.jsonpath.JsonPath;

import io.strimzi.kafka.topicenc.enc.CryptoUtils;

/**
 * Key Management System interface implemented with Vault.
 */
public class VaultKms implements KeyMgtSystem {

    public static final String VAULT_TOKEN_HEADER = "X-Vault-Token";

    private HttpClient client;
    private KmsDefinition config;

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
    }

    @Override
    public SecretKey getKey(String keyReference) throws KmsException {

        HttpRequest request = HttpRequest.newBuilder().uri(config.getUri())
                .header(VAULT_TOKEN_HEADER, config.getCredential()).GET().build();

        HttpResponse<String> rsp;
        try {
            rsp = client.send(request, BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new KmsException("Error requesting key.", e);
        }
        if (rsp.statusCode() != 200) {
            throw new KmsException("Error accessing Vault instance: HTTP " + rsp.statusCode());
        }
        String key = JsonPath.read(rsp.body(), "data.data." + keyReference);
        return CryptoUtils.base64Decode(key);
    }
}

