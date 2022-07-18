package io.strimzi.kafka.topicenc.kms;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * The mechanism for instantiating a KMS instance. This factory uses the
 * className field available in the KmsDefinition to create the instance.
 */
public class KmsFactory {

    /**
     * Given a valid KmsDefinition, instantiate and return the KMS implementation
     * described by the KmsDefinition. Currently we use the className field of the
     * KMS def and assume it has a constructor accepting one argument, namely the
     * KmsDefinition instance. Other factory methods are conceivable such as JSON
     * objects and are being explored.
     * 
     * @param kmsDef
     * @return
     * @throws KmsException
     */
    public static KeyMgtSystem createKms(KmsDefinition kmsDef) throws KmsException {

        Class<?> kmsClass;
        try {
            kmsClass = Class.forName(kmsDef.getKmsClassname());
        } catch (ClassNotFoundException e) {
            throw new KmsException(
                    "Error loading KMS implementation class for KMS definiton, " + kmsDef.getName(),
                    e);
        }

        Constructor<?> constructor;
        try {
            constructor = kmsClass.getConstructor(KmsDefinition.class);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new KmsException(
                    "Error creating KMS constructor for KMS definition, " + kmsDef.getName(), e);
        }
        KeyMgtSystem kms;
        try {
            kms = (KeyMgtSystem) constructor.newInstance(kmsDef);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new KmsException(
                    "Error instantiating KMS for KMS definition, " + kmsDef.getName(), e);
        }
        return kms;
    }
}
