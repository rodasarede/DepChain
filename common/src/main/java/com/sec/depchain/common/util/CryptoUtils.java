package com.sec.depchain.common.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import com.sec.depchain.common.Transaction;

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
    public static String deriveEthAddress(PublicKey publicKey)
    {
        ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
        ECPoint point = ecPublicKey.getW();
        byte[] x = to32Bytes(point.getAffineX().toByteArray());
        byte[] y = to32Bytes(point.getAffineY().toByteArray());

        byte[] uncompressedPubKey = new byte[65];
        uncompressedPubKey[0] = 0x04; // Uncompressed prefix
        System.arraycopy(x, 0, uncompressedPubKey, 1, 32);
        System.arraycopy(y, 0, uncompressedPubKey, 33, 32);

        byte[] hash = keccak256(uncompressedPubKey);
        byte[] addressBytes = Arrays.copyOfRange(hash, hash.length - 20, hash.length);
    
        // 4. Convert to hex (with "0x" prefix)
        return "0x" + bytesToHex(addressBytes);
        
    }
    /**
 * Converts a BigInteger byte array to exactly 32 bytes, padding with zeros if needed
 */
private static byte[] to32Bytes(byte[] bytes) {
    byte[] result = new byte[32];
    int start = Math.max(0, 32 - bytes.length); // Calculate padding start
    System.arraycopy(bytes, 0, result, start, Math.min(bytes.length, 32));
    return result;
}
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    private static byte[] keccak256(byte[] input) {
    // This is a simplified version. For production, use Bouncy Castle or a proper Keccak lib.
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA3-256");
        return digest.digest(input);
    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("SHA3-256 not supported", e);
    }
}
public static boolean verifySignature(Transaction tx) {
    try {
        // 1. Recover the public key from signature
        byte[] txHash = hashTransaction(tx);
        byte[] signatureBytes = Numeric.hexStringToByteArray(tx.getSignature());
        
        // Extract r, s, v
        byte[] r = Arrays.copyOfRange(signatureBytes, 0, 32);
        byte[] s = Arrays.copyOfRange(signatureBytes, 32, 64);
        byte v = signatureBytes[64];
        
        // 2. Recover the public key
        
        BigInteger publicKey = Sign.signedMessageHashToKey(
            txHash,
            new Sign.SignatureData(v, r, s)
        );        
        // 3. Compare with sender's address
        String recoveredAddress = "0x" + Keys.getAddress(publicKey);

        return recoveredAddress.equalsIgnoreCase(tx.getFrom().toHexString());
    } catch (Exception e) {
        return false;
    }
}
public static byte[] hashTransaction(Transaction tx)
{
    byte[] rawTxData = tx.getRawDataForSigning();
    return Hash.sha3(rawTxData); // Keccak-256
}

}
