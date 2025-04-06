package com.sec.depchain.common;


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;

import org.hyperledger.besu.datatypes.Address;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.Expose;
import com.sec.depchain.common.util.CryptoUtils;

import java.util.*;
import java.util.stream.Collectors;

public class Block {
    @Expose
    private String blockHash;
    @Expose
    private String previousBlockHash;
    @Expose
    private List<Transaction> transactions;


    private Map<Address, AccountState> state;
    private int height;
    private long timestamp;

    // Constructor for Genesis block
    public Block(String genesisFilename) {
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
        /*
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
        */
    }

    public long getTimestamp() {
        return timestamp;
    }
     
    public  void persistBlockToJson( String filePath, String fileName) {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Address.class, (JsonSerializer<Address>) (address, type, context) ->
                    context.serialize(address.toString())  // or customize as needed
                )
                .registerTypeAdapter(BigInteger.class, (JsonSerializer<BigInteger>) (bigInt, t, ctx) -> ctx.serialize(bigInt.toString()))
                .setPrettyPrinting()
                .create();

        File directory = new File(filePath);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                // System.out.println("Created directory: " + filePath);
            } else {
                System.err.println("Failed to create directory: " + filePath);
                return;
            }
        }

        String fullPath = filePath.endsWith("/") ? filePath + fileName : filePath + "/" + fileName;

        try (FileWriter writer = new FileWriter(fullPath)) {
            gson.toJson(this, writer);
            // System.out.println("Block written to " + fullPath);
        } catch (IOException e) {
            System.err.println("Failed to write block to JSON: " + e.getMessage());
        }
    }
}
