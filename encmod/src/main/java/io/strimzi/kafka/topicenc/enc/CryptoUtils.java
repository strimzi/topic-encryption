/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.enc;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Commonly general purpose cryptographic functions and definitions.
 */
public class CryptoUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CryptoUtils.class);
    
    public static final String AES = "AES";
	public static final String AES_GCM_PADDING = AES + "/GCM/PKCS5Padding";
	public static final String AES256_GCM_NOPADDING = "AES_256/GCM/NoPadding";

	/**
	 * Create an array of bytes with random bits, suitable for use
	 * as nonce or initialization vector.
	 *
	 * @param sizeBytes
	 * @return
	 */
	public static byte[] createRandom(int numBytes) {
        byte[] buf = new byte[numBytes];
        new SecureRandom().nextBytes(buf);
        return buf;
	}
	
	public static SecretKey generateKey(String algo, int keySize) throws NoSuchAlgorithmException  {
	    KeyGenerator kgen = KeyGenerator.getInstance(algo);
	    kgen.init(keySize);
	    return kgen.generateKey();		
	}

	public static SecretKey generateAesKey(int keySize) throws NoSuchAlgorithmException  {
		return generateKey(AES, keySize);
	}
	
	public static void logCiphers() {
	    for (Provider provider : Security.getProviders()) {
	        LOGGER.debug(provider.getName());
	        for (Map.Entry<Object, Object> entry : provider.entrySet()) {
	            if (((String) entry.getValue()).contains("GCM")) {
	                LOGGER.debug("key: [%s]  value: [%s]%n",
	                    entry.getKey(),
	                    entry.getValue());
	            }
	        }
	    }	    
	}
}
