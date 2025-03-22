package com.sec.depchain.common.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.Base64;

import org.junit.jupiter.api.Test;

public class CryptoUtilsTest {

    @Test
    public void testDeriveSymmetricKey() throws Exception {
        byte[] sharedSecret = "testSharedSecret".getBytes();
        byte[] symmetricKey = CryptoUtils.deriveSymmetricKey(sharedSecret);
        assertNotNull(symmetricKey);
        assertEquals(32, symmetricKey.length);
    }

    @Test
    public void testDeriveSharedSecret() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256);
        KeyPair keyPair1 = keyGen.generateKeyPair();
        KeyPair keyPair2 = keyGen.generateKeyPair();

        byte[] sharedSecret = CryptoUtils.deriveSharedSecret(keyPair1.getPrivate(), keyPair2.getPublic());
        assertNotNull(sharedSecret);
        assertTrue(sharedSecret.length > 0);
    }

    @Test
    void testDeriveSharedSecret2() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256);

        KeyPair keyPair1 = keyGen.generateKeyPair();
        KeyPair keyPair2 = keyGen.generateKeyPair();

        byte[] secret1 = CryptoUtils.deriveSharedSecret(keyPair1.getPrivate(), keyPair2.getPublic());
        byte[] secret2 = CryptoUtils.deriveSharedSecret(keyPair2.getPrivate(), keyPair1.getPublic());

        assertArrayEquals(secret1, secret2, "Shared secrets should be identical for both parties");

        byte[] secret3 = CryptoUtils.deriveSharedSecret(keyPair1.getPrivate(), keyPair1.getPublic());
        assertNotEquals(Arrays.toString(secret1), Arrays.toString(secret3),
                "Shared secrets should be different for different key pairs");
    }

    @Test
    void testGenerateHMAC() throws Exception {
        byte[] key = "testkey".getBytes();
        byte[] message = "Hello, world!".getBytes();

        byte[] hmac1 = CryptoUtils.generateHMAC(key, message);
        byte[] hmac2 = CryptoUtils.generateHMAC(key, message);

        assertArrayEquals(hmac1, hmac2, "HMAC should be consistent for the same input");
    }

    @Test
    public void testGenerateAndVerifyMAC() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256);
        KeyPair keyPair1 = keyGen.generateKeyPair();
        KeyPair keyPair2 = keyGen.generateKeyPair();

        String message = "testMessage";
        String mac = CryptoUtils.generateMAC(keyPair1.getPrivate(), keyPair2.getPublic(), message);
        assertNotNull(mac);

        boolean isValid = CryptoUtils.verifyMAC(keyPair1.getPrivate(), keyPair2.getPublic(), message, mac);
        assertTrue(isValid);
    }

    @Test
    public void testSignAndVerifySignature() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256);
        KeyPair keyPair = keyGen.generateKeyPair();

        String message = "testMessage";
        String signature = CryptoUtils.signMessage(keyPair.getPrivate(), message);
        assertNotNull(signature);

        boolean isValid = CryptoUtils.verifySignature(keyPair.getPublic(), message, signature);
        assertTrue(isValid);
    }

    @Test
    void testVerifySignature2() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256);
        KeyPair keyPair = keyGen.generateKeyPair();

        String message = "Verify this";
        String signature = CryptoUtils.signMessage(keyPair.getPrivate(), message);

        assertTrue(CryptoUtils.verifySignature(keyPair.getPublic(), message, signature), "Signature should verify");

        // Tamper with the message
        assertFalse(CryptoUtils.verifySignature(keyPair.getPublic(), "Fake message", signature),
                "Tampered message should not verify");

        // Tamper with the signature by modifying a decoded byte
        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        signatureBytes[signatureBytes.length - 1] ^= 1; // Flip the last byte

        String tamperedSignature = Base64.getEncoder().encodeToString(signatureBytes);
        assertFalse(CryptoUtils.verifySignature(keyPair.getPublic(), message, tamperedSignature),
                "Tampered signature should not verify");

    }
}
