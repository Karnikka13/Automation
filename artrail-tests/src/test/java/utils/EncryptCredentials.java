package utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EncryptCredentials {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: java utils.EncryptCredentials <username> <password> <outputFile>");
            return;
        }

        String username = args[0];
        String password = args[1];
        String outputFile = args[2];

        String key = System.getenv("CRED_ENC_KEY");
        if (key == null || key.isEmpty()) {
            throw new RuntimeException("Environment variable CRED_ENC_KEY is not set!");
        }

        // Decode Base64 key
        byte[] decodedKey = Base64.getDecoder().decode(key);
        SecretKeySpec secretKey = new SecretKeySpec(decodedKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

        // Encrypt username and password
        Map<String, String> creds = new HashMap<>();
        creds.put("username", encrypt(username, cipher, secretKey));
        creds.put("password", encrypt(password, cipher, secretKey));

        // Write JSON output file
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(Paths.get(outputFile).toFile(), creds);

        System.out.println("✅ Encrypted credentials saved to: " + outputFile);
    }

    private static String encrypt(String value, Cipher cipher, SecretKeySpec secretKey) throws Exception {
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes("UTF-8")));
    }
}
