/*
 * Copyright Strimzi authors. License: Apache License 2.0 (see the file LICENSE or
 * http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.policy;

import static io.strimzi.kafka.topicenc.common.Strings.createKey;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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

        // validating each topic policy, assign a KMS instance,
        // and ensuring unique topic names by creating a map.
        Map<String, KeyMgtSystem> kmsPool = new HashMap<>();
        policies.stream()
                .map(policy -> policy.validate())
                .map(policy -> assignKms(policy, kmsDefs, kmsPool))
                .collect(Collectors.toMap(JsonPolicyLoader::key, Function.identity()));

        // as an FYI, log unused kms defs:
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

        return kmsDefs.stream()
                .map(kmsDef -> kmsDef.validate())
                .collect(Collectors.toMap(JsonPolicyLoader::key, Function.identity()));
    }

    /**
     * Assign a KeyMgtSystem instance to a topic policy. This is done by matching
     * the KMS name in the topic policy to a KeyMgtSystem instance.
     * 
     * @param policy  the topic policy to which a KMS instance is assigned
     * @param kmsDefs A map of KmsDefinitions, indexed by name
     * @param kmsPool A map of already instantiated KMS instances
     * @return
     */
    private static TopicPolicy assignKms(TopicPolicy policy, Map<String, KmsDefinition> kmsDefs,
            Map<String, KeyMgtSystem> kmsPool) {

        String kmsName = createKey(policy.getKmsName(), Locale.getDefault());
        KmsDefinition kmsDef = kmsDefs.computeIfAbsent(
                kmsName,
                k -> {
                    throw new IllegalArgumentException(
                            "Topic " + policy.getTopic() + " refers to unknown KMS");
                });
        KeyMgtSystem kms = kmsPool.computeIfAbsent(
                kmsName, k -> KmsFactory.createKms(kmsDef));
        policy.setKms(kms);
        return policy;
    }

    private static String key(KmsDefinition kmsDef) {
        return createKey(kmsDef.getName(), Locale.getDefault());
    }

    private static String key(TopicPolicy policy) {
        return createKey(policy.getTopic(), Locale.getDefault());
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

