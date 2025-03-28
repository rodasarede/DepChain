package com.sec.depchain.common.util;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

public class KeyLoader {
    private KeyLoader() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static PrivateKey loadPrivateKey(String filePath) throws Exception {
        try {
            // Read the entire file content as a string
            String pemContent = new String(Files.readAllBytes(Paths.get(filePath)));

            // Remove the PEM headers and footers
            pemContent = pemContent.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", ""); // Remove all whitespace

            // Decode the Base64-encoded key content
            byte[] encodedKey = Base64.getDecoder().decode(pemContent);

            return parsePrivateKey(encodedKey);
        } catch (IOException e) {
            System.err.println("Error reading file: " + filePath);
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("EC algorithm not supported.");
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            System.err.println("Invalid key specification.");
            e.printStackTrace();
        }
        return null;
    }

    public static Map<Integer, PublicKey> loadPublicKeys(String keyDir) throws Exception {
        Map<Integer, PublicKey> publicKeys = new HashMap<>();
        for (int i = 1; i <= 100; i++) {
            String filename = keyDir + "/public_key_" + i + ".pem";
            PublicKey publicKey = loadPublicKey(filename);
            publicKeys.put(i, publicKey);
        }
        return publicKeys;
    }

    public static PublicKey loadPublicKey(String filename) throws Exception {
        try {
            // Read the entire file content as a string
            String pemContent = new String(Files.readAllBytes(Paths.get(filename)));

            // Remove the PEM headers and footers
            pemContent = pemContent.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", ""); // Remove all whitespace

            // Decode the Base64-encoded key content
            byte[] encodedKey = Base64.getDecoder().decode(pemContent);

            // Create a KeyFactory for the EC algorithm
            return parsePublicKey(encodedKey);
        } catch (IOException e) {
            System.err.println("Error reading file: " + filename);
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("EC algorithm not supported.");
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            System.err.println("Invalid key specification.");
            e.printStackTrace();
        }
        return null;
    }

    private static PublicKey parsePublicKey(byte[] binaryKey) throws Exception {
        // Use KeyFactory to parse the binary key
        KeyFactory keyFactory = KeyFactory.getInstance(Constants.EC); // Use "EC" for elliptic curve keys
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(binaryKey);
        return keyFactory.generatePublic(keySpec);
    }

    private static PrivateKey parsePrivateKey(byte[] binaryKey) throws Exception {
        // Use KeyFactory to parse the binary key
        KeyFactory keyFactory = KeyFactory.getInstance(Constants.EC);

        // Create an PKCS8EncodedKeySpec from the encoded key
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(binaryKey);
        return keyFactory.generatePrivate(keySpec);
    }

    public static String loadPrivateKeyFromFile(String filePath) throws IOException{
    String content = new String(Files.readAllBytes(Paths.get(filePath)));
        
    // Clean the content - remove whitespace, newlines, and "0x" prefix if present
    String privateKeyHex = content.trim()
        .replaceAll("\\s+", "")
        .replaceAll("0x", "");
        
    return privateKeyHex;
}
    

}