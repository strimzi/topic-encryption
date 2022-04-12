/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

import io.vertx.core.buffer.Buffer;

/**
 * Quick and dirty hex dump methods for troubleshooting.
 */
public class LogUtils {
    
    private static final String MID_COLUMN = "   ";

	
	public static void hexDump(String title, io.vertx.rxjava3.core.buffer.Buffer buffer) {
		hexDump(title, buffer.getBytes());
	}
	
	public static void hexDump(String title, Buffer buffer) {
		hexDump(title, buffer.getBytes());
	}
	
	public static void hexDump(String title, byte[] buffer) {
		
		if (buffer == null) {
			return;
		}
		if (title != null) {
		    title = String.format("%s (buffer.length=%d %04X bytes)", title, buffer.length, buffer.length);
			System.out.println(title);
		}
		StringBuilder hex = new StringBuilder();
		StringBuilder chars = new StringBuilder();
		int i = 0;
		for (i = 0; i < buffer.length; i++) {
			
			if ((i > 0) && (i % 16 == 0)) {
				hex.append(MID_COLUMN);
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
		    if (i % 16 != 0) {
    			for (int j = i % 16; j < 16; j++) {
    				hex.append("   ");
    			}
		    }
			hex.append(MID_COLUMN);
			hex.append(chars);
			hex.append('\n');
		}
		// for now, write to stdout
		System.out.println(hex);
	}
}
