package com.sec.depchain.server;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import com.sec.depchain.common.Transaction;

public class Mempool {
    private final ConcurrentHashMap <String, Transaction> transactions; 
    
    public Mempool()
    {
        this.transactions = new ConcurrentHashMap<>();
    } 

    public void addTransactionToMempool(Transaction tx)
    {
        byte[] serializedTx = tx.getRawDataForSigning();

        // 2. Hash the serialized transaction data (Keccak-256)
        byte[] txHash = Hash.sha3(serializedTx);

        // 3. Convert the transaction hash to a hexadecimal string
        String txHashHex =  Numeric.toHexString(txHash);

        transactions.putIfAbsent(txHashHex, tx);
    }

    public void removeTransactionFromMempool(Transaction tx){

        byte[] serializedTx = tx.getRawDataForSigning();

        // 2. Hash the serialized transaction data (Keccak-256)
        byte[] txHash = Hash.sha3(serializedTx);

        // 3. Convert the transaction hash to a hexadecimal string
        String txHashHex =  Numeric.toHexString(txHash);

        transactions.remove(txHashHex);
    }
    public ConcurrentHashMap<String, Transaction> getTransactions() {
        return transactions;
    }
    public boolean containsTransaction(String txHash) {
        return transactions.containsKey(txHash);
    }
    public int size() {
        return transactions.size();
    }
    public void clear() {
        transactions.clear();
    }

}
