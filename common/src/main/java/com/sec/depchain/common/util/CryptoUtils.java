package com.sec.depchain.common.util;

import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
    private CryptoUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static byte[] deriveSymmetricKey(byte[] sharedSecret) throws Exception {
        byte[] salt = new byte[32]; // Could be a fixed or dynamically generated salt
        Mac mac = Mac.getInstance( Constants.HMAC_SHA256_ALGORITHM);
        mac.init(new SecretKeySpec(salt, Constants.HMAC_SHA256_ALGORITHM));
        return mac.doFinal(sharedSecret);
    }

    public static byte[] deriveSharedSecret(PrivateKey privateKey, PublicKey publicKey) throws Exception {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");

        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);
        return keyAgreement.generateSecret();
    }

    public static byte[] generateHMAC(byte[] symmetricKey, byte[] message) throws Exception {
        Mac hmac = Mac.getInstance(Constants.HMAC_SHA256_ALGORITHM);
        SecretKeySpec secretKey = new SecretKeySpec(symmetricKey, Constants.HMAC_SHA256_ALGORITHM);
        hmac.init(secretKey);
        return hmac.doFinal(message);
    }

    public static boolean verifyHMAC(byte[] symmetricKey, byte[] message, byte[] receivedHMAC) throws Exception {
        byte[] expectedHMAC = generateHMAC(symmetricKey, message);
        return MessageDigest.isEqual(expectedHMAC, receivedHMAC);
    }

    public static String generateMAC(PrivateKey privateKey, PublicKey publicKey, String message) throws Exception {
        // Derive shared secret using ECDH

        byte[] sharedSecret = CryptoUtils.deriveSharedSecret(privateKey, publicKey); // IP
        // Derive symmetric key using HKDF
        byte[] symmetricKey = CryptoUtils.deriveSymmetricKey(sharedSecret);

        // Generate HMAC for the message
        byte[] mac = CryptoUtils.generateHMAC(symmetricKey, message.getBytes());
        return java.util.Base64.getEncoder().encodeToString(mac);
    }

    public static boolean verifyMAC(PrivateKey privateKey, PublicKey publicKey, String message, String receivedMAC)
            throws Exception {
        // Derive shared secret using ECDH

        byte[] sharedSecret = CryptoUtils.deriveSharedSecret(privateKey, publicKey);

        // Derive symmetric key using HKDF
        byte[] symmetricKey = CryptoUtils.deriveSymmetricKey(sharedSecret);

        // Verify HMAC for the message
        byte[] expectedMAC = CryptoUtils.generateHMAC(symmetricKey, message.getBytes());

        return java.util.Arrays.equals(expectedMAC, java.util.Base64.getDecoder().decode(receivedMAC));
    }
    public static String signMessage(PrivateKey privateKey, String message) throws Exception
    {
        Signature signature = Signature.getInstance(Constants.SIGNATURE_ALGORITHM_SHA256_ECDSA);

        signature.initSign(privateKey);

        signature.update(message.getBytes());

        byte[] digitalSignature = signature.sign();

        return Base64.getEncoder().encodeToString(digitalSignature);
    }
    public static boolean verifySignature(PublicKey publicKey, String messsage, String receivedSignature) throws Exception{

        Signature signature = Signature.getInstance(Constants.SIGNATURE_ALGORITHM_SHA256_ECDSA);

        signature.initVerify(publicKey);

        signature.update(messsage.getBytes());

        byte[] signatureBytes = Base64.getDecoder().decode(receivedSignature);

        return signature.verify(signatureBytes);
    }
}
