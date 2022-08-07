/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.kms;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * The KmsFactoryManager is a singleton managing KMS factories declared using
 * the Java service providers interface (SPI).
 */
public class KmsFactoryManager {

    private static final KmsFactoryManager INSTANCE = new KmsFactoryManager();
    private static final String DUP_KMS_MSG = """
            KMS provider deployment error. Provider names must be unique.
            The following provider names appear more than once: %s.
            Possible actions:
            - Remove one or more jar files from the classpath.
            - Rebuild the KMS provider with a new, unique name.
            - Verify the correct KMS provider name is specified in the configuration.
            """;

    private final ServiceLoader<KmsFactory> loader;
    private final List<String> dups;
    private final Locale local = Locale.getDefault();

    private KmsFactoryManager() {
        // load all factory service providers
        loader = ServiceLoader.load(KmsFactory.class);
        // determine if any have duplicate names.
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
            throw new KmsException("Unknown KMS type while initializing KMS: " + kmsDef.getType());
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
    private KmsFactory getFactory(String type) throws KmsException {
        // currently a naive implementation, iterating linearly over service providers.
        // This is fine for now because there are only 3 ytypes of factories supported.
        Iterator<KmsFactory> it = loader.iterator();
        while (it.hasNext()) {
            KmsFactory factory = it.next();
            if (factory.getName().equalsIgnoreCase(type)) {
                return factory;
            }
        }
        // if this far, a factory by the given name does not exist. Throw exception.
        throw new KmsException("Unknown KMS type: " + type);
    }

    /**
     * Creates a list of duplicate KMS provider names appearing on the classpath.
     * Factories names must be unique.
     * 
     * @return
     */
    private List<String> getDuplicateNames() {
        Map<String, Integer> nameMap = new HashMap<>();
        int initValue = 0;
        for (KmsFactory factory : loader) {
            String name = factory.getName().toLowerCase(local);
            Integer num = nameMap.getOrDefault(name, initValue);
            nameMap.put(name, num.intValue() + 1);
        }

        return nameMap.entrySet()
                .stream()
                .filter(x -> x.getValue() > 1)
                .map(x -> x.getKey())
                .collect(Collectors.toList());
    }
}
