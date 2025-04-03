package com.sec.depchain.common;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class AccountState {
    private BigInteger balance;
    private String code; // Only for contract accounts
    private Map<String, String> storage; // Only for contract accounts
    private BigInteger nonce;

    // Constructor for EOAs
    public AccountState(BigInteger balance) {
        this.balance = balance;
        this.code = null;
        this.storage = null;
        this.nonce = BigInteger.ZERO;
    }

    // Constructor for Contract Accounts
    public AccountState(BigInteger balance, String code) {
        this.balance = balance;
        this.code = code;
        this.storage = new HashMap<>();
        this.nonce = BigInteger.ZERO;
    }

    public BigInteger getBalance() {
        return balance;
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getStorage() {
        return storage;
    }

    public void setBalance(BigInteger balance) {
        this.balance = balance;
    }

    public void setStorage(String key, String value) {
        if (storage != null) {
            storage.put(key, value);
        }
    }

    public BigInteger getNonce() {
        return nonce;
    }
    public void incrementNonce() {
        this.nonce = this.nonce.add(BigInteger.ONE);
    }
}
