/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestDataFileUtil {

    public static byte[] hexToBin(File file) throws FileNotFoundException, IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            List<Byte> data = new ArrayList<>();
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                int numBytes = 0;
                for (int i = 0; i < line.length() && numBytes < 16; i+=3, numBytes++) {
                    if (line.charAt(i) != ' ') {
                        byte b = hexToByte(line.charAt(i), line.charAt(i+1));
                        data.add(b);
                    }
                }
            }
            return toByteArray(data);
        }
    }
    
    public static byte hexToByte(char c1, char c2) {
        int n1 = hexCharToInt(c1);
        int n2 = hexCharToInt(c2);
        return (byte) ((n1 << 4) + n2);
    }
    
    public static int hexCharToInt(char hexChar) {
        int n = Character.digit(hexChar, 16);
        if (n == -1) {
            throw new IllegalArgumentException(
              "Invalid Hexadecimal Character: "+ hexChar);
        }
        return n;        
    }
    
    private static byte[] toByteArray(List<Byte> list) {
        byte[] array = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i).byteValue();
        }
        return array;
    }
}
