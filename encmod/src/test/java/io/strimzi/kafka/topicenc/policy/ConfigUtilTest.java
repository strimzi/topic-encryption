/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.policy;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.strimzi.kafka.topicenc.kms.KmsDefinition;

/**
 * Test JSON policy loading into the data structures used by the Encryption
 * Module.
 */
public class ConfigUtilTest {

    private static final File KMS_DEF_FILE = new File("src/test/resources/kmsdefs.json");
    private static final File POLICY_FILE = new File("src/test/resources/policies.json");

    @Test
    public void kmsDefLoadTest() throws IOException, URISyntaxException {
        Map<String, KmsDefinition> kmsMap = JsonPolicyLoader.loadKmsDefs(KMS_DEF_FILE);
        assertEquals("Unexpected kmsMap size", kmsMap.size(), 3);
    }

    @Test
    public void policyLoadTest() throws IOException, URISyntaxException {
        Map<String, KmsDefinition> kmsMap = JsonPolicyLoader.loadKmsDefs(KMS_DEF_FILE);

        List<TopicPolicy> policies = JsonPolicyLoader.loadTopicPolicies(POLICY_FILE, kmsMap);

        assertEquals("Unexpected policy size", policies.size(), 1);

        List<TopicPolicy> policies2 = JsonPolicyLoader.loadTopicPolicies(KMS_DEF_FILE, POLICY_FILE);

        assertEquals("Unexpected policy size", policies2.size(), 1);

        for (int i = 0; i < policies.size(); i++) {
            assertEquals("Unequal policy lists", policies.get(i), policies.get(i));
        }
    }
}
