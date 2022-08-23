/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The KmsFactoryManager is a singleton managing KMS factories declared using
 * the Java service providers interface (SPI).
 */
public class KmsFactoryManager {

    private static final KmsFactoryManager INSTANCE = new KmsFactoryManager();
    private static final String DUP_KMS_MSG = """
            KMS provider error: Provider factory names must be unique.
            The following provider names appear more than once: %s.
            Possible actions:
            - Remove one or more jar files from the classpath.
            - Rebuild the KMS provider with a new, unique name.
            - Verify the correct KMS provider name is specified in the configuration.
            """;
    private final int INIT_FACTORY_COUNT = 0;

    private final ServiceLoader<KmsFactory> loader;
    private final Set<String> dups;

    private KmsFactoryManager() {
        // load all factory service providers
        loader = ServiceLoader.load(KmsFactory.class);
        // determine duplicate factory names.
        dups = getDuplicateNames();
    }

    /**
     * Returns the singleton KmsFactoryManager instance.
     * 
     * @return
     * @throws KmsException
     */
    public static KmsFactoryManager getInstance() throws KmsException {

        if (INSTANCE.dups.size() > 1) {
            throw new KmsException(String.format(DUP_KMS_MSG, INSTANCE.dups));
        }
        return INSTANCE;
    }

    /**
     * Given a valid KmsDefinition, instantiate and return the appropriate KMS
     * implementation as described by the KmsDefinition.
     * 
     * @param kmsDef
     * @return
     * @throws KmsException
     */
    public KeyMgtSystem createKms(KmsDefinition kmsDef) throws KmsException {

        // obtain the factory using its type (name).
        final KmsFactory kmsFactory = getFactory(kmsDef.getType());
        if (kmsFactory == null) {
            throw new KmsException(
                    "Unknown KMS factory name while initializing KMS: " + kmsDef.getType());
        }
        // use the factory to return the KeyMgtSystem instance.
        return kmsFactory.createKms(kmsDef);
    }

    /**
     * Look up the KmsFactory service provider by name.
     * 
     * @param type
     * @return
     * @throws KmsException
     */
    private KmsFactory getFactory(String name) throws KmsException {
        // currently a naive implementation, iterating linearly over service providers.
        // This is fine for now because there are only 3 types of factories supported.
        for (KmsFactory factory : loader) {
            if (factory.getName().equals(name)) {
                return factory;
            }
        }
        // if this far, a factory with the given name does not exist.
        return null;
    }

    /**
     * Creates a list of duplicate KMS provider names appearing on the classpath.
     * Factories names must be unique.
     * 
     * @return
     */
    private Set<String> getDuplicateNames() {
        // construct map of factory names to an integer representing how often the
        // factory name occurs:
        Map<String, Integer> factoryNameCounts = new HashMap<>();
        for (KmsFactory factory : loader) {
            String name = factory.getName();
            Integer num = factoryNameCounts.getOrDefault(name, INIT_FACTORY_COUNT);
            factoryNameCounts.put(name, num.intValue() + 1);
        }
        // return any name occuring more than once:
        return factoryNameCounts.entrySet()
                .stream()
                .filter(x -> x.getValue() > 1)
                .map(x -> x.getKey())
                .collect(Collectors.toSet());
    }
}
