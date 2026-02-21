package utils;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class EncryptCredentials {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private static String encrypt(String plainText, byte[] keyBytes) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Store IV + ciphertext together
        byte[] allBytes = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, allBytes, 0, iv.length);
        System.arraycopy(encrypted, 0, allBytes, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(allBytes);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: java EncryptCredentials <username> <password> <outputFile>");
            return;
        }

        // Load AES key from environment variable
        String keyEnv = System.getenv("CRED_ENC_KEY");
        if (keyEnv == null) {
            throw new IllegalStateException("CRED_ENC_KEY not set in environment");
        }
        byte[] keyBytes = Base64.getDecoder().decode(keyEnv);

        String username = args[0];
        String password = args[1];
        String outputFile = args[2];

        Map<String, String> creds = new HashMap<>();
        creds.put("username", encrypt(username, keyBytes));
        creds.put("password", encrypt(password, keyBytes));

        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(Paths.get(outputFile).toFile(), creds);

        System.out.println("Encrypted credentials written to " + outputFile);
    }
}
