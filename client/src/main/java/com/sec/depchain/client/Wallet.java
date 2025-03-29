package com.sec.depchain.client;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECKey;
import java.security.interfaces.ECPrivateKey;

import org.bouncycastle.crypto.ec.ECPair;
import org.bouncycastle.util.BigIntegers;
import org.identityconnectors.common.ByteUtil;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

import com.sec.depchain.common.Transaction;
import com.sec.depchain.common.util.CryptoUtils;
import com.sec.depchain.common.util.KeyLoader;
import com.sec.depchain.common.util.EthereumKeyGen;

public class Wallet {
    private final ECKeyPair ecPair;
    private final String address;
    
    
    public Wallet(int clientId) throws Exception{
        String filePath = "../common/src/main/java/com/sec/depchain/resources/keys/eth_keys/private_key_" + clientId + ".hex";

        String hex = KeyLoader.loadPrivateKeyEth(filePath);
        this.ecPair = EthereumKeyGen.createECPair(hex);
        this.address = "0x" + Keys.getAddress(this.ecPair);
    }
    public String getAddress() {
        return address;
    }

 public String signTransaction(Transaction tx) throws Exception {
        // 1. Create raw transaction
        byte[] rawTxData = tx.getRawDataForSigning();
        System.out.println("Transação para dar hash" + rawTxData );

        byte[] txHash = Hash.sha3(rawTxData); // Keccak-256
        System.out.println("Transa Hashed" + txHash);

        Sign.SignatureData signatureData = Sign.signMessage(txHash, ecPair, false);

        byte[] signatureBytes = new byte[65];

        System.arraycopy(signatureData.getR(), 0, signatureBytes, 0, 32);
        System.arraycopy(signatureData.getS(), 0, signatureBytes, 32, 32);
        signatureBytes[64] = signatureData.getV()[0];
        return Numeric.toHexString(signatureBytes);
    }
    private String createSigningData(Transaction tx) {
        // Create deterministic string representation for signing
        return String.join(":",
            tx.getFrom(),
            tx.getTo(),
            tx.getValue().toString(),
            tx.getData(),
            String.valueOf(tx.getNonce())
        );
    }

}
