package com.sec.depchain.common.util;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.util.Arrays;

public class EthereumKeyGen {
    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }
    private EthereumKeyGen() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    public static ECKeyPair createECPair(String privateKeyHex){
        BigInteger privateKey = new BigInteger(privateKeyHex, 16);
        ECKeyPair  keyPair = ECKeyPair.create(privateKey);
        return keyPair;
    }
    public static ECKeyPair getKeyPairFromPrivateKey(String privateKeyHex) throws Exception {
        // Convert hex private key to BigInteger
        // Convert private key hex to BigInteger
        BigInteger privateKey = new BigInteger(privateKeyHex, 16);
        
        // Derive ECKeyPair (contains private + public key)
        ECKeyPair keyPair = ECKeyPair.create(privateKey);
        
        // Get public key (uncompressed, 130 hex chars with '0x04' prefix)
        String publicKey = Numeric.toHexStringWithPrefix(keyPair.getPublicKey());
        System.out.println("Public Key (Uncompressed): " + publicKey);
        
        // Derive Ethereum address
        String address = Keys.getAddress(keyPair);
        address = "0x" + address; // Add '0x' prefix
        System.out.println("Ethereum Address: " + address);
        return keyPair;
    }
    

    public static String deriveEthereumAddress(KeyPair keyPair) {
        // Get the public key in uncompressed format (65 bytes)
        byte[] publicKeyBytes = ((java.security.interfaces.ECPublicKey)keyPair.getPublic()).getEncoded();
        
        // The first byte is 0x04 (uncompressed indicator), we need the X and Y coordinates (64 bytes)
        byte[] pubKeyNoPrefix = Arrays.copyOfRange(publicKeyBytes, 1, publicKeyBytes.length);
        
        // Calculate Keccak-256 hash of the public key
        byte[] hash = Hash.sha3(pubKeyNoPrefix);
        
        // Take last 20 bytes of the hash
        byte[] addressBytes = Arrays.copyOfRange(hash, hash.length - 20, hash.length);
        
        // Convert to hex string and add 0x prefix
        return "0x" + bytesToHex(addressBytes);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

}
