package com.sec.depchain.server;

import java.util.List;

public class Block {
    private String blockHash;
    private String previousHash;
    private List <Transaction> transactions;
    private long timeStamp;
    private int nonce;
    //TODO maybe more
    public Block(String blockHash, List <Transaction> transactions){
        this.previousHash = previousHash;
        this.transactions = transactions;
        //this.timeStamp = 
        //this.blockHash compute Hash
    }
    public String getBlockHash() {
        return blockHash;
    }
    public int getNonce() {
        return nonce;
    }
    public String getPreviousHash() {
        return previousHash;
    }
    public long getTimeStamp() {
        return timeStamp;
    }
    public List<Transaction> getTransactions() {
        return transactions;
    }
    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }
    public void setNonce(int nonce) {
        this.nonce = nonce;
    }
    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
}
