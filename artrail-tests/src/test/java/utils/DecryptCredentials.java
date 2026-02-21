package utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import org.json.JSONObject;

public class DecryptCredentials {

    public static JSONObject getDecryptedCredentials(String filePath) throws Exception {

        // First try System property (-D)
        String key = System.getProperty("CRED_ENC_KEY");

        // If not found, use OS env
        if (key == null || key.isEmpty()) {
            key = System.getenv("CRED_ENC_KEY");
        }

        if (key == null || key.isEmpty()) {
            throw new RuntimeException("Environment variable CRED_ENC_KEY is not set!");
        }

        // ✅ Key must be Base64 decoded only once
        byte[] decodedKey = Base64.getDecoder().decode(key);

        if (decodedKey.length != 16) {  // AES-128 = 16 bytes
            throw new RuntimeException("Invalid AES key length = " + decodedKey.length + " bytes, must be 16");
        }

        SecretKeySpec secretKey = new SecretKeySpec(decodedKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

        // ✅ Read encrypted JSON
        String content = new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");
        JSONObject encData = new JSONObject(content);

        JSONObject decrypted = new JSONObject();

        decrypted.put("username", decrypt(encData.getString("username"), cipher, secretKey));
        decrypted.put("password", decrypt(encData.getString("password"), cipher, secretKey));

        return decrypted;
    }

    private static String decrypt(String encryptedValue, Cipher cipher, SecretKeySpec key) throws Exception {
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decodedValue = Base64.getDecoder().decode(encryptedValue);
        return new String(cipher.doFinal(decodedValue), "UTF-8");
    }
}
