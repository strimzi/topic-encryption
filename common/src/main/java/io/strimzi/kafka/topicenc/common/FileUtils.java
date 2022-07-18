/*
 * Copyright Strimzi authors. License: Apache License 2.0 (see the file LICENSE or
 * http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.common;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

/**
 * Common utility methods involving Files.
 */
public class FileUtils {

    /**
     * Returns a File instance for the filename, derived from the classpath.
     * 
     * @param hostObject the object whose classloader will be used to locate the
     *                   file
     * @param filename   the filename
     * @return a File instance
     * @throws IOException
     * @throws URISyntaxException
     */
    public static File getFileFromClasspath(Object hostObject, String filename)
            throws IOException, URISyntaxException {

        URL url = hostObject.getClass().getClassLoader()
                .getResource(filename);
        if (url == null) {
            throw new IOException("File not accessible from classpath: " + filename);
        }
        return Paths.get(url.toURI()).toFile();
    }
}
