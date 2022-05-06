/*
 * Copyright Strimzi authors. License: Apache License 2.0 (see the file LICENSE or
 * http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.enc;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * An Encrypter/Decrypter for AES GCM.
 */
public class AesGcmEncrypter implements EncrypterDecrypter {

    public static final int IV_SIZE = 16; // bytes
    public static final int KEY_SIZE = 128; // for now
    private static final String JCE_PROVIDER = "SunJCE"; // for now

    private final String transformation;
    private final SecretKey key;
    private final SecureRandom random;

    public AesGcmEncrypter(SecretKey key) {
        this.key = key;
        this.transformation = CryptoUtils.AES256_GCM_NOPADDING;
        this.random = new SecureRandom();
    }

    @Override
    public EncData encrypt(byte[] plaintext) throws GeneralSecurityException {
        byte[] iv = createIV();
        return encrypt(plaintext, iv);
    }

    @Override
    public EncData encrypt(byte[] plaintext, byte[] iv) throws GeneralSecurityException {
        Cipher encCipher = createEncryptionCipher(transformation, key, iv);
        byte[] ciphertext = encCipher.doFinal(plaintext);
        return new EncData(iv, ciphertext);
    }

    @Override
    public byte[] decrypt(EncData encData) throws GeneralSecurityException {
        // every encryption assumed to have its own IV
        Cipher decCipher = createDecryptionCipher(transformation, key, encData.getIv());
        return decCipher.doFinal(encData.getCiphertext());
    }

    private byte[] createIV() {
        byte[] buf = new byte[IV_SIZE];
        random.nextBytes(buf);
        return buf;
    }

    private static Cipher createEncryptionCipher(String transformation, SecretKey key, byte[] iv)
            throws GeneralSecurityException {
        return createCipher(Cipher.ENCRYPT_MODE, transformation, key, iv);
    }

    private static Cipher createDecryptionCipher(String transformation, SecretKey key, byte[] iv)
            throws GeneralSecurityException {
        return createCipher(Cipher.DECRYPT_MODE, transformation, key, iv);
    }

    private static Cipher createCipher(int mode, String transformation, SecretKey key, byte[] iv)
            throws GeneralSecurityException {
        if (iv == null || iv.length == 0) {
            throw new GeneralSecurityException("Initialization vector either null or empty.");
        }
        Cipher cipher = Cipher.getInstance(transformation, JCE_PROVIDER);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(KEY_SIZE, iv);
        cipher.init(mode, key, gcmSpec);
        return cipher;
    }
}
