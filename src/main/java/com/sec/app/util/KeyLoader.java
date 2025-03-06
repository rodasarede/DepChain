package com.sec.app.util;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

public class KeyLoader {
    public static PrivateKey loadPrivateKey(String filename) throws Exception {
        // Read the private key file
        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));

        // Remove PEM headers/footers and decode the base64 content
        String pem = new String(keyBytes);
        pem = pem.replace("-----BEGIN EC PRIVATE KEY-----", "")
                 .replace("-----END EC PRIVATE KEY-----", "")
                 .replaceAll("\\s", ""); // Remove whitespace
        byte[] decodedKey = java.util.Base64.getDecoder().decode(pem);

        // Create a PrivateKey object
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedKey);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePrivate(spec);
    }

    public static List<PublicKey> loadPublicKeys(String filename) throws Exception {
        // Read the combined public keys file
        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));

        // Split the file into individual public keys
        String pem = new String(keyBytes);
        String[] pemKeys = pem.split("-----BEGIN PUBLIC KEY-----");

        List<PublicKey> publicKeys = new ArrayList<>();
        KeyFactory kf = KeyFactory.getInstance("EC");

        for (String pemKey : pemKeys) {
            if (pemKey.trim().isEmpty()) continue;

            // Remove PEM footer and whitespace
            pemKey = pemKey.replace("-----END PUBLIC KEY-----", "")
                           .replaceAll("\\s", ""); // Remove whitespace

            // Decode the base64 content
            byte[] decodedKey = java.util.Base64.getDecoder().decode(pemKey);

            // Create a PublicKey object
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedKey);
            publicKeys.add(kf.generatePublic(spec));
        }

        return publicKeys;
    }
}