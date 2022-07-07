/*
 * Copyright Strimzi authors. License: Apache License 2.0 (see the file LICENSE or
 * http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.policy;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.strimzi.kafka.topicenc.common.Strings;
import io.strimzi.kafka.topicenc.kms.KeyMgtSystem;
import io.strimzi.kafka.topicenc.kms.KmsDefinition;
import io.strimzi.kafka.topicenc.kms.KmsFactory;

/**
 * Utility methods for loading and processing configuration information residing
 * in JSON files.
 */
public class JsonPolicyLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonPolicyLoader.class);

    /**
     * Load JSON config files and return a fully initialized list of topic
     * encryption policies, one policy for each topic to be encrypted. Configuration
     * errors (e.g., missing mandatory parameter) raise IllegalArgumentException.
     * "Fully initialized" means instances of KeyMgtService are created from the KMS
     * definitions and assigned to each topic policy.
     * 
     * @param kmsDefsFile       JSON file of KMS definitions
     * @param topicPoliciesFile file containing topic files
     * @return a list of initialized topic policies
     * @throws IOException if an error occurs while accessing the config files
     */
    public static List<TopicPolicy> loadTopicPolicies(File kmsDefsFile, File topicPoliciesFile)
            throws IOException {

        Map<String, KmsDefinition> kmsDefMap = loadKmsDefs(kmsDefsFile);
        return loadTopicPolicies(topicPoliciesFile, kmsDefMap);
    }

    /**
     * Load JSON policy config file and return a fully initialized list of topic
     * encryption policies, one policy for each topic to be encrypted. Configuration
     * errors (e.g., missing mandatory parameter) raise IllegalArgumentException.
     * "Fully initialized" means instances of KeyMgtService are created from the KMS
     * definitions and assigned to each topic policy.
     * 
     * @param file    JSON file containing topic policies
     * @param kmsDefs a map of KMS definitions indexed by kms name.
     * @return a list of initialized topic policies
     * @throws IOException if an error occurs while accessing the config files
     */
    public static List<TopicPolicy> loadTopicPolicies(File file,
            Map<String, KmsDefinition> kmsDefs)
            throws IOException {

        // read in policies
        ObjectMapper objectMapper = new ObjectMapper();
        List<TopicPolicy> policies = objectMapper.readValue(file,
                new TypeReference<List<TopicPolicy>>() {
                });

        // iterate over policies, validating them and assigning a KMS instance:
        Map<String, KeyMgtSystem> kmsPool = new HashMap<>();
        Map<String, TopicPolicy> validPolicies = new HashMap<>();
        policies.stream().forEach(policy -> {

            // validate policy (
            String key = validate(policy, validPolicies);
            validPolicies.put(key, policy);

            // the policy is valid, assign it a KMS instance
            String kmsName = Strings.createKey(policy.getKmsName(), Locale.getDefault());
            KmsDefinition kmsDef = kmsDefs.get(kmsName);
            if (kmsDef == null) {
                // unknown KMS
                String msg = String.format(
                        "Policy for topic %s refers to unknown KMS, %s",
                        policy.getTopic(), kmsName);
                throw new IllegalArgumentException(msg);
            }

            KeyMgtSystem kms = kmsPool.get(kmsName);
            if (kms == null) {
                kms = KmsFactory.createKms(kmsDef);
                kmsPool.put(kmsName, kms);
            }
            policy.setKms(kms);
        });

        // log unused kms defs:
        logUnassignedKmsDefs(kmsPool.keySet(), kmsDefs.keySet());
        return policies;
    }

    /**
     * Loads KMS config definitions from a JSON file.
     *
     * @param file file in JSON format containing KMS defs
     * @return a map of KMS defs indexed on KMS name.
     * @throws IOException if an error occurs accessing the JSON file.
     */
    public static Map<String, KmsDefinition> loadKmsDefs(File file) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<KmsDefinition> kmsDefs = objectMapper.readValue(file,
                new TypeReference<List<KmsDefinition>>() {
                });
        Map<String, KmsDefinition> kmsMap = new HashMap<>();
        kmsDefs.stream().forEach(kmsDef -> {
            kmsDef.validate();
            String name = Strings.createKey(kmsDef.getName(), Locale.getDefault());
            if (kmsMap.containsKey(name)) {
                throw new IllegalArgumentException("KMS name is not unique: " + name);
            }
            kmsMap.put(name, kmsDef);
        });
        return kmsMap;
    }

    /**
     * Given a policy and the set of currently known policies, validate the policy
     * contents and verify that there is not already a policy for the policy's
     * topic. If a constraint is violated, an IllegalArgumentException is thrown.
     *
     * @param policy        the policy to validate
     * @param validPolicies a map of the current valid policies
     * @return a string to be used as the key to identify this policy
     */
    private static String validate(TopicPolicy policy, Map<String, TopicPolicy> validPolicies) {
        policy.validate();
        String key = Strings.createKey(policy.getTopic(), Locale.getDefault());
        if (validPolicies.containsKey(key)) {
            throw new IllegalArgumentException("Multiple policies defined for topic, " + key);
        }
        return key;
    }

    /**
     * Log any unused KMS definitions. The objective is to inform administrators so
     * that unused KMS definitions are removed.
     * 
     * @param kmsPool
     * @param kmsDefs
     */
    private static void logUnassignedKmsDefs(Set<String> kmsPool, Set<String> kmsDefs) {
        Set<String> unassigned = new HashSet<>();
        kmsDefs.stream().forEach(kmsDefName -> {
            if (!kmsPool.contains(kmsDefName)) {
                unassigned.add(kmsDefName);
            }
        });
        if (unassigned.size() > 0) {
            LOGGER.warn("Unused KMS definitions: " + unassigned.toString());
        }
    }
}
