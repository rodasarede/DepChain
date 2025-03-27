package com.sec.depchain.common;

import java.util.*;
import org.hyperledger.besu.datatypes.Address;




public class Blockchain {
    private static List<Block> chain = new ArrayList<>();
    private static List<Transaction> pendingTransactions = new ArrayList<>();
    private static Map<Address, AccountState> currentState = new HashMap<>();

    public Blockchain() {
        // replace with loaded block
        // Create the genesis block with initial state
        // Map<Address, AccountState> genesisState = new HashMap<>();
        // genesisState.put( Address.fromHexString("0xEoaAddress1"), new AccountState(java.math.BigInteger.valueOf(1000))); // Example EOA
        // genesisState.put(Address.fromHexString("0xEoaAddress1"), new AccountState(java.math.BigInteger.valueOf(1000), "0x60016002...")); // Example Contract
        // Block genesisBlock = new Block(null, new ArrayList<>(), genesisState);

        Block genesisBlock = new Block("src/main/java/com/sec/depchain/common/SmartContractsUtil/Genesis.json");


        chain.add(genesisBlock);
        currentState.putAll(genesisBlock.getState());
    }

    

    public  void addTransaction(Transaction tx) {
        pendingTransactions.add(tx);
    }


    public  List<Block> getChain() {
        return chain;
    }
    public  Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }
}

