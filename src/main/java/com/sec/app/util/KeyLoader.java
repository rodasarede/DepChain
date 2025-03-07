package com.sec.app.util;


import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;


import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


public class KeyLoader {
    public static PrivateKey loadPrivateKey(String filePath) throws Exception {
        PemReader pemReader = new PemReader(new FileReader(new File(filePath)));
        PemObject pemObject = pemReader.readPemObject();
        byte[] keyBytes = pemObject.getContent();
        pemReader.close();

        // Convert the key to Java's PrivateKey format
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        
        return keyFactory.generatePrivate(keySpec);
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