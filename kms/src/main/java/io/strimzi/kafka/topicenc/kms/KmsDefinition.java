/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms;

import java.net.URI;

import io.strimzi.kafka.topicenc.common.Strings;

public class KmsDefinition {

    private URI uri; // optional
    private String name; // required
    private String instanceId; // optional. IBM Cloud requires this
    private String credential;
    private String kmsClassname;

    public URI getUri() {
        return uri;
    }

    public KmsDefinition setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getName() {
        return name;
    }

    public KmsDefinition setName(String name) {
        this.name = name;
        return this;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public KmsDefinition setInstanceId(String instance) {
        this.instanceId = instance;
        return this;
    }

    public String getCredential() {
        return credential;
    }

    public KmsDefinition setCredential(String credential) {
        this.credential = credential;
        return this;
    }

    public String getKmsClassname() {
        return kmsClassname;
    }

    public KmsDefinition setKmsClassname(String className) {
        this.kmsClassname = className;
        return this;
    }

    public KmsDefinition validate() {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Name missing from KMS definition");
        }
        if (Strings.isNullOrEmpty(kmsClassname)) {
            throw new IllegalArgumentException("Implementation class name not present.");
        }
        return this;
    }
}
