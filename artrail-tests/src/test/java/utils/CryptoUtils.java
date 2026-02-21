package utils;

import java.util.Base64;

public class CryptoUtils {

    // Encrypt a string (for safe console display or storing in Excel)
    public static String encrypt(String plainText) {
        if (plainText == null) return "";
        return Base64.getEncoder().encodeToString(plainText.getBytes());
    }

    // Decrypt a string (if you later store encrypted passwords in Excel)
    public static String decrypt(String encryptedText) {
        if (encryptedText == null) return "";
        return new String(Base64.getDecoder().decode(encryptedText));
    }

    // Optional main method to test
    public static void main(String[] args) {
        String test = "Art@1234";
        String encrypted = encrypt(test);
        String decrypted = decrypt(encrypted);
        System.out.println("Original: " + test);
        System.out.println("Encrypted: " + encrypted);
        System.out.println("Decrypted: " + decrypted);
    }
}
