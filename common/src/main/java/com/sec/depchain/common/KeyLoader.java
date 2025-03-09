package com.sec.depchain.common;



import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.openssl.PEMParser;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.NumberFormat.Style;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class KeyLoader {
        static {
        // Add BouncyCastle as a security provider
        Security.addProvider(new BouncyCastleProvider());
    }

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

    public static Map <Integer,PublicKey> loadPublicKeys(String keyDir) throws IOException {
        //TODO read all public keys from all_public_keys.pem. I tried to implement this way but didn´t work
        Map<Integer, PublicKey> publicKeys = new HashMap<>();
        for (int i = 1; i <= 100; i++) {
            String filename = keyDir + "/public_key_" + i + ".pem";
            PublicKey publicKey = loadPublicKey(filename);
            publicKeys.put(i, publicKey);
        }
        return publicKeys;
    }
    
    public static PublicKey loadPublicKey(String filename) {
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
            KeyFactory keyFactory = KeyFactory.getInstance("EC");

            // Create an X509EncodedKeySpec from the encoded key
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);

            // Generate the PublicKey object
            return keyFactory.generatePublic(keySpec);
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

}