package utils;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Scanner;

public class EncryptCredentials {
    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);

        System.out.println("Enter credentials (format: username,password):");
        String credentials = sc.nextLine().trim();

        String secret = System.getenv("AES_SECRET_KEY");
        if (secret == null || secret.isEmpty()) {
            throw new RuntimeException("AES_SECRET_KEY not set in system variables");
        }

        char[] passphrase = secret.toCharArray();

        String encrypted = CryptoUtils.encrypt(credentials, passphrase);

        System.out.println("\nEncrypted payload (copy this into credentials.txt):\n");
        System.out.println(encrypted);

        System.out.println("\nDo you want to save this to a file? (y/N)");
        String save = sc.nextLine().trim();

        if ("y".equalsIgnoreCase(save)) {
            String outFile = "E:\\selenium\\credentials.txt";
            try (PrintWriter pw = new PrintWriter(new FileWriter(outFile))) {
                pw.print(encrypted);
            }
            System.out.println("Saved encrypted credentials to: " + outFile);
        }

        for (int i = 0; i < passphrase.length; i++) passphrase[i] = 0;
        sc.close();
    }
}
