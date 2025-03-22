package com.sec.depchain.common.util;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KeyLoaderTest {

    @TempDir
    Path tempDir; // JUnit will create a temporary directory
    
    
    @Test
    void testLoadValidPrivateKey() throws Exception {
        // Create a temporary private key file
        Path privateKeyPath = tempDir.resolve("private_key.pem");
        Files.write(privateKeyPath, (
            "-----BEGIN PRIVATE KEY-----\n" +
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgMDwNFYHevdxWm+h/\n" +
            "vj5ZuuyHXjxtFIz0z7dmbhTt80GhRANCAAT9lkMSfXDMxZAsSAU2engRIH5HxW0/\n" +
            "RkzwGTTpo4XvA4gixEQpBZicdOnoozqJUozNVyxWbDEZ+rQlD7FtvIN3\n" +
            "-----END PRIVATE KEY-----\n"
        ).getBytes());

        PrivateKey privateKey = KeyLoader.loadPrivateKey(privateKeyPath.toString());
        assertNotNull(privateKey, "Private key should be loaded successfully");
    }

    @Test
    void testLoadValidPublicKey() throws Exception {
        // Create a temporary public key file
        Path publicKeyPath = tempDir.resolve("public_key.pem");
           Files.write(publicKeyPath, (
        "-----BEGIN PUBLIC KEY-----\n" +
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE/ZZDEn1wzMWQLEgFNnp4ESB+R8Vt\n" +
        "P0ZM8Bk06aOF7wOIIsREKQWYnHTp6KM6iVKMzVcsVmwxGfq0JQ+xbbyDdw==\n" +
        "-----END PUBLIC KEY-----\n"
    ).getBytes());


        PublicKey publicKey = KeyLoader.loadPublicKey(publicKeyPath.toString());
        assertNotNull(publicKey, "Public key should be loaded successfully");
    }


    @Test
    void testLoadMalformedPrivateKey() throws Exception {
        // Create a file with invalid key content
        Path privateKeyPath = tempDir.resolve("malformed_private_key.pem");
        Files.write(privateKeyPath,("INVALID FORMAT").getBytes());

        assertThrows(Exception.class, () -> KeyLoader.loadPrivateKey(privateKeyPath.toString()));
    }
}
