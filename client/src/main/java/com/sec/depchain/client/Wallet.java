package com.sec.depchain.client;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECKey;
import java.security.interfaces.ECPrivateKey;

import com.sec.depchain.common.Transaction;
import com.sec.depchain.common.util.CryptoUtils;
import com.sec.depchain.common.util.KeyLoader;

public class Wallet {
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final String address;
    
    
    public Wallet(int clientId) throws Exception{
        String filePath = "../common/src/main/java/com/sec/depchain/resources/clientKey/private_key_" + clientId + ".pem";
        this.privateKey = KeyLoader.loadPrivateKey(filePath);
        filePath = "../common/src/main/java/com/sec/depchain/resources/clientKey/public_key_" + clientId + ".pem";
        this.publicKey = KeyLoader.loadPublicKey(filePath);

        this.address = CryptoUtils.deriveEthAddress(publicKey);

    }
    public String getAddress() {
        return address;
    }

    public String signTransaction(Transaction tx) throws Exception{

        return CryptoUtils.signMessage(privateKey, createSigningData(tx));
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
