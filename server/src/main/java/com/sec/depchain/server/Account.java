package com.sec.depchain.server;

import java.math.BigInteger;

import org.bouncycastle.pqc.jcajce.provider.lms.LMSSignatureSpi.generic;

//https://ethereum.org/en/developers/docs/accounts/
public abstract class Account {
    protected final String address;
    protected BigInteger nonce; // A ctr -> nº transactions sent from an EOA or the nº of contracts created by a contract account. Only 1 transaction with a given nonce can be executed for each account, protecting against replay attacks where signed transactions are repeatedly broadcast and re-executed.
    protected BigInteger balance; //The nº of wei owned by this address. Wei is a denomination of ETH and there are 1e+18 wei per ETH.
    protected final String codeHash; //code of an account in EVM. EVM code gets executed if the account gets a message call
    protected String storageRoot; // ~ storage hash. A 256-bit hash of the root node of a Merkle Patricia trie that encodes the storage contents of the account (a mapping between 256-bit integer values), encoded into the trie as a mapping from the Keccak 256-bit hash of the 256-bit integer keys to the RLP-encoded 256-bit integer values. This trie encodes the hash of the storage contents of this account, and is empty by default.
    protected String type;

    public Account(String address, BigInteger balance, BigInteger nonce, String type, String codeHash, String storageRoot){
        if (address == null || !address.startsWith("0x") || address.length() != 42) {
            throw new IllegalArgumentException("Invalid Ethereum address format");}

        this.address = address; // to lower case?
        this.balance = balance != null ? balance : BigInteger.ZERO;
        this.nonce = nonce != null ? nonce : BigInteger.ZERO;
        this.type = type;
        this.codeHash = codeHash;
        this.storageRoot = storageRoot;
    }

    public Account(String address, BigInteger balance, BigInteger nonce, String codeHash){
        if (address == null || !address.startsWith("0x") || address.length() != 42) {
            throw new IllegalArgumentException("Invalid Ethereum address format");}

        this.address = address; // to lower case?
        this.balance = balance != null ? balance : BigInteger.ZERO;
        this.nonce = nonce != null ? nonce : BigInteger.ZERO;
        this.codeHash = codeHash;
        this.storageRoot = "";
    }
    public String getAddress() {
        return address;
    }
    public BigInteger getBalance() {
        return balance;
    }
    public BigInteger getNonce() {
        return nonce;
    }
    public String getType() {
        return type;
    }
    public void incrementNonce(){
        this.nonce = this.nonce.add(BigInteger.ONE);
    }
    public void setBalance(BigInteger balance){
        if (balance.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        this.balance = balance; 
    }
    @Override
    public String toString() {
        return String.format("<Account %s.. type=%s balance=%s>", 
        address.substring(0, 8), type, balance.toString());
    }
}
