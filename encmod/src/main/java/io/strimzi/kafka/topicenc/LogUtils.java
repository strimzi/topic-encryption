/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc;

import java.util.Base64;

public class LogUtils {

	public static String base64Encode(byte[] rawBytes) {
		return Base64.getEncoder().encodeToString(rawBytes);
	}
	
	public static byte[] base64Decode(String base64Str) {
		return Base64.getDecoder().decode(base64Str);
	}
	
    public static void hexDump(String title, byte[] buffer) {
        
        if (buffer == null) {
            return;
        }
        if (title != null) {
            title = String.format("%s (buffer.length=%d %04X bytes)", title, buffer.length, buffer.length);
            System.out.println(title);
        }
        final String MID_FILLER = "   ";
        StringBuilder hex = new StringBuilder();
        StringBuilder chars = new StringBuilder();
        int numBytes = buffer.length;
        int i = 0;
        for (i = 0; i < numBytes; i++) {
            
            if ((i > 0) && (i % 16 == 0)) {
                hex.append(MID_FILLER);
                hex.append(chars);
                hex.append('\n');
                chars = new StringBuilder();
            }
            byte b = buffer[i];
            hex.append(String.format("%02X ", b));
            if (b >= 0x20 && b < 0x7F) {
                chars.append((char) b);
            } else {
                chars.append('.');
            }
        }
        
        // loop over. add remainders
        if (chars.length() > 0) {
            for (int j = i % 16; j < 16; j++) {
                hex.append("   ");
            }
            hex.append(MID_FILLER);
            hex.append(chars);
            hex.append('\n');
        }
        // for now, write to stdout
        System.out.println(hex);
    }
	
}
