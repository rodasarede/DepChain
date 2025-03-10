package com.sec.depchain.common.util;

import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;



public class CryptoUtils {
    private CryptoUtils()
    {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    public static byte[] deriveSymmetricKey(byte[] sharedSecret) throws Exception {
        byte[] salt = new byte[32]; // Could be a fixed or dynamically generated salt
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        return mac.doFinal(sharedSecret);
    }
     public static byte[] deriveSharedSecret(PrivateKey privateKey, PublicKey publicKey) throws Exception {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");

        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);
        return keyAgreement.generateSecret();
    }
    public static byte[] generateHMAC(byte[] symmetricKey, byte[] message) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(symmetricKey, "HmacSHA256");
        hmac.init(secretKey);
        return hmac.doFinal(message);
    }

    public static boolean verifyHMAC(byte[] symmetricKey, byte[] message, byte[] receivedHMAC) throws Exception {
        byte[] expectedHMAC = generateHMAC(symmetricKey, message);
        return MessageDigest.isEqual(expectedHMAC, receivedHMAC);
    }
    public static String generateMAC(PrivateKey privateKey, PublicKey publicKey, String message) throws Exception {
        // Derive shared secret using ECDH
        // TODO here with the IP and index...
            
        byte[] sharedSecret = CryptoUtils.deriveSharedSecret(privateKey, publicKey);               // IP
        // Derive symmetric key using HKDF
        byte[] symmetricKey = CryptoUtils.deriveSymmetricKey(sharedSecret);

        // Generate HMAC for the message
        byte[] mac = CryptoUtils.generateHMAC(symmetricKey, message.getBytes());
        return java.util.Base64.getEncoder().encodeToString(mac);
    }

    public static boolean verifyMAC(PrivateKey privateKey, PublicKey publicKey, String message, String receivedMAC) throws Exception {
        // Derive shared secret using ECDH
    
        byte[] sharedSecret = CryptoUtils.deriveSharedSecret(privateKey, publicKey);

        // Derive symmetric key using HKDF
        byte[] symmetricKey = CryptoUtils.deriveSymmetricKey(sharedSecret);

        // Verify HMAC for the message
        byte[] expectedMAC = CryptoUtils.generateHMAC(symmetricKey, message.getBytes());

        return java.util.Arrays.equals(expectedMAC, java.util.Base64.getDecoder().decode(receivedMAC));
    }
}
