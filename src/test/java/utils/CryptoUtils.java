package utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtils {
    private static final int AES_KEY_BITS = 256;
    private static final int GCM_IV_LENGTH = 12; // bytes
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int SALT_LENGTH = 16; // bytes
    private static final int PBKDF2_ITERATIONS = 65536;

    /**
     * Decrypt a Base64 string that contains: salt(16) || iv(12) || ciphertext+tag
     *
     * @param base64Combined Base64 string from file
     * @param passphrase     passphrase used to derive key
     * @return plaintext
     * @throws Exception when decryption fails
     */
    public static String decrypt(String base64Combined, char[] passphrase) throws Exception {
        byte[] combined = Base64.getDecoder().decode(base64Combined);

        if (combined.length < SALT_LENGTH + GCM_IV_LENGTH + 1) {
            throw new IllegalArgumentException("Invalid encrypted data.");
        }

        byte[] salt = new byte[SALT_LENGTH];
        System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);

        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, SALT_LENGTH, iv, 0, GCM_IV_LENGTH);

        int ctLength = combined.length - SALT_LENGTH - GCM_IV_LENGTH;
        byte[] ciphertext = new byte[ctLength];
        System.arraycopy(combined, SALT_LENGTH + GCM_IV_LENGTH, ciphertext, 0, ctLength);

        SecretKey key = deriveKey(passphrase, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] plain = cipher.doFinal(ciphertext);
        return new String(plain, "UTF-8");
    }

    /**
     * Convenience encrypt method (used by EncryptCredentials tool).
     *
     * @param plaintext  text to encrypt
     * @param passphrase passphrase to derive key
     * @return Base64(salt||iv||ciphertext+tag)
     * @throws Exception on error
     */
    public static String encrypt(String plaintext, char[] passphrase) throws Exception {
        SecureRandom secureRandom = new SecureRandom();

        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);

        SecretKey key = deriveKey(passphrase, salt);

        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

        byte[] out = new byte[salt.length + iv.length + ciphertext.length];
        System.arraycopy(salt, 0, out, 0, salt.length);
        System.arraycopy(iv, 0, out, salt.length, iv.length);
        System.arraycopy(ciphertext, 0, out, salt.length + iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(out);
    }

    private static SecretKey deriveKey(char[] passphrase, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, AES_KEY_BITS);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = skf.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
