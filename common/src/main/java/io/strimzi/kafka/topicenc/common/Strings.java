/*
 * Copyright Strimzi authors. License: Apache License 2.0 (see the file LICENSE or
 * http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.common;

/**
 * Utilities for working with Strings.
 */
public class Strings {

    /**
     * Returns true is a String is null, empty or blank.
     * 
     * @param s the input string
     * @return the result of the evaluation
     */
    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Create a normalized key for use in maps. The string is trimmed and converted
     * to lowercase. If the input string is null, a NullPointerException is thrown.
     * 
     * @param s the input string
     * @return the converted output key
     */
    public static String createKey(String s) {
        return s.trim().toLowerCase();
    }

}
