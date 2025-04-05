package com.sec.depchain.common;

import java.security.MessageDigest;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import org.hyperledger.besu.datatypes.Address;

import com.google.gson.Gson;
import com.sec.depchain.common.util.CryptoUtils;

import java.util.*;
import java.util.stream.Collectors;

public class Block {
    private String blockHash;
    private String previousBlockHash;
    private List<Transaction> transactions;
    private Map<Address, AccountState> state;
    private int height;
    private long timestamp;

    public Block(String genesisFilename) {
        // Load the genesis block from the json file
        // System.out.println("System is in this path: " +
        // System.getProperty("user.dir"));
        loadFromJson(genesisFilename);
        this.height = 0;
        this.timestamp = System.currentTimeMillis();
    }

    public Block(String previousBlockHash, List<Transaction> transactions, Map<Address, AccountState> state,
            int height) {
        this.previousBlockHash = previousBlockHash;
        this.transactions = transactions;
        this.state = state;
        this.blockHash = calculateHash();
        this.height = height;
        this.timestamp = System.currentTimeMillis();
    }

    public Block(String previousBlockHash, List<Transaction> transactions, int height) {
        this.previousBlockHash = previousBlockHash;
        this.transactions = transactions;
        this.blockHash = calculateHash();
        this.height = height;
        this.timestamp = System.currentTimeMillis();
    }

    public String calculateHash() {
        return CryptoUtils.calculateHash(previousBlockHash, height, transactions);
    }

    public String getBlockHash() {
        return blockHash;
    }

    public String getPreviousBlockHash() {
        return previousBlockHash;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public Map<Address, AccountState> getState() {
        return state;
    }

    public void loadFromJson(String filename) {

        try (FileReader reader = new FileReader(filename)) {
            Gson gson = new Gson();
            JsonBlockData data = gson.fromJson(reader, JsonBlockData.class);

            this.blockHash = data.block_hash;
            this.previousBlockHash = data.previous_block_hash;
            this.transactions = data.transactions;
            this.state = parseState(data.state);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load block from JSON: " + e.getMessage());
        }
    }

    private Map<Address, AccountState> parseState(Map<String, AccountState> stateMap) {
        return stateMap.entrySet().stream()
                .collect(Collectors.toMap(entry -> Address.fromHexString(entry.getKey()), Map.Entry::getValue));
    }

    private static class JsonBlockData {
        String block_hash;
        String previous_block_hash;
        List<Transaction> transactions;
        Map<String, AccountState> state;
    }

    public int getHeight() {
        return height;
    }

    public void printBlockDetails() {

        System.out.println("Block Details:");
        System.out.println("Hash: " + this.blockHash);
        System.out.println("Previous Hash: " + this.previousBlockHash);

        System.out.println("\nTransactions:");
        for (Transaction tx : this.transactions) {
            System.out.println(tx);
        }

        System.out.println("\nState:");
        Map<String, AccountState> state = this.state.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().toHexString(), Map.Entry::getValue));
        for (Map.Entry<String, AccountState> entry : state.entrySet()) {
            System.out.println("Address: " + entry.getKey());
            System.out.println("Balance: " + entry.getValue().getBalance());
            if (entry.getValue().getCode() != null) {
                System.out.println("Code: " + entry.getValue().getCode());
                System.out.println("Storage: " + entry.getValue().getStorage());
                // storage needs to be printed
            }

        }
    }

    public long getTimestamp() {
        return timestamp;
    }
}
